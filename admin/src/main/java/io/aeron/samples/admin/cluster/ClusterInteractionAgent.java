package io.aeron.samples.admin.cluster;

import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.admin.model.ResponseWrapper;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderDecoder;
import io.aeron.samples.cluster.admin.protocol.*;
import io.aeron.samples.cluster.protocol.MessageHeaderEncoder;
import io.aeron.samples.cluster.protocol.*;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;
import sbe.msg.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent to interact with the cluster
 */
public class ClusterInteractionAgent implements Agent, MessageHandler
{
    private static final long HEARTBEAT_INTERVAL = 250;
    private static final long RETRY_COUNT = 10;
    private static final String INGRESS_CHANNEL = "aeron:udp?term-length=64k";
    private final MutableDirectBuffer sendBuffer = new ExpandableDirectByteBuffer(1024);
    private long lastHeartbeatTime = Long.MIN_VALUE;
    private final RingBuffer adminClusterComms;
    private final IdleStrategy idleStrategy;
    private final AtomicBoolean runningFlag;
    private final PendingMessageManager pendingMessageManager;
    private AdminClientEgressListener adminClientEgressListener;
    private AeronCluster aeronCluster;
    private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    private LineReader lineReader;
    private MediaDriver mediaDriver;

    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final ConnectClusterDecoder connectClusterDecoder = new ConnectClusterDecoder();

    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final CreateAuctionCommandEncoder createAuctionCommandEncoder = new CreateAuctionCommandEncoder();
    private final AddParticipantCommandEncoder addParticipantCommandEncoder = new AddParticipantCommandEncoder();
    private final AddAuctionBidCommandEncoder addAuctionBidCommandEncoder = new AddAuctionBidCommandEncoder();
    private final ListParticipantsCommandEncoder listParticipantsCommandEncoder = new ListParticipantsCommandEncoder();
    private final ListAuctionsCommandEncoder listAuctionsCommandEncoder = new ListAuctionsCommandEncoder();

    private final sbe.msg.MessageHeaderDecoder sbeMessageHeaderDecoder = new sbe.msg.MessageHeaderDecoder();

    private final NewOrderDecoder newOrderDecoder = new NewOrderDecoder();
    private final AdminDecoder adminDecoder = new AdminDecoder();
    private final OrderCancelRequestDecoder orderCancelRequestDecoder = new OrderCancelRequestDecoder();
    private final OrderCancelReplaceRequestDecoder orderCancelReplaceRequestDecoder = new OrderCancelReplaceRequestDecoder();
    private final NewInstrumentDecoder newInstrumentDecoder = new NewInstrumentDecoder();


    /**
     * Creates a new agent to interact with the cluster
     * @param adminClusterChannel the channel to send messages to the cluster from the REPL
     * @param idleStrategy the idle strategy to use
     * @param runningFlag the flag to indicate if the REPL is still running
     */
    public ClusterInteractionAgent(
        final RingBuffer adminClusterChannel,
        final IdleStrategy idleStrategy,
        final AtomicBoolean runningFlag)
    {
        this.adminClusterComms = adminClusterChannel;
        this.idleStrategy = idleStrategy;
        this.runningFlag = runningFlag;
        this.pendingMessageManager = new PendingMessageManager(SystemEpochClock.INSTANCE);
    }



    @Override
    public int doWork()
    {
        //send cluster heartbeat roughly every 250ms
        final long now = SystemEpochClock.INSTANCE.time();
        if (now >= (lastHeartbeatTime + HEARTBEAT_INTERVAL))
        {
            lastHeartbeatTime = now;
            if (connectionState == ConnectionState.CONNECTED)
            {
                aeronCluster.sendKeepAlive();
            }
        }

        //poll inbound to this agent messages (from the REPL)
        adminClusterComms.read(this);

        //poll outbound messages from the cluster
        if (null != aeronCluster && !aeronCluster.isClosed())
        {
            aeronCluster.pollEgress();
        }

        //check for timed-out messages
        pendingMessageManager.doWork();

        //always sleep
        return 0;
    }

    @Override
    public String roleName()
    {
        return "cluster-interaction-agent";
    }

