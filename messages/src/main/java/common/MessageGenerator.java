package common;

import org.agrona.DirectBuffer;
import sbe.builder.*;
import sbe.msg.*;

public class MessageGenerator {

    public static DirectBuffer buildLogonRequest(){
        LogonBuilder logonBuilder = new LogonBuilder();
        return logonBuilder.compID(1)
                .password("password12".getBytes())
                .newPassword("1234567890".getBytes())
                .build();
    }

    public static DirectBuffer buildLogonResponse(){
        LogonResponseBuilder logonResponseBuilder = new LogonResponseBuilder();
        return logonResponseBuilder.compID(1)
                .rejectCode(RejectCode.LoginSuccessful)
                .passwordExpiry(1)
                .build();
    }

    public static DirectBuffer buildOrderCancelRequest() {
        OrderCancelRequestBuilder orderCancelRequestBuilder = new OrderCancelRequestBuilder();
        orderCancelRequestBuilder.compID(1);

        String clientOrderId = BuilderUtil.fill("2", OrderCancelRequestEncoder.clientOrderIdLength());
        orderCancelRequestBuilder.clientOrderId(clientOrderId.getBytes());

        String origClientOrderId = BuilderUtil.fill("1", OrderCancelRequestEncoder.origClientOrderIdLength());
        orderCancelRequestBuilder.origClientOrderId(origClientOrderId.getBytes());
        orderCancelRequestBuilder.securityId(1);


        orderCancelRequestBuilder.traderId(1)
            .side(SideEnum.BUY)
            .orderBook(OrderBookEnum.Regular);

        return orderCancelRequestBuilder.build();
    }

    public static DirectBuffer buildOrderCancelRequestInvalidSecurity() {
        OrderCancelRequestBuilder orderCancelRequestBuilder = new OrderCancelRequestBuilder();
        orderCancelRequestBuilder.compID(1);

        String clientOrderId = BuilderUtil.fill("2", OrderCancelRequestEncoder.clientOrderIdLength());
        orderCancelRequestBuilder.clientOrderId(clientOrderId.getBytes());

        String origClientOrderId = BuilderUtil.fill("1", OrderCancelRequestEncoder.origClientOrderIdLength());
        orderCancelRequestBuilder.origClientOrderId(origClientOrderId.getBytes());
        orderCancelRequestBuilder.securityId(99);

        orderCancelRequestBuilder.traderId(1)
            .side(SideEnum.BUY)
            .orderBook(OrderBookEnum.Regular);

        return orderCancelRequestBuilder.build();
    }

    public static DirectBuffer buildOrderCancelReplaceRequest() {
        OrderCancelReplaceRequestBuilder orderCancelReplaceRequestBuilder = new OrderCancelReplaceRequestBuilder();
        orderCancelReplaceRequestBuilder.compID(1);

        String clientOrderId = BuilderUtil.fill("2", OrderCancelReplaceRequestEncoder.clientOrderIdLength());
        orderCancelReplaceRequestBuilder.clientOrderId(clientOrderId.getBytes());

        String origClientOrderId = BuilderUtil.fill("1",OrderCancelReplaceRequestEncoder.origClientOrderIdLength());
        orderCancelReplaceRequestBuilder.origClientOrderId(origClientOrderId.getBytes());
        orderCancelReplaceRequestBuilder.securityId(1);

        orderCancelReplaceRequestBuilder.traderId(1);

        String account = BuilderUtil.fill("test", OrderCancelReplaceRequestEncoder.accountLength());
        orderCancelReplaceRequestBuilder.account(account.getBytes());

        orderCancelReplaceRequestBuilder.orderType(OrdTypeEnum.Limit)
            .timeInForce(TimeInForceEnum.Day)
            .expireTime("20150823-10:00:00".getBytes())
            .side(SideEnum.BUY)
            .orderQuantity(1000)
            .displayQuantity(1000)
            .minQuantity(0)
            .limitPrice(10000)
            .stopPrice(0)
            .orderBook(OrderBookEnum.Regular);

        return orderCancelReplaceRequestBuilder.build();
    }
}
