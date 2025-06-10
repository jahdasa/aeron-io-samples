package io.aeron.samples.admin.cluster;

import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.exceptions.TimeoutException;
import io.aeron.samples.admin.model.BaseError;
import io.aeron.samples.admin.model.ResponseWrapper;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderDecoder;
import io.aeron.samples.cluster.admin.protocol.*;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import sbe.msg.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent to interact with the cluster
 */
public class ClusterInteractionAgent implements Agent, MessageHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInteractionAgent.class);

    private static final long HEARTBEAT_INTERVAL = 250;
    private static final long RETRY_COUNT = 10;
    private static final String INGRESS_CHANNEL = "aeron:udp?term-length=64k";
    private final MutableDirectBuffer sendBuffer = new ExpandableDirectByteBuffer(1024);
    private long lastHeartbeatTime = Long.MIN_VALUE;
    private final RingBuffer channel;
    private final IdleStrategy idleStrategy;
    private final AtomicBoolean runningFlag;
    private final PendingMessageManager pendingMessageManager;
    private AdminClientEgressListener adminClientEgressListener;
    private AeronCluster aeronCluster;
    private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
    private MediaDriver mediaDriver;

    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final ConnectClusterDecoder connectClusterDecoder = new ConnectClusterDecoder();

    private final sbe.msg.MessageHeaderDecoder sbeMessageHeaderDecoder = new sbe.msg.MessageHeaderDecoder();

    private final NewOrderDecoder newOrderDecoder = new NewOrderDecoder();
    private final AdminDecoder adminDecoder = new AdminDecoder();
    private final OrderCancelRequestDecoder orderCancelRequestDecoder = new OrderCancelRequestDecoder();
    private final OrderCancelReplaceRequestDecoder orderCancelReplaceRequestDecoder = new OrderCancelReplaceRequestDecoder();
    private final NewInstrumentDecoder newInstrumentDecoder = new NewInstrumentDecoder();
    private final ListInstrumentsMessageRequestDecoder listInstrumentsMessageRequestDecoder = new ListInstrumentsMessageRequestDecoder();


    /**
     * Creates a new agent to interact with the cluster
     * @param channel the channel to send messages to the cluster from the REPL
     * @param idleStrategy the idle strategy to use
     * @param runningFlag the flag to indicate if the REPL is still running
     */
    public ClusterInteractionAgent(
        final RingBuffer channel,
        final IdleStrategy idleStrategy,
        final AtomicBoolean runningFlag)
    {
        this.channel = channel;
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
                if(!aeronCluster.sendKeepAlive())
                {
                    LOGGER.warn("Send keep alive failed, try to connect again");
                    connectToCluster(basePort, port, clusterHosts, localHostName);
                }
            }
        }

        //poll inbound to this agent messages (from the REPL)
        channel.read(this);

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
            case ListInstrumentsMessageRequestDecoder.TEMPLATE_ID -> processListInstruments(messageHeaderDecoder, buffer, offset);
            default -> LOGGER.warn("Unknown message type: " + messageHeaderDecoder.templateId());
        }
    }

    private void processListInstruments(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset)
    {
        LOGGER.info("Process list instrument " + messageHeaderDecoder.templateId());

        listInstrumentsMessageRequestDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        final String correlationId = listInstrumentsMessageRequestDecoder.correlationId();
        pendingMessageManager.addMessage(correlationId.trim(), "list-instruments");

        final boolean success = retryingClusterOffer(
                buffer,
                offset,
                sbeMessageHeaderDecoder.encodedLength() + listInstrumentsMessageRequestDecoder.encodedLength());

        if(!success)
        {
            replyFail(correlationId);
        }
    }

    private void replyFail(String correlationId)
    {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        if (connectionState == ConnectionState.CONNECTED)
        {
            status = HttpStatus.BAD_GATEWAY;
        }
        pendingMessageManager.replyFail(correlationId.trim(), new BaseError("Not connected to cluster"), status);
    }

    private void processNewInstrument(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        LOGGER.info("Process new instrument" + messageHeaderDecoder.templateId());

        newInstrumentDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // newinstrument-tid@security@code@client
        final String correlationId = NewInstrumentDecoder.TEMPLATE_ID + "@" +
                newInstrumentDecoder.securityId() + "@" +
                newInstrumentDecoder.code().trim() + "@" +
                sbeMessageHeaderDecoder.compID();

        pendingMessageManager.addMessage(correlationId, "new-instrument");

        final boolean success = retryingClusterOffer(
            buffer,
            offset,
            sbeMessageHeaderDecoder.encodedLength() + newInstrumentDecoder.encodedLength());

        if(!success)
        {
            replyFail(correlationId);
        }
    }

    private void processReplaceOrder(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        LOGGER.info("Process replace order" + messageHeaderDecoder.templateId());

        orderCancelReplaceRequestDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // neworder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
                orderCancelReplaceRequestDecoder.side().value() + "@" +
                orderCancelReplaceRequestDecoder.securityId() + "@" +
                orderCancelReplaceRequestDecoder.clientOrderId().trim() + "@" +
                orderCancelReplaceRequestDecoder.traderId() + "@" +
                sbeMessageHeaderDecoder.compID();

        pendingMessageManager.addMessage(correlationId, "admin-message");

        final boolean success = retryingClusterOffer(
            buffer,
            offset,
            sbeMessageHeaderDecoder.encodedLength() + orderCancelReplaceRequestDecoder.encodedLength());

        if(!success)
        {
            replyFail(correlationId);
        }
    }

    private void processCancelOrder(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        LOGGER.info("Process new order" + messageHeaderDecoder.templateId());

        orderCancelRequestDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // cancelorder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderEncoder.TEMPLATE_ID + "@" +
                orderCancelRequestDecoder.side().value() + "@" +
                orderCancelRequestDecoder.securityId() + "@" +
                orderCancelRequestDecoder.clientOrderId().trim() + "@" +
                orderCancelRequestDecoder.traderId() + "@" +
                "1";

        pendingMessageManager.addMessage(correlationId, "cancel-order");

        final boolean success = retryingClusterOffer(
            buffer,
            offset,
            sbeMessageHeaderDecoder.encodedLength() + orderCancelRequestDecoder.encodedLength());

        if(!success)
        {
            replyFail(correlationId);
        }
    }

    private void processNowOrder(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset) {
        LOGGER.info("Process new order" + messageHeaderDecoder.templateId());

        newOrderDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // placeorder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
            newOrderDecoder.side().value() + "@" +
            newOrderDecoder.securityId() + "@" +
            newOrderDecoder.clientOrderId().trim() + "@" +
            newOrderDecoder.traderId() + "@" +
            sbeMessageHeaderDecoder.compID();

        pendingMessageManager.addMessage(correlationId, "place-order");

        final boolean success = retryingClusterOffer(
            buffer,
            offset,
            sbeMessageHeaderDecoder.encodedLength() + newOrderDecoder.encodedLength());

        if(!success)
        {
            replyFail(correlationId);
        }
    }

    private void processAdminMessage(MessageHeaderDecoder messageHeaderDecoder, MutableDirectBuffer buffer, int offset)
    {
        LOGGER.info("Process admin message: " + messageHeaderDecoder.templateId());

        adminDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

        // admin-tid@type@security@reqid@traderId@client
        final String correlationId = AdminEncoder.TEMPLATE_ID + "@" +
                adminDecoder.adminMessage().name() + "@" +
                adminDecoder.securityId() + "@" +
                "1" + "@" +
                "1" + "@" +
                sbeMessageHeaderDecoder.compID();
        pendingMessageManager.addMessage(correlationId, "admin-message");

        final boolean success = retryingClusterOffer(
            buffer,
            offset,
            sbeMessageHeaderDecoder.encodedLength() + adminDecoder.encodedLength());

        if(!success)
        {
            replyFail(correlationId);
        }
    }

    int basePort;
    int port;
    String clusterHosts;
    String localHostName;

    /**
     * Opens the cluster connection
     * @param buffer the buffer containing the message
     * @param offset the offset of the message
     */
    private void processConnectCluster(final MutableDirectBuffer buffer, final int offset)
    {
        connectClusterDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);

        basePort = connectClusterDecoder.baseport();
        port = connectClusterDecoder.port();
        clusterHosts = connectClusterDecoder.clusterHosts();
        localHostName = connectClusterDecoder.localhostName();

        connectToCluster(basePort, port, clusterHosts, localHostName);
    }

    private void connectToCluster(
        final int basePort,
        final int port,
        final String clusterHosts,
        final String localHostName)
    {
        connectCluster(
            basePort,
            port,
            clusterHosts,
            localHostName);

        connectionState = ConnectionState.CONNECTED;
    }

    /**
     * Closes the cluster connection
     */
    private void processDisconnectCluster()
    {
        LOGGER.info("Disconnecting from cluster");
        disconnectCluster();
        connectionState = ConnectionState.NOT_CONNECTED;
        LOGGER.info("Cluster disconnected");
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
        mediaDriver = MediaDriver.launch(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .errorHandler(this::logError)
            .dirDeleteOnShutdown(true)
            .aeronDirectoryName(getAeronDriverDir().getAbsolutePath()));

        final AeronCluster.Context context = new AeronCluster.Context()
            .egressListener(adminClientEgressListener)
            .egressChannel(egressChannel)
            .ingressChannel(INGRESS_CHANNEL)
            .ingressEndpoints(ingressEndpoints)
            .errorHandler(this::logError)
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());
        try
        {
            aeronCluster = AeronCluster.connect(context);
        }
        catch (final TimeoutException e)
        {
            LOGGER.error("Connect timeout, error: {}", e.getMessage());
        }

        LOGGER.info("Connected to cluster leader, node " + aeronCluster.leaderMemberId());
    }

    private void logError(final Throwable throwable)
    {
        LOGGER.error("Error: ", throwable);
    }

    /**
     * sends to cluster with retry as needed, up to the limit
     *
     * @param buffer buffer containing the message
     * @param offset offset of the message
     * @param length length of the message
     */
    private boolean retryingClusterOffer(final DirectBuffer buffer, final int offset, final int length)
    {
        if (connectionState == ConnectionState.CONNECTED)
        {
            int retries = 0;
            do
            {
                final long result = aeronCluster.offer(buffer, offset, length);
                if (result > 0L)
                {
                    return true;
                }
                else if (result == Publication.ADMIN_ACTION || result == Publication.BACK_PRESSURED)
                {
                    LOGGER.error("backpressure or admin action on cluster offer");
                }
                else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED)
                {
                    LOGGER.error("Cluster is not connected, or maximum position has been exceeded. Message lost.");

                    connectToCluster(basePort, port, clusterHosts, localHostName);

                    return false;
                }

                idleStrategy.idle();
                retries += 1;
                LOGGER.error("failed to send message to cluster. Retrying (" + retries + " of " + RETRY_COUNT + ")");
            }
            while (retries < RETRY_COUNT);

            LOGGER.error("Failed to send message to cluster. Message lost.");

            connectToCluster(basePort, port, clusterHosts, localHostName);

            return false;
        }
        else
        {
            LOGGER.error("Not connected to cluster. Connect first");
            return false;
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

    private static File getAeronDriverDir()
    {
        final String baseDir = System.getenv("AERON_DRIVER_DIR");
        if (null == baseDir || baseDir.isEmpty())
        {
            return new File(System.getProperty("user.dir"), "me-admin-client");
        }

        return new File(baseDir);
    }
}
