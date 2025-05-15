package io.aeron.samples.admin.service;

import io.aeron.samples.admin.cli.*;
import io.aeron.samples.admin.client.Client;
import io.aeron.samples.admin.cluster.ClusterInteractionAgent;
import io.aeron.samples.admin.model.BaseError;
import io.aeron.samples.admin.model.ResponseWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.springframework.stereotype.Service;
import sbe.builder.BuilderUtil;
import sbe.msg.*;

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
            new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024*1024*1024 + TRAILER_LENGTH, 8));

    final RingBuffer adminClusterChannel = new ManyToOneRingBuffer(adminClusterBuffer);

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

    public ResponseWrapper placeOrder(
        final int securityId,
        String clientOrderId,
        final long volume,
        final long price,
        final String side,
        final String orderType,
        final String timeInForce,
        final long displayQuantity,
        final long minQuantity,
        final long stopPrice,
        final int traderId,
        final int clientId) throws Exception
    {
        final Client client = Client.newInstance(clientId, securityId);

        final int claimIndex = adminClusterChannel.tryClaim(10, 114);

        if (claimIndex < 0)
        {
            log.error("Failed to claim space in ring buffer for new order");
            return new ResponseWrapper(-2, null, new BaseError("Failed to accept more new order"));
        }

        try
        {
            final MutableDirectBuffer buffer = adminClusterChannel.buffer();

            client.placeOrder(
                    buffer,
                    claimIndex,
                    clientOrderId,
                    volume,
                    price,
                    side,
                    orderType,
                    timeInForce,
                    displayQuantity,
                    minQuantity,
                    stopPrice,
                    traderId
            );

            clientOrderId = BuilderUtil.fill(clientOrderId, NewOrderEncoder.clientOrderIdLength());

            final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
                    SideEnum.valueOf(side).value() + "@" +
                    securityId + "@" +
                    clientOrderId.trim() + "@" +
                    traderId + "@" +
                    clientId;

            final CompletableFuture<ResponseWrapper> response = clusterInteractionAgent.onComplete(correlationId);

            adminClusterChannel.commit(claimIndex);

            final ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            String error = e.getMessage();
            try
            {
                adminClusterChannel.abort(claimIndex);
            }
            catch (final Exception e1)
            {
                log.error(error + ", abort error: " + e1.getMessage(), e1);
            }
            log.error("Failed to write order to ring buffer: " + error, e);
            return new ResponseWrapper(-3, null, new BaseError(e.getMessage()));
        }

    }

    public ResponseWrapper submitAdminMessage(
        int securityId,
        String adminMessageType,
        final long requestId,
        final long traderId,
        final int clientId) throws Exception {

        Client client = Client.newInstance(clientId, securityId);

        DirectBuffer buffer = null;
        if(adminMessageType.equals("LOB"))
        {
            buffer = client.lobSnapshot();
        }
        else if(adminMessageType.equals("VWAP"))
        {
            buffer = client.calcVWAP();
        }
        else if(adminMessageType.equals("MarketDepth"))
        {
            buffer = client.marketDepth();
        }
        else if(adminMessageType.equals("BestBidOfferRequest"))
        {
            buffer = client.bbo();
        }
        else
        {
            return new ResponseWrapper();
        }

        // admin-tid@type@security@reqid@traderId@client
        final String correlationId = AdminEncoder.TEMPLATE_ID + "@" +
                adminMessageType + "@" +
                securityId + "@" +
                requestId + "@" +
                traderId + "@" +
                clientId;

        log.info("CorrelationId: {}", correlationId);

        final CompletableFuture<ResponseWrapper> response = clusterInteractionAgent.onComplete(correlationId);

        adminClusterChannel.write(10, buffer, 0, client.getLobSnapshotMessageLength());

        try {
            final ResponseWrapper responseWrapper = response.get(15, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
            return new ResponseWrapper(-1 , "timeout");
        }
    }

    public ResponseWrapper cancelOrder(
        int securityId,
        String clientOrderId,
        final String side,
        final long price,
        final int traderId,
        final int clientId) throws Exception
    {
        final Client client = Client.newInstance(clientId, securityId);
        DirectBuffer buffer = client.cancelOrder(clientOrderId, side, price, traderId);

        // cancelorder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderEncoder.TEMPLATE_ID + "@" +
                SideEnum.valueOf(side).value() + "@" +
                securityId + "@" +
                "-" + clientOrderId.trim() + "@" +
                traderId + "@" +
                clientId;

        log.info("CorrelationId: {}", correlationId);

        final CompletableFuture<ResponseWrapper> response = clusterInteractionAgent.onComplete(correlationId);

        adminClusterChannel.write(10, buffer, 0, client.getCancelOrderEncodedLength());

        try {
            final ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
            return new ResponseWrapper(-1 , "timeout");
        }
    }


    public ResponseWrapper replaceOrder(
        int securityId,
        String clientOrderId,
        long volume,
        long price,
        String side,
        String orderType,
        String timeInForce,
        long displayQuantity,
        long minQuantity,
        long stopPrice,
        int traderId,
        int clientId) throws Exception
    {
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
            stopPrice,
            traderId
        );

        // neworder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
            SideEnum.valueOf(side).value() + "@" +
            securityId + "@" +
            clientOrderId.trim() + "@" +
            traderId + "@" +
            clientId;

        log.info("CorrelationId: {}", correlationId);

        final CompletableFuture<ResponseWrapper> response = clusterInteractionAgent.onComplete(correlationId);

        adminClusterChannel.write(10, buffer, 0, client.getReplaceOrderEncodedLength());

        try {
            final ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.debug("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            log.error(e.getMessage(), e);
            return new ResponseWrapper(-1 , "timeout");
        }
    }

    public ResponseWrapper newInstrument(
        final int securityId,
        final String code,
        final String name,
        final int clientId) throws Exception
    {
        final Client client = Client.newInstance(clientId, securityId);

        final int claimIndex = adminClusterChannel.tryClaim(10, 114);

        if (claimIndex < 0)
        {
            log.error("Failed to claim space in ring buffer for new order");
            return new ResponseWrapper(-2, null, new BaseError("Failed to accept more new order"));
        }

        try
        {
            final MutableDirectBuffer buffer = adminClusterChannel.buffer();

            client.newInstrument(
                    buffer,
                    claimIndex,
                    securityId,
                    code,
                    name
            );

            // newinstrument-tid@security@code@client
            final String correlationId = NewInstrumentDecoder.TEMPLATE_ID + "@" +
                    securityId + "@" +
                    code + "@" +
                    clientId;

            final CompletableFuture<ResponseWrapper> response = clusterInteractionAgent.onComplete(correlationId);

            adminClusterChannel.commit(claimIndex);

            final ResponseWrapper responseWrapper = response.get(5, TimeUnit.SECONDS);
            log.info("Response: {}", responseWrapper.getData());

            return responseWrapper;
        }
        catch (final Exception e)
        {
            String error = e.getMessage();
            try
            {
                adminClusterChannel.abort(claimIndex);
            }
            catch (final Exception e1)
            {
                log.error(error + ", abort error: " + e1.getMessage(), e1);
            }
            log.error("Failed to write command to ring buffer: " + error, e);
            return new ResponseWrapper(-3, null, new BaseError(e.getMessage()));
        }

    }
}