    @Override
    public void onMessage(final int msgTypeId, final MutableDirectBuffer buffer, final int offset, final int length)
    {
        messageHeaderDecoder.wrap(buffer, offset);
        switch (messageHeaderDecoder.templateId())
        {
            case ConnectClusterDecoder.TEMPLATE_ID -> processConnectCluster(buffer, offset);
            case DisconnectClusterDecoder.TEMPLATE_ID -> processDisconnectCluster();
            case NewOrderDecoder.TEMPLATE_ID -> processNowOrder(messageHeaderDecoder, buffer, offset);
            case AdminDecoder.TEMPLATE_ID -> processAdminMessage(messageHeaderDecoder, buffer, offset);
            case OrderCancelRequestDecoder.TEMPLATE_ID -> processCancelOrder(messageHeaderDecoder, buffer, offset);
            case OrderCancelReplaceRequestDecoder.TEMPLATE_ID -> processReplaceOrder(messageHeaderDecoder, buffer, offset);
            case NewInstrumentDecoder.TEMPLATE_ID -> processNewInstrument(messageHeaderDecoder, buffer, offset);
            default -> log("Unknown message type: " + messageHeaderDecoder.templateId(), AttributedStyle.RED);
        }
    }

    private void processNewInstrument(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        log("Process new instrument" + messageHeaderDecoder.templateId(), AttributedStyle.RED);

        newInstrumentDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // newinstrument-tid@security@code@client
        final String correlationId = NewInstrumentDecoder.TEMPLATE_ID + "@" +
                newInstrumentDecoder.securityId() + "@" +
                newInstrumentDecoder.code().trim() + "@" +
                sbeMessageHeaderDecoder.compID();

        pendingMessageManager.addMessage(correlationId, "new-instrument");

        retryingClusterOffer(buffer, offset, sbeMessageHeaderDecoder.encodedLength() + newInstrumentDecoder.encodedLength());
    }

    private void processReplaceOrder(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        log("Process replace order" + messageHeaderDecoder.templateId(), AttributedStyle.RED);

        orderCancelReplaceRequestDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // neworder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
                orderCancelReplaceRequestDecoder.side().value() + "@" +
                orderCancelReplaceRequestDecoder.securityId() + "@" +
                orderCancelReplaceRequestDecoder.clientOrderId().trim() + "@" +
                orderCancelReplaceRequestDecoder.traderId() + "@" +
                sbeMessageHeaderDecoder.compID();

        pendingMessageManager.addMessage(correlationId, "admin-message");

        retryingClusterOffer(buffer, offset, sbeMessageHeaderDecoder.encodedLength() + orderCancelReplaceRequestDecoder.encodedLength());
    }

    private void processCancelOrder(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        log("Process new order" + messageHeaderDecoder.templateId(), AttributedStyle.RED);

        orderCancelRequestDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // cancelorder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderEncoder.TEMPLATE_ID + "@" +
                orderCancelRequestDecoder.side().value() + "@" +
                orderCancelRequestDecoder.securityId() + "@" +
                orderCancelRequestDecoder.clientOrderId().trim() + "@" +
                orderCancelRequestDecoder.traderId() + "@" +
                "1";

        pendingMessageManager.addMessage(correlationId, "cancel-order");

        retryingClusterOffer(buffer, offset, sbeMessageHeaderDecoder.encodedLength() + orderCancelRequestDecoder.encodedLength());
    }

    private void processNowOrder(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        log("Process new order" + messageHeaderDecoder.templateId(), AttributedStyle.RED);

        newOrderDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // placeorder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
            newOrderDecoder.side().value() + "@" +
            newOrderDecoder.securityId() + "@" +
            newOrderDecoder.clientOrderId().trim() + "@" +
            newOrderDecoder.traderId() + "@" +
            sbeMessageHeaderDecoder.compID();

        pendingMessageManager.addMessage(correlationId, "place-order");

        retryingClusterOffer(buffer, offset, sbeMessageHeaderDecoder.encodedLength() + newOrderDecoder.encodedLength());
    }

    private void processAdminMessage(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        log("Process admin message: " + messageHeaderDecoder.templateId(), AttributedStyle.RED);

        adminDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // admin-tid@type@security@reqid@traderId@client
        final String correlationId = AdminEncoder.TEMPLATE_ID + "@" +
                adminDecoder.adminMessage().name() + "@" +
                adminDecoder.securityId() + "@" +
                "1" + "@" +
                "1" + "@" +
                sbeMessageHeaderDecoder.compID();
        pendingMessageManager.addMessage(correlationId, "admin-message");

        retryingClusterOffer(buffer, offset, sbeMessageHeaderDecoder.encodedLength() + adminDecoder.encodedLength());
    }


