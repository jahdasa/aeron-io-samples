package io.aeron.samples.admin.service;

import io.aeron.samples.admin.cli.*;
import io.aeron.samples.admin.client.Client;
import io.aeron.samples.admin.cluster.ClusterInteractionAgent;
import io.aeron.samples.admin.model.BaseError;
import io.aeron.samples.admin.model.Pair;
import io.aeron.samples.admin.model.ResponseWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.springframework.stereotype.Service;
import sbe.msg.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

/**
 * Admin service for the cluster main class, working on a direct connection to the cluster
 */
@Slf4j
@Service
public class AdminService
{
    final UnsafeBuffer channelBuffer =
            new UnsafeBuffer(BufferUtil.allocateDirectAligned(64*1024*1024 + TRAILER_LENGTH, 8));

    final RingBuffer channel = new ManyToOneRingBuffer(channelBuffer);

    final AtomicBoolean running = new AtomicBoolean(true);
    final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();

    final Client client = new Client();

    ClusterInteractionAgent clusterInteractionAgent;

    @PostConstruct
    public void postConstruct()
    {
        try
        {
            clusterInteractionAgent = new ClusterInteractionAgent(
                channel,
                idleStrategy,
                running);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }

        final AgentRunner clusterInteractionAgentRunner = new AgentRunner(
            idleStrategy,
            Throwable::printStackTrace,
            null,
            clusterInteractionAgent);

        AgentRunner.startOnThread(clusterInteractionAgentRunner);

        final String connectOnBoot = System.getenv("CONNECT_ON_BOOT");
        if (connectOnBoot != null && connectOnBoot.equals("1"))
        {
           connect();
        }
    }

    /**
     * method to handle the command execution
     */
    public void connect()
    {
        final CliCommands parent = new CliCommands();
        parent.setAdminChannel(channel);

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
        parent.setAdminChannel(channel);

        final DisconnectCluster command = new DisconnectCluster();
        command.setParent(parent);

        final String correlationId = UUID.randomUUID().toString();
        command.setCorrelationId(correlationId);

        command.run();
    }

    public ResponseWrapper placeOrder(
        final int securityId,
        final String clientOrderId,
        final long volume,
        final long price,
        final String side,
        final String orderType,
        final String timeInForce,
        final long displayQuantity,
        final long minQuantity,
        final long stopPrice,
        final int traderId,
        final int clientId)
    {
        final int claimIndex = channel.tryClaim(10, 256);

        if (claimIndex < 0)
        {
            log.error("Failed to claim space in ring buffer for new order");
            return new ResponseWrapper(-2, null, new BaseError("Failed to accept more new order"));
        }

        final AtomicBuffer buffer = channel.buffer();

        client.placeOrder(
            securityId,
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
            traderId,
            clientId
        );

        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
                SideEnum.valueOf(side.toUpperCase()).value() + "@" +
                securityId + "@" +
                clientOrderId.trim() + "@" +
                traderId + "@" +
                clientId;

        final CompletableFuture<ResponseWrapper> future = clusterInteractionAgent.onComplete(correlationId);

        commit(claimIndex);

        return await(future);
    }

    public ResponseWrapper submitAdminMessage(
        final int securityId,
        final String adminMessageType,
        final long requestId,
        final long traderId,
        final int clientId)
    {
        DirectBuffer buffer;

        switch (adminMessageType)
        {
            case "LOB" -> buffer = client.lobSnapshot(securityId, clientId);
            case "VWAP" -> buffer = client.calcVWAP(securityId, clientId);
            case "MarketDepth" -> buffer = client.marketDepth(securityId, clientId);
            case "BestBidOfferRequest" -> buffer = client.bbo(securityId, clientId);
            default ->
            {
                return new ResponseWrapper();
            }
        }

        // admin-tid@type@security@reqid@traderId@client
        final String correlationId = AdminEncoder.TEMPLATE_ID + "@" +
                adminMessageType + "@" +
                securityId + "@" +
                requestId + "@" +
                traderId + "@" +
                clientId;

        log.debug("CorrelationId: {}", correlationId);

        final CompletableFuture<ResponseWrapper> future = clusterInteractionAgent.onComplete(correlationId);

        channel.write(10, buffer, 0, client.getLobSnapshotMessageLength());

        return await(future);
    }

