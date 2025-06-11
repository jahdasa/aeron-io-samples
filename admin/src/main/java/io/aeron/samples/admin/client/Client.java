package io.aeron.samples.admin.client;

import io.aeron.samples.admin.model.Pair;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import sbe.builder.*;
import sbe.msg.*;

public class Client
{


    private AdminBuilder adminBuilder = new AdminBuilder();
    private NewInstrumentBuilder newInstrumentBuilder = new NewInstrumentBuilder();
    private ListInstrumentsBuilder  listInstrumentsBuilder = new ListInstrumentsBuilder();

    public Client()
    {
    }

    public DirectBuffer placeOrder(
        final int securityId,
        final AtomicBuffer buffer,
        final int claimIndex,
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
        final NewOrderBuilder newOrderBuilder = new NewOrderBuilder()
            .account("account123".getBytes())
            .capacity(CapacityEnum.Agency)
            .cancelOnDisconnect(CancelOnDisconnectEnum.DoNotCancel)
            .orderBook(OrderBookEnum.Regular)
            .expireTime("20211230-23:00:00".getBytes());

        final DirectBuffer directBuffer = newOrderBuilder
            .compID(clientId)
            .clientOrderId(clientOrderId)
            .securityId(securityId)
            .orderType(OrdTypeEnum.valueOf(orderType))
            .timeInForce(TimeInForceEnum.valueOf(timeInForce))
            .side(SideEnum.valueOf(side.toUpperCase()))
            .orderQuantity((int) volume)
            .displayQuantity((int) displayQuantity)
            .minQuantity((int) minQuantity)
            .limitPrice(price)
            .stopPrice(stopPrice)
            .traderId(traderId)
            .build(buffer, claimIndex);

        System.out.println("Message=OrderAdd|OrderId=" + clientOrderId.trim() + "|Type=" + orderType + "|Side=" + side + "|Volume=" + volume + "(" + displayQuantity + ")" + "|Price=" + price + "|StopPrice=" + stopPrice + "|TIF=" + timeInForce + "|MES=" + minQuantity);

        return directBuffer;
    }

    public DirectBuffer newInstrument(
            final int clientId,
            final MutableDirectBuffer buffer,
            final int claimIndex,
            final int securityId,
            final String code,
            final String name)
    {
        final DirectBuffer directBuffer = newInstrumentBuilder.compID(clientId)
                .securityId(securityId)
                .code(code)
                .name(name)
                .build(buffer, claimIndex);

        System.out.println("Message=NewInstrument|SecurityId=" + securityId + "|Code=" + code + "|Name=" + name);

        return directBuffer;
    }

    public void listInstruments(
            final int clientId,
            final MutableDirectBuffer buffer,
            final int claimIndex,
            final String correlationId)
    {
        final DirectBuffer directBuffer = listInstrumentsBuilder.compID(clientId)
                .correlationId(correlationId)
                .build(buffer, claimIndex);

        System.out.println("Message=ListInstruments|CorrelationId=" + correlationId);
    }

    public Pair<DirectBuffer, Integer> cancelOrder(
        final int securityId,
        final String originalClientOrderId,
        final String side,
        final long price,
        final int traderId,
        final int clientId)
    {
        final OrderCancelRequestBuilder orderCancelRequestBuilder = new OrderCancelRequestBuilder()
                .orderBook(OrderBookEnum.Regular);

        final String origClientOrderId = BuilderUtil.fill(
            originalClientOrderId,
            OrderCancelRequestEncoder.origClientOrderIdLength());

        final String clientOrderId = BuilderUtil.fill(
            "-" + originalClientOrderId,
            OrderCancelRequestEncoder.clientOrderIdLength());

        final DirectBuffer directBuffer = orderCancelRequestBuilder
                .compID(clientId)
                .clientOrderId(clientOrderId.getBytes())
                .origClientOrderId(origClientOrderId.getBytes())
                .securityId(securityId)
                .side(SideEnum.valueOf(side.toUpperCase()))
                .limitPrice(price)
                .securityId(securityId)
                .traderId(traderId)
                .build();

        System.out.println("Message=OrderCancel|origClientOrderId=" + origClientOrderId.trim() + "|clientOrderId=" + clientOrderId.trim() + "|Side=" + side.toUpperCase() + "|Price=" + price);

        return new Pair<>(directBuffer, orderCancelRequestBuilder.getMessageEncodedLength());
    }

    public Pair<DirectBuffer, Integer> replaceOrder(
        int securityId,
        String originalClientOrderId,
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
        final String clientOrderId = BuilderUtil.fill(
            originalClientOrderId,
            OrderCancelReplaceRequestEncoder.clientOrderIdLength());

        final String origClientOrderId = BuilderUtil.fill(
            originalClientOrderId,
            OrderCancelReplaceRequestEncoder.origClientOrderIdLength());

        final OrderCancelReplaceRequestBuilder orderCancelReplaceRequestBuilder = new OrderCancelReplaceRequestBuilder()
            .account("account123".getBytes())
            .orderBook(OrderBookEnum.Regular);

        final DirectBuffer directBuffer = orderCancelReplaceRequestBuilder
            .compID(clientId)
            .clientOrderId(clientOrderId.getBytes())
            .origClientOrderId(origClientOrderId.getBytes())
            .securityId(securityId)
            .traderId(traderId)
            .orderType(OrdTypeEnum.valueOf(orderType))
            .timeInForce(TimeInForceEnum.valueOf(timeInForce))
            .expireTime("20211230-23:00:00".getBytes())
            .side(SideEnum.valueOf(side.toUpperCase()))
            .orderQuantity((int) volume)
            .displayQuantity((int) displayQuantity)
            .minQuantity((int) minQuantity)
            .limitPrice(price)
            .stopPrice(stopPrice)
            .build();

        System.out.println("Message=OrderModify|Time=" + clientOrderId + "|OrderId=" + origClientOrderId +
            "|Type=" + orderType + "|Side=" + side + "|Volume=" + volume + "(" + displayQuantity + ")" +
            "|Price=" + price + "|StopPrice=" + stopPrice + "|TIF=" + timeInForce + "|MES=" + minQuantity);

        return new Pair<>(directBuffer, orderCancelReplaceRequestBuilder.getMessageEncodedLength());
    }

    public DirectBuffer calcVWAP(final int securityId, final int clientId)
    {
        return adminBuilder
            .compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.VWAP)
            .build();

    }

    public DirectBuffer lobSnapshot(final int securityId, final int clientId)
    {
        return adminBuilder
            .compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.LOB)
            .build();
    }

    public DirectBuffer marketDepth(final int securityId, final int clientId)
    {
        return adminBuilder
            .compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.MarketDepth)
            .build();
    }

    public DirectBuffer bbo(final int securityId, final int clientId)
    {
        return adminBuilder
            .compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.BestBidOfferRequest)
            .build();
    }

    public int getLobSnapshotMessageLength()
    {
        return adminBuilder.getMessageLength();
    }

}
