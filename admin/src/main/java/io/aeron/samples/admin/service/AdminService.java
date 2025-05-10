package io.aeron.samples.admin.service;

import io.aeron.samples.admin.cli.*;
import io.aeron.samples.admin.client.Client;
import io.aeron.samples.admin.cluster.ClusterInteractionAgent;
import io.aeron.samples.admin.model.ResponseWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

/**
 * Admin service for the cluster main class, working on a direct connection to the cluster
 */
@Slf4j
@Service
public class AdminService
{
    final UnsafeBuffer adminClusterBuffer =
            new UnsafeBuffer(BufferUtil.allocateDirectAligned(128*1024*1024 + TRAILER_LENGTH, 8));

    final OneToOneRingBuffer adminClusterChannel = new OneToOneRingBuffer(adminClusterBuffer);

    final AtomicBoolean running = new AtomicBoolean(true);
    final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();

    ClusterInteractionAgent clusterInteractionAgent;

    @PostConstruct
    public void postConstruct()
    {
        try {
            clusterInteractionAgent = new ClusterInteractionAgent(
                adminClusterChannel,
                idleStrategy,
                running);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final AgentRunner clusterInteractionAgentRunner = new AgentRunner(
            idleStrategy,
            Throwable::printStackTrace,
            null,
            clusterInteractionAgent);

        AgentRunner.startOnThread(clusterInteractionAgentRunner);
    }

    /**
     * method to handle the command execution
     */
    public void connect()
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final ConnectCluster command = new ConnectCluster();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        command.run();
    }

    /**
     * method to handle the disconnect execution
     */
    public void disconnect()
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final DisconnectCluster command = new DisconnectCluster();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        command.run();
    }

   /**
    * method to handle the command execution
    *
    * @param id command name
    * @param name command input
    */
    public void addParticipant(
        final int id,
        final String name)
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final AddParticipant command = new AddParticipant();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        command.setParticipantId(id);
        command.setParticipantName(name);

        final CompletableFuture<ResponseWrapper> response = new CompletableFuture<>();
        clusterInteractionAgent.onComplete(correlationId, response);

        command.run();

        try {
            ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * method to handle the command execution
     *
     */
    public ResponseWrapper listParticipants()
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final ListParticipants command = new ListParticipants();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        final CompletableFuture<ResponseWrapper> response = new CompletableFuture<>();
        clusterInteractionAgent.onComplete(correlationId, response);

        command.run();

        try {
            ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return new ResponseWrapper();
    }

    public ResponseWrapper addAuction(final String name, final int participantId, final int duration)
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final AddAuction command = new AddAuction();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        command.setParticipantId(participantId);
        command.setAuctionName(name);
        command.setDuration(duration);

        final CompletableFuture<ResponseWrapper> response = new CompletableFuture<>();
        clusterInteractionAgent.onComplete(correlationId, response);

        command.run();

        try {
            ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return new ResponseWrapper();
    }

    public ResponseWrapper listActions()
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final ListAuctions command = new ListAuctions();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        final CompletableFuture<ResponseWrapper> response = new CompletableFuture<>();
        clusterInteractionAgent.onComplete(correlationId, response);

        command.run();

        try {
            ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return new ResponseWrapper();
    }

    public ResponseWrapper addAuctionBid(final long auctionId, final int participantId, final long price)
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(adminClusterChannel);

        final AddAuctionBid command = new AddAuctionBid();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        command.setParticipantId(participantId);
        command.setAuctionId(auctionId);
        command.setPrice(price);

        final CompletableFuture<ResponseWrapper> response = new CompletableFuture<>();
        clusterInteractionAgent.onComplete(correlationId, response);

        command.run();

        try {
            ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return new ResponseWrapper();

    }

    public void submitOrder(String clientOrderId, long volume, long price, String side, String orderType, String timeInForce, long displayQuantity, long minQuantity, long stopPrice) throws Exception {

        Client client = Client.newInstance(1, 1);
        DirectBuffer buffer = client.submitOrder(
                clientOrderId,
                volume,
                price,
                side,
                orderType,
                timeInForce,
                displayQuantity,
                minQuantity,
                stopPrice
        );

        adminClusterChannel.write(10, buffer, 0, client.getNewOrderEncodedLength());
    }

    public void submitAdminMessage(int securityId, String adminMessageType) throws Exception {

        Client client = Client.newInstance(1, securityId);

        DirectBuffer buffer = null;
        if(adminMessageType.equals("lob"))
        {
            buffer = client.lobSnapshot();
        }
        else if(adminMessageType.equals("vwap"))
        {
            buffer = client.calcVWAP();
        }
        else if(adminMessageType.equals("marketDepth"))
        {
            buffer = client.marketDepth();
        }
        else
        {
            return;
        }

        adminClusterChannel.write(10, buffer, 0, client.getLobSnapshotMessageLength());
    }

    public void bbo() throws Exception {
/*        Client client = Client.newInstance(1, 1);
        DirectBuffer buffer = client.get

        adminClusterChannel.write(10, buffer, 0, client.getNewOrderEncodedLength());*/
    }

    public void cancelOrder(int securityId, String clientOrderId, final String side, final long price) throws Exception {
        Client client = Client.newInstance(1, securityId);
        DirectBuffer buffer = client.cancelOrder(clientOrderId, side, price);

        adminClusterChannel.write(10, buffer, 0, client.getCancelOrderEncodedLength());
    }


    public void replaceOrder(int securityId,  String clientOrderId, long volume, long price, String side, String orderType, String timeInForce, long displayQuantity, long minQuantity, long stopPrice) throws Exception {

        final Client client = Client.newInstance(1, securityId);
        final DirectBuffer buffer = client.replaceOrder(
                clientOrderId,
                volume,
                price,
                side,
                orderType,
                timeInForce,
                displayQuantity,
                minQuantity,
                stopPrice
        );

        adminClusterChannel.write(10, buffer, 0, client.getReplaceOrderEncodedLength());
    }
}
