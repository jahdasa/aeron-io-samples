package io.aeron.samples.admin.cluster;

import io.aeron.samples.admin.model.*;
import org.agrona.concurrent.EpochClock;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import sbe.msg.NewInstrumentCompleteStatus;

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
    private final Map<String, ResponseWrapper> partialData = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(50);

    private final Deque<PendingMessage> trackedMessages = new LinkedList<>();
    private final ConcurrentHashMap<String, PendingMessage> trackedMessagesMap = new ConcurrentHashMap<>();

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
        final PendingMessage message = new PendingMessage(timeoutAt, correlationId, messageType);
        trackedMessagesMap.put(correlationId, message);
        trackedMessages.add(message);
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
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

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
            trackedMessagesMap.remove(correlationId);
        }
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

        String correlationIdPeak = trackedMessages.peek().correlationId();
        if (!trackedMessagesMap.containsKey(correlationIdPeak))
        {
            trackedMessages.remove();
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

    public CompletableFuture<ResponseWrapper> onComplete(String correlationId)
    {
        final CompletableFuture<ResponseWrapper> future = new CompletableFuture<>();

        futures.put(correlationId, future);

        return future;
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

    public void markMarketDataMessageAsReceived(String correlationId, MarketDepthDTO marketDepthDTO) {
        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        final ResponseWrapper responseWrapper = partialData.get(correlationId);
        if(responseWrapper == null)
        {
            var response = new ResponseWrapper();
            response.setData(marketDepthDTO);
            response.setStatus(HttpStatus.OK.value());
            partialData.put(correlationId, response);
        }
        else
        {
            final MarketDepthDTO aggData =  (MarketDepthDTO)responseWrapper.getData();
            aggData.getLines().addAll(marketDepthDTO.getLines());
            aggData.setAskTotalVolume(aggData.getAskTotalVolume() + marketDepthDTO.getAskTotalVolume());
            aggData.setBidTotalVolume(aggData.getBidTotalVolume() + marketDepthDTO.getBidTotalVolume());
            aggData.setAskTotal(aggData.getAskTotal() + marketDepthDTO.getAskTotal());
            aggData.setBidTotal(aggData.getBidTotal() + marketDepthDTO.getBidTotal());
        }
    }

    public void markLOBMessageAsReceived(String correlationId, LimitOrderBookDTO limitOrderBookDTO) {
        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        final ResponseWrapper responseWrapper = partialData.get(correlationId);
        if(responseWrapper == null)
        {
            var response = new ResponseWrapper();
            response.setData(limitOrderBookDTO);
            response.setStatus(HttpStatus.OK.value());
            partialData.put(correlationId, response);
        }
        else
        {
            final LimitOrderBookDTO aggData =  (LimitOrderBookDTO)responseWrapper.getData();
            aggData.getOrders().addAll(limitOrderBookDTO.getOrders());
        }
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markAdminMessageAsReceived(
        final String correlationId
    )
    {
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        if (exist)
        {
            final ResponseWrapper wrapper = partialData.get(correlationId);

            BaseResponse data = new LimitOrderBookDTO();
            if(wrapper != null)
            {
                data = wrapper.getData();
            }

            replySuccess(correlationId, data);
            partialData.remove(correlationId);
            trackedMessagesMap.remove(correlationId);
        }
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markNewInstrumentMessageAsReceived(
            final String correlationId,
            final int secutiryId,
            final String code,
            final NewInstrumentCompleteStatus status
    )
    {
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        if (exist)
        {
            replySuccess(correlationId, new NewInstrumentResponse(correlationId, secutiryId, code, status));
            trackedMessagesMap.remove(correlationId);
        }
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markVwapMessageAsReceived(
            final String correlationId,
            double bidVWAP,
            double offerVWAP
    )
    {
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        if (exist)
        {
            replySuccess(correlationId, new VWAPDTO(bidVWAP, offerVWAP));
            partialData.remove(correlationId);
            trackedMessagesMap.remove(correlationId);
        }
    }

    /**
     * Mark a message as received
     * @param correlationId the correlation id of the message
     */
    public void markBBOMessageAsReceived(
            final String correlationId,
            final long bidQuantity,
            final long offerQuantity,
            final double bidValue,
            final double offerValue
    )
    {
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        if (exist)
        {
            replySuccess(correlationId, new BBODTO(bidQuantity, offerQuantity,bidValue, offerValue));
            partialData.remove(correlationId);
            trackedMessagesMap.remove(correlationId);
        }
    }

}