    public ResponseWrapper cancelOrder(
        final int securityId,
        final String clientOrderId,
        final String side,
        final long price,
        final int traderId,
        final int clientId)
    {
        Pair<DirectBuffer, Integer> directBufferLongPair = client.cancelOrder(securityId, clientOrderId, side, price, traderId, clientId);

        // cancelorder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderEncoder.TEMPLATE_ID + "@" +
                SideEnum.valueOf(side.toUpperCase()).value() + "@" +
                securityId + "@" +
                "-" + clientOrderId.trim() + "@" +
                traderId + "@" +
                clientId;

        log.info("CorrelationId: {} , price: {}", correlationId, price);

        final CompletableFuture<ResponseWrapper> future = clusterInteractionAgent.onComplete(correlationId);

        channel.write(10, directBufferLongPair.getFirst(), 0, directBufferLongPair.getSecond());

        return await(future);
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
        int clientId)
    {
        final Pair<DirectBuffer, Integer> directBufferIntegerPair = client.replaceOrder(
            securityId,
            clientOrderId,
            volume,
            price,
            side,
            orderType,
            timeInForce,
            displayQuantity,
            minQuantity,
            stopPrice,
            traderId,
            clientId
        );

        // neworder-tid@side@security@clientOrderId@trader@client
        final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
            SideEnum.valueOf(side.toUpperCase()).value() + "@" +
            securityId + "@" +
            clientOrderId.trim() + "@" +
            traderId + "@" +
            clientId;

        log.debug("CorrelationId: {}", correlationId);

        final CompletableFuture<ResponseWrapper> future = clusterInteractionAgent.onComplete(correlationId);

        channel.write(10, directBufferIntegerPair.getFirst(), 0, directBufferIntegerPair.getSecond());

        return await(future);
    }

    public ResponseWrapper newInstrument(
        final int securityId,
        final String code,
        final String name,
        final int clientId)
    {
        final int claimIndex = channel.tryClaim(10, 114);

        if (claimIndex < 0)
        {
            log.error("Failed to claim space in ring buffer for new order");
            return new ResponseWrapper(-2, null, new BaseError("Failed to accept more new order"));
        }

        final MutableDirectBuffer buffer = channel.buffer();

        client.newInstrument(
                clientId,
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

        commit(claimIndex);

        return await(response);
    }


    public ResponseWrapper listInstruments(final int clientId) throws Exception
    {
        final int claimIndex = channel.tryClaim(10, 114);

        if (claimIndex < 0)
        {
            log.error("Failed to claim space in ring buffer for new order");
            return new ResponseWrapper(-2, null, new BaseError("Failed to accept more new order"));
        }

        final MutableDirectBuffer buffer = channel.buffer();

        // newinstrument-tid@timestamp@client
        final String correlationId = ListInstrumentsMessageRequestDecoder.TEMPLATE_ID + "@" +
            System.currentTimeMillis() + "@" +
            clientId;

        final CompletableFuture<ResponseWrapper> future = clusterInteractionAgent.onComplete(correlationId);

        client.listInstruments(
                clientId,
                buffer,
                claimIndex,
                correlationId
        );

        commit(claimIndex);

        return await(future);
    }

    private static ResponseWrapper await(final CompletableFuture<ResponseWrapper> future)
    {
        String error;
        try
        {
            final ResponseWrapper response = future.get(5, TimeUnit.SECONDS);
            log.debug("Response: {}", response);

            return response;
        }
        catch (final TimeoutException e)
        {
            error = "Timeout error";
        }
        catch (final InterruptedException e)
        {
            error = "Interrupted error";
        }
        catch (final ExecutionException e)
        {
            error = "Execution error";
        }
        return new ResponseWrapper(-3, null, new BaseError(error));
    }

    private void commit(int claimIndex)
    {
        try
        {
            channel.commit(claimIndex);
        }
        catch (final Exception e)
        {
            log.error("Failed to write command to ring buffer: {} ", e.getMessage(), e);
            channel.abort(claimIndex);
        }
    }
}
