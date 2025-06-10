package io.aeron.samples.admin.cluster;

import io.aeron.samples.admin.model.*;
import org.agrona.concurrent.EpochClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import sbe.msg.NewInstrumentCompleteStatus;
import sbe.msg.SideEnum;

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
            LOGGER.error("Message with correlation id " + timedOut.correlationId() + " and type " +
                timedOut.messageType() + " timed out.");
            trackedMessages.remove(timedOut);
        }
    }


    public CompletableFuture<ResponseWrapper> onComplete(final String correlationId)
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

    public void replyFail(final String correlationId, final BaseError error, final HttpStatus httpStatus)
    {
        var future = futures.remove(correlationId);
        trackedMessagesMap.remove(correlationId);

        if (future == null)
        {
            return;
        }

        var response = new ResponseWrapper();
        response.setError(error);
        response.setStatus(httpStatus.value());
        future.complete(response);

    }

    public void markMarketDataMessageAsReceived(String correlationId, MarketDepthDTO marketDepthDTO)
    {
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

    public void markLOBMessageAsReceived(String correlationId, LimitOrderBookDTO limitOrderBookDTO)
    {
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
    public void markMarketDepth(final String correlationId)
    {
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        if (exist)
        {
            final ResponseWrapper wrapper = partialData.get(correlationId);

            MarketDepthDTO data = new MarketDepthDTO();
            if(wrapper != null)
            {
                data = (MarketDepthDTO) wrapper.getData();
                final List<MarketDepthDTO.MarketDepthLine> lines = data.getLines();

                final List<MarketDepthDTO.MarketDepthLine> bidOrders = lines.stream()
                        .filter(line -> line.getSide() == SideEnum.Buy)
                        .sorted(Comparator.comparing(MarketDepthDTO.MarketDepthLine::getPrice).reversed())
                        .toList();

                if (!bidOrders.isEmpty())
                {
                    final MarketDepthDTO.MarketDepthLine firstBid = bidOrders.getFirst();
                    if(firstBid != null)
                    {
                        firstBid.setTotal(firstBid.getQuantity());
                    }
                    for (int i = 1; i < bidOrders.size(); i++)
                    {
                        final MarketDepthDTO.MarketDepthLine current = bidOrders.get(i);
                        current.setTotal(current.getQuantity().add(bidOrders.get(i - 1).getTotal()));
                    }
                }


                final List<MarketDepthDTO.MarketDepthLine> askOrders = lines.stream()
                        .filter(line -> line.getSide() == SideEnum.Sell)
                        .sorted(Comparator.comparing(MarketDepthDTO.MarketDepthLine::getPrice))
                        .toList();

                if (!askOrders.isEmpty())
                {
                    final MarketDepthDTO.MarketDepthLine firstAsk = askOrders.getFirst();
                    if (firstAsk != null) {
                        firstAsk.setTotal(firstAsk.getQuantity());
                    }

                    for (int i = 1; i < askOrders.size(); i++) {
                        final MarketDepthDTO.MarketDepthLine current = askOrders.get(i);
                        current.setTotal(current.getQuantity().add(askOrders.get(i - 1).getTotal()));
                    }
                }

            }

            if(data.getLines() != null)
            {
                data.getLines().sort(Comparator.comparing(MarketDepthDTO.MarketDepthLine::getPrice).reversed());
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
    public void markListInstrumentsMessageAsReceived(
            final String correlationId,
            final List<InstrumentDTO> instruments
    )
    {
        final boolean exist = trackedMessagesMap.containsKey(correlationId);

        LOGGER.info("markMessageAsReceived correlationId: {}", correlationId);
        if (exist)
        {
            instruments.sort(Comparator.comparing(InstrumentDTO::getSecurityId));
            replySuccess(correlationId, new ListInstrumentsResponse(correlationId, instruments));
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