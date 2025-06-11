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
import io.aeron.samples.admin.model.InstrumentDTO;
import io.aeron.samples.admin.model.LimitOrderBookDTO;
import io.aeron.samples.admin.model.MarketDepthDTO;
import io.aeron.samples.cluster.protocol.MessageHeaderDecoder;
import org.agrona.DirectBuffer;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sbe.msg.*;
import sbe.msg.marketData.*;
import sbe.msg.marketData.PriceDecoder;
import sbe.msg.marketData.SideEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final PendingMessageManager pendingMessageManager;

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
            case ListInstrumentsMessageResponseDecoder.TEMPLATE_ID ->
            {
                final ListInstrumentsMessageResponseDecoder decoder = new ListInstrumentsMessageResponseDecoder();
                decoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                final String correlationId = decoder.correlationId();

                final List<InstrumentDTO> instrumentDTOS = new ArrayList<>();

                decoder.instruments().forEach(instrument ->
                {
                    final int securityId = instrument.securityId();
                    final String code = instrument.code().trim();
                    final String name = instrument.name().trim();

                    instrumentDTOS.add(new InstrumentDTO(securityId, code, name));

                });

                pendingMessageManager.markListInstrumentsMessageAsReceived(correlationId.trim(), instrumentDTOS);
            }
            case NewInstrumentCompleteDecoder.TEMPLATE_ID ->
            {
                NewInstrumentCompleteDecoder completeDecoder = new NewInstrumentCompleteDecoder();
                completeDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                final int securityId = completeDecoder.securityId();
                final String code = completeDecoder.code();
                NewInstrumentCompleteStatus status = completeDecoder.status();


                LOGGER.debug("New instrument complete: " + NewInstrumentCompleteDecoder.TEMPLATE_ID);

                final String correlationId = NewInstrumentDecoder.TEMPLATE_ID + "@" +
                        securityId + "@" +
                        code.trim() + "@" +
                        "1";

                pendingMessageManager.markNewInstrumentMessageAsReceived(correlationId, securityId, code, status);
            }
            case MarketDepthDecoder.TEMPLATE_ID ->
            {
                MarketDepthDecoder marketDepthDecoder = new MarketDepthDecoder();
                marketDepthDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                int securityId = marketDepthDecoder.securityId();
                AtomicLong bidTotalVolume = new AtomicLong(0L);
                AtomicLong bidTotal = new AtomicLong(0L);
                AtomicLong offerTotalVolume = new AtomicLong(0L);
                AtomicLong offerTotal = new AtomicLong(0L);

                final MarketDepthDTO marketDepthDTO = new MarketDepthDTO();
                marketDepthDTO.setSecurityId(securityId);

                final List<MarketDepthDTO.MarketDepthLine> lines = new ArrayList<>();
                marketDepthDTO.setLines(lines);

                marketDepthDecoder.depth().iterator().forEachRemaining(depthDecoder ->
                {
                    int count = depthDecoder.orderCount();
                    long quantity = depthDecoder.quantity();
                    sbe.msg.PriceDecoder priceDecoder = depthDecoder.price();
                    sbe.msg.SideEnum side = depthDecoder.side();

                    double priceValue = priceDecoder.mantissa() * Math.pow(10, priceDecoder.exponent());

                    if(side == sbe.msg.SideEnum.BUY)
                    {
                        bidTotalVolume.addAndGet(quantity);
                        bidTotal.addAndGet((long) (quantity*priceValue));
                    }
                    else if(side == sbe.msg.SideEnum.SELL)
                    {
                        offerTotalVolume.addAndGet(quantity);
                        offerTotal.addAndGet((long) (quantity*priceValue));
                    }

/*                    LOGGER.info(
                        "securityId: " + securityId +
                        " side: " + side +
                        "@" + count + "@" + quantity + "@" + priceValue);*/

                    final MarketDepthDTO.MarketDepthLine line = new MarketDepthDTO.MarketDepthLine();
                    line.setCount(count);
                    line.setPrice(BigDecimal.valueOf(priceValue).setScale(2, RoundingMode.HALF_UP));
                    line.setSide(side);
                    line.setQuantity(BigDecimal.valueOf(quantity).divide(BigDecimal.valueOf(1000_000), 8, RoundingMode.HALF_UP));
                    lines.add(line);
                });

/*                LOGGER.info(
                    "securityId: " + securityId +
                    " b-t/v: " + bidTotal + "@" + bidTotalVolume + " o-t/v: " + offerTotal + "@" + offerTotalVolume);*/

                marketDepthDTO.setBidTotalVolume(bidTotalVolume.get());
                marketDepthDTO.setBidTotal(bidTotal.get());
                marketDepthDTO.setAskTotalVolume(offerTotalVolume.get());
                marketDepthDTO.setAskTotal(offerTotal.get());

                // placeorder-tid@side@security@clientOrderId@trader@client
                final String correlationId = AdminDecoder.TEMPLATE_ID + "@" +
                        "MarketDepth" + "@" +
                        securityId + "@" +
                        "1" + "@" +
                        "1" + "@" +
                        "1";

                pendingMessageManager.markMarketDataMessageAsReceived(correlationId, marketDepthDTO);

                LOGGER.debug("correlationId: " + correlationId);

            }
            case VWAPDecoder.TEMPLATE_ID -> {
                VWAPDecoder vwapDecoder = new VWAPDecoder();
                vwapDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                sbe.msg.PriceDecoder priceDecoderBid = vwapDecoder.bidVWAP();
                sbe.msg.PriceDecoder priceDecoderOffer = vwapDecoder.offerVWAP();

                double priceBidValue = priceDecoderBid.mantissa() * Math.pow(10, priceDecoderBid.exponent());
                double priceOfferValue = priceDecoderOffer.mantissa() * Math.pow(10, priceDecoderOffer.exponent());

                LOGGER.debug("VWAP: " + VWAPDecoder.TEMPLATE_ID +
                        " bidVWAP/offerVWAP: " + priceBidValue + "@" + priceOfferValue);

                final String correlationId = AdminDecoder.TEMPLATE_ID + "@" +
                        "VWAP" + "@" +
                        vwapDecoder.securityId() + "@" +
                        "1" + "@" +
                        "1" + "@" +
                        "1";

                pendingMessageManager.markVwapMessageAsReceived(correlationId, priceBidValue, priceOfferValue);
            }
            case LOBDecoder.TEMPLATE_ID -> {
                LOBDecoder lobDecoder = new LOBDecoder();
                lobDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                int securityId = lobDecoder.securityId();

                final LimitOrderBookDTO limitOrderBookDTO = new LimitOrderBookDTO();
                limitOrderBookDTO.setSecurityId(securityId);

                final List<LimitOrderBookDTO.OrderDTO> orders = new ArrayList<>();
                limitOrderBookDTO.setOrders(orders);

                lobDecoder.orders().iterator().forEachRemaining(ordersDecoder ->
                {
                    String clientOrderId = ordersDecoder.clientOrderId();
                    int orderId = ordersDecoder.orderId();
                    int orderQuantity = ordersDecoder.orderQuantity();
                    sbe.msg.PriceDecoder priceDecoder = ordersDecoder.price();
                    sbe.msg.SideEnum side = ordersDecoder.side();

                    double priceValue = priceDecoder.mantissa() * Math.pow(10, priceDecoder.exponent());

/*                    log(
                        "securityId: " + securityId +
                                " clientOrderId: "+ clientOrderId +
                                " orderId: " + orderId +
                                " side: " + side +
                            " orderQuantity/priceValue: " + count + "@" + orderQuantity + "@" + priceValue, AttributedStyle.YELLOW);*/

                    final LimitOrderBookDTO.OrderDTO order = new LimitOrderBookDTO.OrderDTO();
                    order.setOrderId(orderId);
                    order.setClientOrderId(clientOrderId);
                    order.setPrice(priceValue);
                    order.setSide(side);
                    order.setQuantity(orderQuantity);
                    orders.add(order);
                });

                // placeorder-tid@side@security@clientOrderId@trader@client
                final String correlationId = AdminDecoder.TEMPLATE_ID + "@" +
                    "LOB" + "@" +
                    securityId + "@" +
                    "1" + "@" +
                    "1" + "@" +
                    "1";

                pendingMessageManager.markLOBMessageAsReceived(correlationId, limitOrderBookDTO);
            }
            case AdminDecoder.TEMPLATE_ID -> {
                AdminDecoder adminDecoder = new AdminDecoder();
                adminDecoder.wrapAndApplyHeader(buffer, offset, sbeMessageHeaderDecoder);

                int securityId = adminDecoder.securityId();
                AdminTypeEnum adminTypeEnum = adminDecoder.adminMessage();

                LOGGER.debug("Admin Message:" + AdminDecoder.TEMPLATE_ID + " adminTypeEnum: " + adminTypeEnum +
                    " securityId: " + securityId);

                if(adminTypeEnum == AdminTypeEnum.EndMarketDepth)
                {
                    final String correlationId = AdminDecoder.TEMPLATE_ID + "@" +
                        "MarketDepth" + "@" +
                        securityId + "@" +
                        "1" + "@" +
                        "1" + "@" +
                        "1";

                    pendingMessageManager.markMarketDepth(correlationId);
                }
                else if(adminTypeEnum == AdminTypeEnum.EndLOB)
                {
                    final String correlationId = AdminDecoder.TEMPLATE_ID + "@" +
                        "LOB" + "@" +
                        securityId + "@" +
                        "1" + "@" +
                        "1" + "@" +
                        "1";

                    pendingMessageManager.markAdminMessageAsReceived(correlationId);
                }


            }
            case ExecutionReportDecoder.TEMPLATE_ID -> {
                final ExecutionReportDecoder executionReportDecoder = new ExecutionReportDecoder();
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
                int traderId = executionReportDecoder.traderId();
                String account = executionReportDecoder.account();
                IsMarketOpsRequestEnum marketOpsRequest = executionReportDecoder.isMarketOpsRequest();
                long transactTime = executionReportDecoder.transactTime();
                OrderBookEnum orderBookEnum = executionReportDecoder.orderBook();

                executionReportDecoder.fillsGroup().iterator().forEachRemaining(fillsGroupDecoder ->
                {
                    sbe.msg.PriceDecoder priceDecoder = fillsGroupDecoder.fillPrice();
                    double priceValue = priceDecoder.mantissa() * Math.pow(10, priceDecoder.exponent());
                    int fillQty = fillsGroupDecoder.fillQty();

                    LOGGER.debug(" fillQty/fillPrice: " + fillQty + "@" + priceValue);
                });

                LOGGER.info("Execution report: " + ExecutionReportDecoder.TEMPLATE_ID +
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
                    " traderId: '" + traderId + "'" +
                    " account: '" + account + "'" +
                    " marketOpsRequest: '" + marketOpsRequest.name() + "'" +
                    " transactTime: '" + transactTime + "'" +
                    " orderBookEnum: '" + orderBookEnum.name() + "'");

            }
            case OrderViewDecoder.TEMPLATE_ID -> {
                OrderViewDecoder orderViewDecoder = new OrderViewDecoder();
                orderViewDecoder.wrapAndApplyHeader(buffer, offset, sbeMsgMessageHeaderDecoder);

                final long securityId = orderViewDecoder.securityId();
                final int traderId = orderViewDecoder.traderId();
                final String clientOrderId = orderViewDecoder.clientOrderId();
                final long orderId = orderViewDecoder.orderId();
                final long submittedTime = orderViewDecoder.submittedTime();

                final sbe.msg.PriceDecoder price = orderViewDecoder.price();
                final double priceValue = price.mantissa() * Math.pow(10, price.exponent());

                int orderQuantity = orderViewDecoder.orderQuantity();
                sbe.msg.SideEnum side = orderViewDecoder.side();

                LOGGER.info("Order view: " + OrderViewDecoder.TEMPLATE_ID + " securityId: " +
                    securityId + " clientOrderId: '" + clientOrderId + "' orderId: " + orderId +
                    " submittedTime: " + submittedTime + " side: " + side.name() +
                    " orderQuantity: " + orderQuantity + " price: " + priceValue + " traderId: " + traderId);

                // placeorder-tid@side@security@clientOrderId@trader@client
                final String correlationId = NewOrderDecoder.TEMPLATE_ID + "@" +
                        side.value() + "@" +
                        securityId + "@" +
                        clientOrderId.trim() + "@" +
                        "1" + "@" +
                        sbeMsgMessageHeaderDecoder.compID();

                pendingMessageManager.markOrderViewMessageAsReceived(
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

                LOGGER.debug("correlationId: " + correlationId);

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

                LOGGER.info("Add order: " + messageTypeEnum.name() + " " +
                    orderId + " " + side.name() + " " + quantity + "@" + priceValue +
                    " instrumentId: " + instrumentId +
                    " flags: " + flags.name());
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

                LOGGER.info("Order executed: " + OrderExecutedWithPriceSizeDecoder.TEMPLATE_ID +
                    " messageTypeEnum: " + messageTypeEnum.name() +
                    " orderId: " + orderId +
                    " executedQuantity/price: " + executedQuantity + "@" + priceValue +
//                    " displayQuantity/price: " + displayQuantity + "@" + priceValue +
                    " tradeId: " + tradeId +
                    " printable: " + printable.name() +
                    " instrumentId: " + instrumentId +
                    " clientOrderId: " + clientOrderId +
                    " executedTime: " + executedTime);

            }
            case UnitHeaderDecoder.TEMPLATE_ID -> {
                sbeMessageHeaderDecoder.wrap(buffer, offset);

                unitHeaderDecoder.wrapAndApplyHeader(buffer, offset, sbeMarketDataMessageHeaderDecoder);
                final int messageCount = unitHeaderDecoder.messageCount();
                byte marketDataGroup = unitHeaderDecoder.marketDataGroup();

                LOGGER.debug("Unit header: " + messageHeaderDecoder.templateId() + " " +
                    messageCount + " messages in group: " + marketDataGroup);
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

                LOGGER.debug("BBO: " + BestBidOfferDecoder.TEMPLATE_ID + " security "+ instrumentId +
                            " bidQuantity/bid: " + bidQuantity + "@" + bidValue +
                            " offerQuantity/offerValue: " + offerQuantity + "@" + offerValue);

                final String correlationId = AdminDecoder.TEMPLATE_ID + "@" +
                        AdminTypeEnum.BestBidOfferRequest.name() + "@" +
                        instrumentId + "@" +
                        "1" + "@" +
                        "1" + "@" +
                        "1";

                pendingMessageManager.markBBOMessageAsReceived(correlationId, bidQuantity, offerQuantity, bidValue, offerValue);
            }
            default -> LOGGER.info("unknown message type: " + messageHeaderDecoder.templateId());
        }
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
            LOGGER.info("Session event: " + code.name() + " " + detail + ". leadershipTermId=" + leadershipTermId);
        }
    }

    @Override
    public void onNewLeader(
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final String ingressEndpoints)
    {
        LOGGER.info("New Leader: " + leaderMemberId + ". leadershipTermId=" + leadershipTermId);
    }

}
