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

import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import io.aeron.samples.admin.model.ActionDTO;
import io.aeron.samples.admin.model.ParticipantDTO;
import io.aeron.samples.admin.util.EnvironmentUtil;
import io.aeron.samples.cluster.protocol.AddAuctionBidCommandResultDecoder;
import io.aeron.samples.cluster.protocol.AddAuctionBidResult;
import io.aeron.samples.cluster.protocol.AddAuctionResult;
import io.aeron.samples.cluster.protocol.AddParticipantCommandResultDecoder;
import io.aeron.samples.cluster.protocol.AuctionListDecoder;
import io.aeron.samples.cluster.protocol.AuctionStatus;
import io.aeron.samples.cluster.protocol.AuctionUpdateEventDecoder;
import io.aeron.samples.cluster.protocol.CreateAuctionCommandResultDecoder;
import io.aeron.samples.cluster.protocol.MessageHeaderDecoder;
import io.aeron.samples.cluster.protocol.NewAuctionEventDecoder;
import io.aeron.samples.cluster.protocol.ParticipantListDecoder;
import org.agrona.DirectBuffer;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sbe.msg.*;
import sbe.msg.marketData.*;
import sbe.msg.marketData.PriceDecoder;
import sbe.msg.marketData.SideEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Admin client egress listener
 */