    /**
     * Opens the cluster connection
     * @param buffer the buffer containing the message
     * @param offset the offset of the message
     */
    private void processConnectCluster(final MutableDirectBuffer buffer, final int offset)
    {
        connectClusterDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        connectCluster(connectClusterDecoder.baseport(), connectClusterDecoder.port(),
            connectClusterDecoder.clusterHosts(), connectClusterDecoder.localhostName());
        connectionState = ConnectionState.CONNECTED;
    }

    /**
     * Closes the cluster connection
     */
    private void processDisconnectCluster()
    {
        log("Disconnecting from cluster", AttributedStyle.WHITE);
        disconnectCluster();
        connectionState = ConnectionState.NOT_CONNECTED;
        log("Cluster disconnected", AttributedStyle.GREEN);
    }

    /**
     * Disconnects from the cluster
     */
    private void disconnectCluster()
    {
        adminClientEgressListener = null;
        if (aeronCluster != null)
        {
            aeronCluster.close();
        }
        if (mediaDriver != null)
        {
            mediaDriver.close();
        }
    }

    /**
     * Connects to the cluster
     *
     * @param basePort base port to use
     * @param port the port to use
     * @param clusterHosts list of cluster hosts
     * @param localHostName if empty, will be looked up
     */
    private void connectCluster(
        final int basePort,
        final int port,
        final String clusterHosts,
        final String localHostName)
    {
        final List<String> hostnames = Arrays.asList(clusterHosts.split(","));
        final String ingressEndpoints = ClusterConfig.ingressEndpoints(
            hostnames, basePort, ClusterConfig.CLIENT_FACING_PORT_OFFSET);
        final String egressChannel = "aeron:udp?endpoint=" + localHostName + ":" + port;
        adminClientEgressListener = new AdminClientEgressListener(pendingMessageManager);
        adminClientEgressListener.setLineReader(lineReader);
        mediaDriver = MediaDriver.launch(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .errorHandler(this::logError)
            .dirDeleteOnShutdown(true));
        aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .egressListener(adminClientEgressListener)
                .egressChannel(egressChannel)
                .ingressChannel(INGRESS_CHANNEL)
                .ingressEndpoints(ingressEndpoints)
                .errorHandler(this::logError)
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        log("Connected to cluster leader, node " + aeronCluster.leaderMemberId(), AttributedStyle.GREEN);
    }

    private void logError(final Throwable throwable)
    {
        log("Error: " + throwable.getMessage(), AttributedStyle.RED);
    }

    /**
     * Sets the line reader to use for input saving while logging
     *
     * @param lineReader line reader to use
     */
    public void setLineReader(final LineReader lineReader)
    {
        this.lineReader = lineReader;
        pendingMessageManager.setLineReader(lineReader);
    }

    /**
     * Logs a message to the terminal if available or to the logger if not
     *
     * @param message message to log
     * @param color message color to use
     */
    private void log(final String message, final int color)
    {
        LineReaderHelper.log(lineReader, message, color);
    }

    /**
     * sends to cluster with retry as needed, up to the limit
     *
     * @param buffer buffer containing the message
     * @param offset offset of the message
     * @param length length of the message
     */
    private void retryingClusterOffer(final DirectBuffer buffer, final int offset, final int length)
    {
        if (connectionState == ConnectionState.CONNECTED)
        {
            int retries = 0;
            do
            {
                final long result = aeronCluster.offer(buffer, offset, length);
                if (result > 0L)
                {
                    return;
                }
                else if (result == Publication.ADMIN_ACTION || result == Publication.BACK_PRESSURED)
                {
                    log("backpressure or admin action on cluster offer", AttributedStyle.YELLOW);
                }
                else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED)
                {
                    log("Cluster is not connected, or maximum position has been exceeded. Message lost.",
                        AttributedStyle.RED);
                    return;
                }

                idleStrategy.idle();
                retries += 1;
                log("failed to send message to cluster. Retrying (" + retries + " of " + RETRY_COUNT + ")",
                    AttributedStyle.YELLOW);
            }
            while (retries < RETRY_COUNT);

            log("Failed to send message to cluster. Message lost.", AttributedStyle.RED);
        }
        else
        {
            log("Not connected to cluster. Connect first", AttributedStyle.RED);
        }
    }

    @Override
    public void onClose()
    {
        if (aeronCluster != null)
        {
            aeronCluster.close();
        }
        if (mediaDriver != null)
        {
            mediaDriver.close();
        }
        runningFlag.set(false);
    }

    public CompletableFuture<ResponseWrapper> onComplete(final String correlationId)
    {
        return pendingMessageManager.onComplete(correlationId);
    }
}
