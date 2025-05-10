/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aeron.samples.admin.cluster;

import io.aeron.samples.admin.model.*;
import io.aeron.samples.cluster.protocol.AddAuctionBidResult;
import io.aeron.samples.cluster.protocol.AddAuctionResult;
import org.agrona.concurrent.EpochClock;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for keeping track of pending messages and their timeouts
 */
public class PendingMessageManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingMessageManager.class);

    private final Map<String, CompletableFuture<ResponseWrapper>> futures = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private final Deque<PendingMessage> trackedMessages = new LinkedList<>();
    private final EpochClock current;
    private LineReader lineReader;

    /**
     * Constructor
     * @param current the clock to use for timeouts
     */
    public PendingMessageManager(final EpochClock current)
    {
        this.current = current;
    }

    /**
     * Add a message to the list of pending messages
     * @param correlationId the correlation id of the message
     * @param messageType  the type of message
     */
    public void addMessage(final String correlationId, final String messageType)
    {
        LOGGER.info("addMessage to trackedMessages correlationId: {}, messageType: {}", correlationId, messageType);

        final long timeoutAt = current.time() + TIMEOUT_MS;
        trackedMessages.add(new PendingMessage(timeoutAt, correlationId, messageType));
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markMessageAsReceived(final String correlationId)
    {
        trackedMessages.removeIf(pendingMessage ->
        {
            final boolean exist = pendingMessage.correlationId().equals(correlationId);

            LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
            if (exist)
            {
                replySuccess(correlationId, new AddParticipantResponse(correlationId, Collections.emptyList()));
            }
            return exist;
        });
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markCreateAuctionMessageAsReceived(final String correlationId, final long auctionId, final AddAuctionResult result)
    {
        trackedMessages.removeIf(pendingMessage ->
        {
            final boolean exist = pendingMessage.correlationId().equals(correlationId);

            LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
            if (exist)
            {
                replySuccess(correlationId, new CreateAuctionResponse(correlationId, auctionId, result));
            }
            return exist;
        });
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markOrderViewMessageAsReceived(
        final String correlationId,
        final long securityId,
        final int traderId,
        final String clientOrderId ,
        final long orderId,
        final long submittedTime,
        final double priceValue,
        int orderQuantity,
        sbe.msg.SideEnum side
        )
    {
        trackedMessages.removeIf(pendingMessage ->
        {
            final boolean exist = pendingMessage.correlationId().equals(correlationId);

            LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
            if (exist)
            {
                OrderViewResponse responseData = new OrderViewResponse(
                        correlationId,
                        securityId,
                        traderId,
                        clientOrderId,
                        orderId,
                        submittedTime,
                        priceValue,
                        orderQuantity,
                        side
                );
                replySuccess(correlationId, responseData);
            }
            return exist;
        });
    }


    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markListParticipantsMessageAsReceived(
        final String correlationId,
        final List<ParticipantDTO> participantsList)
    {
        trackedMessages.removeIf(pendingMessage ->
        {
            final boolean exist = pendingMessage.correlationId().equals(correlationId);

            LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
            if (exist)
            {
                replySuccess(correlationId, new AddParticipantResponse(correlationId, participantsList));
            }
            return exist;
        });
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markAddAuctionBidMessageAsReceived(
            final String correlationId,
            final long auctionId,
            final AddAuctionBidResult result)
    {
        trackedMessages.removeIf(pendingMessage ->
        {
            final boolean exist = pendingMessage.correlationId().equals(correlationId);

            LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
            if (exist)
            {
                replySuccess(correlationId, new AddAuctionBidResponse(correlationId, auctionId, result));
            }
            return exist;
        });
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markListAuctionsMessageAsReceived(
            final String correlationId,
            final List<ActionDTO> actions)
    {
        trackedMessages.removeIf(pendingMessage ->
        {
            final boolean exist = pendingMessage.correlationId().equals(correlationId);

            LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
            if (exist)
            {
                replySuccess(correlationId, new ListActionsResponse(correlationId, actions));
            }
            return exist;
        });
    }

    /**
     * Duty cycle in which the pending messages are checked for timeout; if a message is found to be timed out,
     * only a single message per duty cycle is checked.
     */
    public void doWork()
    {
        final long currentTime = current.time();
        if (null == trackedMessages.peek())
        {
            return;
        }

        //not yet at timeout
        if (currentTime < trackedMessages.peek().timeoutAt())
        {
            return;
        }

        final PendingMessage timedOut = trackedMessages.poll();

        if (null == timedOut)
        {
            return;
        }

        //after timeout
        if (currentTime >= timedOut.timeoutAt())
        {
            log("Message with correlation id " + timedOut.correlationId() + " and type " +
                timedOut.messageType() + " timed out.", AttributedStyle.RED);
            trackedMessages.remove(timedOut);
        }
    }

    /**
     * Set the line reader
     * @param lineReader the line reader used for logging
     */
    public void setLineReader(final LineReader lineReader)
    {
        this.lineReader = lineReader;
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

    public void onComplete(String correlationId, CompletableFuture<ResponseWrapper> future)
    {
        futures.put(correlationId, future);
    }

    public void replySuccess(String correlationId, BaseResponse responseData) {
        var future = futures.remove(correlationId);
        if (future == null)
        {
            return;
        }

        var response = new ResponseWrapper();
        response.setData(responseData);
        response.setStatus(HttpStatus.OK.value());
        future.complete(response);
    }

    public void replyFail(final String correlationId, final BaseError error)
    {
        var future = futures.remove(correlationId);

        if (future == null)
        {
            return;
        }

        var response = new ResponseWrapper();
        response.setError(error);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        future.complete(response);
    }
}