public class AdminClientEgressListener implements EgressListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientEgressListener.class);

    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final sbe.msg.MessageHeaderDecoder sbeMessageHeaderDecoder = new sbe.msg.MessageHeaderDecoder();
    private final sbe.msg.marketData.MessageHeaderDecoder sbeMarketDataMessageHeaderDecoder = new sbe.msg.marketData.MessageHeaderDecoder();
    private final sbe.msg.MessageHeaderDecoder sbeMsgMessageHeaderDecoder = new sbe.msg.MessageHeaderDecoder();
    private final AuctionUpdateEventDecoder auctionUpdateEventDecoder = new AuctionUpdateEventDecoder();
    private final AddParticipantCommandResultDecoder addParticipantDecoder = new AddParticipantCommandResultDecoder();
    private final CreateAuctionCommandResultDecoder createAuctionResultDecoder =
        new CreateAuctionCommandResultDecoder();
    private final NewAuctionEventDecoder newAuctionEventDecoder = new NewAuctionEventDecoder();
    private final AddAuctionBidCommandResultDecoder addBidResultDecoder = new AddAuctionBidCommandResultDecoder();
    private final AuctionListDecoder auctionListDecoder = new AuctionListDecoder();
    private final ParticipantListDecoder participantListDecoder = new ParticipantListDecoder();
    private final PendingMessageManager pendingMessageManager;
    private LineReader lineReader;

    /**
     * Constructor
     * @param pendingMessageManager the manager for pending messages
     */
    public AdminClientEgressListener(final PendingMessageManager pendingMessageManager)
    {
        this.pendingMessageManager = pendingMessageManager;
    }

    final BestBidOfferDecoder bestBidOfferDecoder = new BestBidOfferDecoder();
    final UnitHeaderDecoder unitHeaderDecoder = new UnitHeaderDecoder();
    final OrderExecutedWithPriceSizeDecoder orderExecutedWithPriceSizeDecoder = new OrderExecutedWithPriceSizeDecoder();
    final AddOrderDecoder addOrderDecoder = new AddOrderDecoder();

    @Override
    public void onMessage(
        final long clusterSessionId,
        final long timestamp,
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final Header header)
    {
        if (length < MessageHeaderDecoder.ENCODED_LENGTH)
        {
            LOGGER.warn("Message too short");
            return;
        }
        messageHeaderDecoder.wrap(buffer, offset);

        switch (messageHeaderDecoder.templateId())
        {
            case MarketDepthDecoder.TEMPLATE_ID ->
            {
                MarketDepthDecoder marketDepthDecoder = new MarketDepthDecoder();
                marketDepthDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                int securityId = marketDepthDecoder.securityId();
                AtomicLong bidTotalVolume = new AtomicLong(0L);
                AtomicLong bidTotal = new AtomicLong(0L);
                AtomicLong offerTotalVolume = new AtomicLong(0L);
                AtomicLong offerTotal = new AtomicLong(0L);
                marketDepthDecoder.depth().iterator().forEachRemaining(depthDecoder ->
                {
                    int count = depthDecoder.orderCount();
                    long quantity = depthDecoder.quantity();
                    sbe.msg.PriceDecoder priceDecoder = depthDecoder.price();
                    sbe.msg.SideEnum side = depthDecoder.side();

                    double priceValue = priceDecoder.mantissa() * Math.pow(10, priceDecoder.exponent());

                    if(side== sbe.msg.SideEnum.Buy){
                        bidTotalVolume.addAndGet(quantity);
                        bidTotal.addAndGet((long) (quantity*priceValue));
                    }
                    else
                    {
                        offerTotalVolume.addAndGet(quantity);
                        offerTotal.addAndGet((long) (quantity*priceValue));
                    }

                    log(
                            "securityId: " + securityId +
                                    " side: " + side +
                                    "@" + count + "@" + quantity + "@" + priceValue, AttributedStyle.YELLOW);
                });

                log(
                        "securityId: " + securityId +
                                " b-t/v: " + bidTotal + "@" + bidTotalVolume + " o-t/v: " + offerTotal + "@" + offerTotalVolume , AttributedStyle.YELLOW);
            }
            case VWAPDecoder.TEMPLATE_ID -> {
                VWAPDecoder vwapDecoder = new VWAPDecoder();
                vwapDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                sbe.msg.PriceDecoder priceDecoderBid = vwapDecoder.bidVWAP();
                sbe.msg.PriceDecoder priceDecoderOffer = vwapDecoder.offerVWAP();

                double priceBidValue = priceDecoderBid.mantissa() * Math.pow(10, priceDecoderBid.exponent());
                double priceOfferValue = priceDecoderOffer.mantissa() * Math.pow(10, priceDecoderOffer.exponent());

                log("VWAP: " + VWAPDecoder.TEMPLATE_ID +
                        " bidVWAP/offerVWAP: " + priceBidValue + "@" + priceOfferValue
                , AttributedStyle.YELLOW);

            }
            case LOBDecoder.TEMPLATE_ID -> {
                LOBDecoder lobDecoder = new LOBDecoder();
                lobDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                int securityId = lobDecoder.securityId();

                lobDecoder.orders().iterator().forEachRemaining(ordersDecoder ->
                {
                    String clientOrderId = ordersDecoder.clientOrderId();
                    int count = ordersDecoder.count();
                    int orderId = ordersDecoder.orderId();
                    int orderQuantity = ordersDecoder.orderQuantity();
                    sbe.msg.PriceDecoder priceDecoder = ordersDecoder.price();
                    sbe.msg.SideEnum side = ordersDecoder.side();

                    double priceValue = priceDecoder.mantissa() * Math.pow(10, priceDecoder.exponent());

                    log(
                        "securityId: " + securityId +
                                " clientOrderId: "+ clientOrderId +
                                " orderId: " + orderId +
                                " side: " + side +
                            " orderQuantity/priceValue: " + count + "@" + orderQuantity + "@" + priceValue, AttributedStyle.YELLOW);
                });
            }
            case AdminDecoder.TEMPLATE_ID -> {
                AdminDecoder adminDecoder = new AdminDecoder();
                adminDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                int securityId = adminDecoder.securityId();
                AdminTypeEnum adminTypeEnum = adminDecoder.adminMessage();

                log("Admin Message:" + AdminDecoder.TEMPLATE_ID + " adminTypeEnum: " + adminTypeEnum +
                    " securityId: " + securityId, AttributedStyle.YELLOW);
            }
            case ExecutionReportDecoder.TEMPLATE_ID -> {
                ExecutionReportDecoder executionReportDecoder = new ExecutionReportDecoder();
                executionReportDecoder.wrapAndApplyHeader(buffer, offset, sbeMsgMessageHeaderDecoder);

                short partitionId = executionReportDecoder.partitionId();
                int sequenceNumber = executionReportDecoder.sequenceNumber();
                String executionID = executionReportDecoder.executionID();
                final String clientOrderId = executionReportDecoder.clientOrderId();
                final long orderId = executionReportDecoder.orderId();
                ExecutionTypeEnum executionTypeEnum = executionReportDecoder.executionType();
                OrderStatusEnum orderStatusEnum = executionReportDecoder.orderStatus();
                RejectCode rejectCode = executionReportDecoder.rejectCode();
                int leavesQuantity = executionReportDecoder.leavesQuantity();
                ContainerEnum container = executionReportDecoder.container();
                final long securityId = executionReportDecoder.securityId();
                sbe.msg.SideEnum side = executionReportDecoder.side();
                String traderMnemonic = executionReportDecoder.traderMnemonic();
                String account = executionReportDecoder.account();
                IsMarketOpsRequestEnum marketOpsRequest = executionReportDecoder.isMarketOpsRequest();
                long transactTime = executionReportDecoder.transactTime();
                OrderBookEnum orderBookEnum = executionReportDecoder.orderBook();


                executionReportDecoder.fillsGroup().iterator().forEachRemaining(fillsGroupDecoder ->
                {
                    sbe.msg.PriceDecoder priceDecoder = fillsGroupDecoder.fillPrice();
                    double priceValue = priceDecoder.mantissa() * Math.pow(10, priceDecoder.exponent());
                    int fillQty = fillsGroupDecoder.fillQty();

                    log(" fillQty/fillPrice: " + fillQty + "@" + priceValue, AttributedStyle.YELLOW);
                });

                log("Execution report: " + ExecutionReportDecoder.TEMPLATE_ID +
                    " partitionId: " + partitionId +
                    " sequenceNumber: " + sequenceNumber +
                    " executionID: '" + executionID + "'" +
                    " clientOrderId: '" + clientOrderId + "'" +
                    " orderId: " + orderId +
                    " executionTypeEnum: " + executionTypeEnum.name() +
                    " orderStatusEnum: " + orderStatusEnum.name() +
                    " rejectCode: " + rejectCode.name() +
                    " leavesQuantity: " + leavesQuantity +
                    " container: " + container.name() +
                    " securityId: " + securityId +
                    " side: " + side.name() +
                    " traderMnemonic: '" + traderMnemonic + "'" +
                    " account: '" + account + "'" +
                    " marketOpsRequest: '" + marketOpsRequest.name() + "'" +
                    " transactTime: '" + transactTime + "'" +
                    " orderBookEnum: '" + orderBookEnum.name() + "'",
                    AttributedStyle.YELLOW);

            }
            case OrderViewDecoder.TEMPLATE_ID -> {
                OrderViewDecoder orderViewDecoder = new OrderViewDecoder();
                orderViewDecoder.wrapAndApplyHeader(buffer, offset, sbeMsgMessageHeaderDecoder);

                final long securityId = orderViewDecoder.securityId();
                final String clientOrderId = orderViewDecoder.clientOrderId();
                final long orderId = orderViewDecoder.orderId();
                final long submittedTime = orderViewDecoder.submittedTime();

                final sbe.msg.PriceDecoder price = orderViewDecoder.price();
                final double priceValue = price.mantissa() * Math.pow(10, price.exponent());

                int orderQuantity = orderViewDecoder.orderQuantity();
                sbe.msg.SideEnum side = orderViewDecoder.side();

                log("Order view: " + OrderViewDecoder.TEMPLATE_ID + " securityId: " +
                    securityId + " clientOrderId: '" + clientOrderId + "' orderId: " + orderId +
                    " submittedTime: " + submittedTime + " side: " + side.name() +
                    " orderQuantity: " + orderQuantity + " price: " + priceValue,
                    AttributedStyle.YELLOW);
            }
            case AddOrderDecoder.TEMPLATE_ID -> {
                addOrderDecoder.wrapAndApplyHeader(buffer, offset, sbeMarketDataMessageHeaderDecoder);
                MessageTypeEnum messageTypeEnum = addOrderDecoder.messageType();
                long nanosecond = addOrderDecoder.nanosecond();
                long orderId = addOrderDecoder.orderId();
                SideEnum side = addOrderDecoder.side();
                long quantity = addOrderDecoder.quantity();
                long instrumentId = addOrderDecoder.instrumentId();

                PriceDecoder price = addOrderDecoder.price();
                final double priceValue = price.mantissa() * Math.pow(10, price.exponent());

                Flags flags = addOrderDecoder.flags();

                log("Add order: " + messageTypeEnum.name() + " " +
                    orderId + " " + side.name() + " " + quantity + "@" + priceValue +
                    " instrumentId: " + instrumentId +
                    " flags: " + flags.name(),
                    AttributedStyle.YELLOW);
            }
            case OrderExecutedWithPriceSizeDecoder.TEMPLATE_ID ->
            {
                orderExecutedWithPriceSizeDecoder.wrapAndApplyHeader(buffer, offset, sbeMarketDataMessageHeaderDecoder);
                MessageTypeEnum messageTypeEnum = orderExecutedWithPriceSizeDecoder.messageType();
                long nanosecond = orderExecutedWithPriceSizeDecoder.nanosecond();
                final long orderId = orderExecutedWithPriceSizeDecoder.orderId();
                long executedQuantity = orderExecutedWithPriceSizeDecoder.executedQuantity();
//                long displayQuantity = orderExecutedWithPriceSizeDecoder.displayQuantity();
                long tradeId = orderExecutedWithPriceSizeDecoder.tradeId();
                PrintableEnum printable = orderExecutedWithPriceSizeDecoder.printable();
                final PriceDecoder price = orderExecutedWithPriceSizeDecoder.price();
                final double priceValue = price.mantissa() * Math.pow(10, price.exponent());

                long instrumentId = orderExecutedWithPriceSizeDecoder.instrumentId();
                long clientOrderId = orderExecutedWithPriceSizeDecoder.clientOrderId();
                long executedTime = orderExecutedWithPriceSizeDecoder.executedTime();

                log("Order executed: " + OrderExecutedWithPriceSizeDecoder.TEMPLATE_ID +
                    " messageTypeEnum: " + messageTypeEnum.name() +
                    " orderId: " + orderId +
                    " executedQuantity/price: " + executedQuantity + "@" + priceValue +
//                    " displayQuantity/price: " + displayQuantity + "@" + priceValue +
                    " tradeId: " + tradeId +
                    " printable: " + printable.name() +
                    " instrumentId: " + instrumentId +
                    " clientOrderId: " + clientOrderId +
                    " executedTime: " + executedTime,
                    AttributedStyle.YELLOW);

            }
            case UnitHeaderDecoder.TEMPLATE_ID -> {
                sbeMessageHeaderDecoder.wrap(buffer, offset);

                unitHeaderDecoder.wrapAndApplyHeader(buffer, offset, sbeMarketDataMessageHeaderDecoder);
                final int messageCount = unitHeaderDecoder.messageCount();
                byte marketDataGroup = unitHeaderDecoder.marketDataGroup();

                log("Unit header: " + messageHeaderDecoder.templateId() + " " +
                    messageCount + " messages in group: " + marketDataGroup,
                    AttributedStyle.YELLOW);
            }
            case BestBidOfferDecoder.TEMPLATE_ID ->
            {
                bestBidOfferDecoder.wrapAndApplyHeader(buffer, offset, sbeMarketDataMessageHeaderDecoder);

                long instrumentId = bestBidOfferDecoder.instrumentId();
                final long bidQuantity = bestBidOfferDecoder.bidQuantity();
                final long offerQuantity = bestBidOfferDecoder.offerQuantity();

                PriceDecoder bid = bestBidOfferDecoder.bid();
                final double bidValue = bid.mantissa() * Math.pow(10, bid.exponent());

                PriceDecoder offer = bestBidOfferDecoder.offer();
                final double offerValue = offer.mantissa() * Math.pow(10, offer.exponent());

                log("BBO: " + BestBidOfferDecoder.TEMPLATE_ID + " security "+ instrumentId +
                            " bidQuantity/bid: " + bidQuantity + "@" + bidValue +
                            " offerQuantity/offerValue: " + offerQuantity + "@" + offerValue,
                    AttributedStyle.YELLOW);
            }
            case AddParticipantCommandResultDecoder.TEMPLATE_ID ->
            {
                addParticipantDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                final String correlationId = addParticipantDecoder.correlationId();
                final long addedId = addParticipantDecoder.participantId();
                log("Participant added with id " + addedId, AttributedStyle.GREEN);
                pendingMessageManager.markMessageAsReceived(correlationId);
            }
            case CreateAuctionCommandResultDecoder.TEMPLATE_ID ->
            {
                createAuctionResultDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                final long auctionId = createAuctionResultDecoder.auctionId();
                final AddAuctionResult result = createAuctionResultDecoder.result();
                final String correlationId = createAuctionResultDecoder.correlationId();

                pendingMessageManager.markCreateAuctionMessageAsReceived(correlationId, auctionId, result);

                if (result.equals(AddAuctionResult.SUCCESS))
                {
                    log("Auction added with id: " + auctionId,
                        AttributedStyle.GREEN);
                }
                else
                {
                    log("Add auction rejected with reason: " + result.name(), AttributedStyle.RED);
                }
            }
            case NewAuctionEventDecoder.TEMPLATE_ID ->
            {
                newAuctionEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                final long auctionId = newAuctionEventDecoder.auctionId();
                final String auctionName = newAuctionEventDecoder.name();
                log("New auction: " + "'" + auctionName + "' (" + auctionId + ")", AttributedStyle.CYAN);
            }
            case AddAuctionBidCommandResultDecoder.TEMPLATE_ID ->
            {
                addBidResultDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                final long auctionId = addBidResultDecoder.auctionId();
                final AddAuctionBidResult result = addBidResultDecoder.result();
                final String correlationId = addBidResultDecoder.correlationId();

                pendingMessageManager.markAddAuctionBidMessageAsReceived(correlationId, auctionId, result);
                if (result.equals(AddAuctionBidResult.SUCCESS))
                {
                    log("Bid added to auction " + auctionId, AttributedStyle.GREEN);
                }
                else
                {
                    log("Add bid rejected with reason: " + result.name(), AttributedStyle.RED);
                }
            }
            case AuctionUpdateEventDecoder.TEMPLATE_ID -> displayAuctionUpdate(buffer, offset);
            case AuctionListDecoder.TEMPLATE_ID -> displayAuctions(buffer, offset);
            case ParticipantListDecoder.TEMPLATE_ID -> displayParticipants(buffer, offset);
            default -> log("unknown message type: " + messageHeaderDecoder.templateId(), AttributedStyle.RED);
        }
    }

    private void displayAuctionUpdate(final DirectBuffer buffer, final int offset)
    {
        auctionUpdateEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        final long auctionId = auctionUpdateEventDecoder.auctionId();
        final AuctionStatus auctionStatus = auctionUpdateEventDecoder.status();
        final int bidCount = auctionUpdateEventDecoder.bidCount();
        final long currentPrice = auctionUpdateEventDecoder.currentPrice();
        final long winningParticipantId = auctionUpdateEventDecoder.winningParticipantId();
        pendingMessageManager.markMessageAsReceived(auctionUpdateEventDecoder.correlationId());

        if (bidCount == 0)
        {
            if (auctionStatus.equals(AuctionStatus.CLOSED))
            {
                log("Auction " + auctionId + " has ended. There were no bids.", AttributedStyle.YELLOW);
            }
            else
            {
                log("Auction " + auctionId + " is now in state " +
                    auctionStatus.name() + ". There have been " +
                    auctionUpdateEventDecoder.bidCount() + " bids.", AttributedStyle.YELLOW);
            }
        }
        else
        {
            if (auctionStatus.equals(AuctionStatus.CLOSED))
            {
                final int participantId = EnvironmentUtil.tryGetParticipantId();
                if (participantId != 0 && winningParticipantId == participantId)
                {
                    log("Auction " + auctionId + " won! Total " + bidCount + " bids. Winning price was " +
                        currentPrice, AttributedStyle.GREEN);
                }
                else
                {
                    log("Auction " + auctionId + " has ended. Total " + bidCount + " bids. Winning price was " +
                        currentPrice + ", and the winning bidder is " + winningParticipantId, AttributedStyle.YELLOW);
                }
            }
            else
            {
                log("Auction update event: auction " + auctionId + " is now in state " +
                    auctionStatus.name() + ". Total " + bidCount + " bids. Current price is " +
                    currentPrice + ". The winning bidder is " + winningParticipantId,
                    AttributedStyle.YELLOW);
            }
        }
    }

    private void displayParticipants(final DirectBuffer buffer, final int offset)
    {
        participantListDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);

        final List<ParticipantDTO> participantsList = new ArrayList<>();
        final ParticipantListDecoder.ParticipantsDecoder participants = participantListDecoder.participants();
        final int count = participants.count();
        if (0 == count)
        {
            log("No participants exist in the cluster.",
                AttributedStyle.YELLOW);
        }
        else
        {
            log("Participant count: " + count, AttributedStyle.YELLOW);
            while (participants.hasNext())
            {
                participants.next();
                final long participantId = participants.participantId();
                final String name = participants.name();

                participantsList.add(new ParticipantDTO(participantId, name));
                log("Participant: id " + participantId + " name: '" + name + "'", AttributedStyle.YELLOW);
            }
        }

        pendingMessageManager.markListParticipantsMessageAsReceived(participantListDecoder.correlationId(), participantsList);
    }

    private void displayAuctions(final DirectBuffer buffer, final int offset)
    {
        auctionListDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        final AuctionListDecoder.AuctionsDecoder auction = auctionListDecoder.auctions();

        final List<ActionDTO> actions = new ArrayList<>();
        final int count = auction.count();
        if (0 == count)
        {
            log("No auctions exist in the cluster. Closed auctions are deleted automatically.",
                AttributedStyle.YELLOW);
        }
        else
        {
            log("Auction count: " + count, AttributedStyle.YELLOW);
            while (auction.hasNext())
            {
                auction.next();

                final long auctionId = auction.auctionId();
                final long createdBy = auction.createdByParticipantId();
                final long startTime = auction.startTime();
                final long endTime = auction.endTime();
                final long winningParticipantId = auction.winningParticipantId();
                final long currentPrice = auction.currentPrice();
                final AuctionStatus status = auction.status();
                final String name = auction.name();

                log("Auction '" + name + "' with id " + auctionId + " created by " + createdBy +
                    " is now in state " + status.name(), AttributedStyle.YELLOW);

                final int participantId = EnvironmentUtil.tryGetParticipantId();
                if (participantId != 0 && winningParticipantId == participantId)
                {
                    log(" Winning auction with price " +
                        currentPrice, AttributedStyle.YELLOW);
                }
                else if (winningParticipantId != -1)
                {
                    log(" Current winning participant " + winningParticipantId + " with price " +
                        currentPrice, AttributedStyle.YELLOW);
                }

                final ActionDTO action = new ActionDTO(
                    auctionId,
                    name,
                    createdBy,
                    startTime,
                    endTime,
                    winningParticipantId,
                    currentPrice,
                    status);
                actions.add(action);
            }
        }
        pendingMessageManager.markListAuctionsMessageAsReceived(auctionListDecoder.correlationId(), actions);
    }

    @Override
    public void onSessionEvent(
        final long correlationId,
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final EventCode code,
        final String detail)
    {
        if (code != EventCode.OK)
        {
            log("Session event: " + code.name() + " " + detail + ". leadershipTermId=" + leadershipTermId,
                AttributedStyle.YELLOW);
        }
    }

    @Override
    public void onNewLeader(
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final String ingressEndpoints)
    {
        log("New Leader: " + leaderMemberId + ". leadershipTermId=" + leadershipTermId, AttributedStyle.YELLOW);
    }

    /**
     * Sets the terminal
     *
     * @param lineReader the lineReader
     */
    public void setLineReader(final LineReader lineReader)
    {
        this.lineReader = lineReader;
    }

    /**
     * Logs a message to the terminal if available or to the logger if not
     *
     * @param message message to log
     * @param color   message color to use
     */
    private void log(final String message, final int color)
    {
        LineReaderHelper.log(lineReader, message, color);
    }
}
