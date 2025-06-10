package io.aeron.samples.admin.client;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import sbe.builder.*;
import sbe.msg.*;

public class Client
{
    private NewOrderBuilder newOrderBuilder = new NewOrderBuilder().account("account123".getBytes())
            .capacity(CapacityEnum.Agency)
            .cancelOnDisconnect(CancelOnDisconnectEnum.DoNotCancel)
            .orderBook(OrderBookEnum.Regular)
            .expireTime("20211230-23:00:00".getBytes());

    private OrderCancelRequestBuilder orderCancelRequestBuilder = new OrderCancelRequestBuilder()
            .orderBook(OrderBookEnum.Regular);

    private OrderCancelReplaceRequestBuilder orderCancelReplaceRequestBuilder = new OrderCancelReplaceRequestBuilder()
            .account("account123".getBytes())
            .orderBook(OrderBookEnum.Regular);

    private AdminBuilder adminBuilder = new AdminBuilder();
    private NewInstrumentBuilder newInstrumentBuilder = new NewInstrumentBuilder();
    private ListInstrumentsBuilder  listInstrumentsBuilder = new ListInstrumentsBuilder();

    private ClientData clientData;

    public Client(final ClientData clientData)
    {
        this.clientData = clientData;
    }

    private static final IntObjectMap<ClientData> clientDataMap = new IntObjectHashMap<>();

    public static Client newInstance(int clientId)
    {
        if(clientDataMap.isEmpty())
        {
            final ClientData clientData = new ClientData(clientId);
            clientDataMap.put(clientId, clientData);
        }
        return new Client(clientDataMap.get(clientId));
    }

    public DirectBuffer placeOrder(
        final int securityId,
        final MutableDirectBuffer buffer,
        final int claimIndex,
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
        final int clientId)
    {
        clientOrderId = BuilderUtil.fill(clientOrderId, NewOrderEncoder.clientOrderIdLength());

        final DirectBuffer directBuffer = newOrderBuilder.compID(clientId)
                .clientOrderId(clientOrderId.getBytes())
                .securityId(securityId)
                .orderType(OrdTypeEnum.valueOf(orderType))
                .timeInForce(TimeInForceEnum.valueOf(timeInForce))
                .side(SideEnum.valueOf(side))
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

    public int getNewOrderEncodedLength()
    {
        return newOrderBuilder.messageEncodedLength();
    }

    public int getCancelOrderEncodedLength()
    {
        return orderCancelRequestBuilder.getMessageEncodedLength();
    }

    public int getReplaceOrderEncodedLength()
    {
        return orderCancelReplaceRequestBuilder.getMessageEncodedLength();
    }

    public DirectBuffer cancelOrder(
        final int securityId,
        final String originalClientOrderId,
        final String side,
        final long price,
        final int traderId,
        final int clientId)
    {
        final String origClientOrderId = BuilderUtil.fill(
            originalClientOrderId,
            OrderCancelRequestEncoder.origClientOrderIdLength());

        final String clientOrderId = BuilderUtil.fill(
            "-" + originalClientOrderId,
            OrderCancelRequestEncoder.clientOrderIdLength());

        DirectBuffer directBuffer = orderCancelRequestBuilder.compID(clientId)
                .clientOrderId(clientOrderId.getBytes())
                .origClientOrderId(origClientOrderId.getBytes())
                .securityId(securityId)
                .side(SideEnum.valueOf(side))
                .limitPrice(price)
                .securityId(securityId)
                .traderId(traderId)
                .compID(clientData.getCompID())
                .build();
        System.out.println("Message=OrderCancel|OrderId=" + origClientOrderId.trim());

        return directBuffer;
    }

    public DirectBuffer replaceOrder(
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

        DirectBuffer directBuffer = orderCancelReplaceRequestBuilder.compID(clientId)
            .clientOrderId(clientOrderId.getBytes())
            .origClientOrderId(origClientOrderId.getBytes())
            .securityId(securityId)
            .traderId(traderId)
            .orderType(OrdTypeEnum.valueOf(orderType))
            .timeInForce(TimeInForceEnum.valueOf(timeInForce))
            .expireTime("20211230-23:00:00".getBytes())
            .side(SideEnum.valueOf(side))
            .orderQuantity((int) volume)
            .displayQuantity((int) displayQuantity)
            .minQuantity((int) minQuantity)
            .limitPrice(price)
            .stopPrice(stopPrice)
            .build();
        System.out.println("Message=OrderModify|Time=" + clientOrderId + "|OrderId=" + origClientOrderId + "|Type=" + orderType + "|Side=" + side + "|Volume=" + volume + "(" + displayQuantity + ")" + "|Price=" + price + "|StopPrice=" + stopPrice + "|TIF=" + timeInForce + "|MES=" + minQuantity);

        return directBuffer;
    }

    public DirectBuffer calcVWAP(final int securityId, final int clientId)
    {
        return adminBuilder.compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.VWAP)
            .build();

    }

    public DirectBuffer lobSnapshot(final int securityId, final int clientId)
    {
        return adminBuilder.compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.LOB)
            .build();
    }

    public DirectBuffer marketDepth(final int securityId, final int clientId)
    {
        return adminBuilder.compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.MarketDepth)
            .build();
    }

    public DirectBuffer bbo(final int securityId, final int clientId)
    {
        return adminBuilder.compID(clientId)
            .securityId(securityId)
            .adminMessage(AdminTypeEnum.BestBidOfferRequest)
            .build();
    }

    public int getLobSnapshotMessageLength()
    {
        return adminBuilder.getMessageLength();
    }

}
