package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.BuilderUtil;
import sbe.builder.OrderCancelReplaceRequestBuilder;
import sbe.msg.*;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 23/08/15.
 */
public class OrderCancelReplaceRequestReaderTest {
    @Test
    public void testRead() throws Exception {
        OrderCancelReplaceRequestReader orderCancelReplaceRequestReader = new OrderCancelReplaceRequestReader();
        DirectBuffer buffer = build();

        StringBuilder sb = orderCancelReplaceRequestReader.read(buffer);
        assertEquals("ClientOrderId=2                   OrigClientOrderId=1                   " +
                     "OrderId=0SecurityId=1TraderId=1Account=test      " +
                     "ExpireTime=20150823-10:00:00Side=BuyOrderQuantity=1000DisplayQuantity=1000MinQuantity=0" +
                     "LimitPrice=10000StopPrice=0OrderBook=Regular",
                      sb.toString());
    }

    private DirectBuffer build(){
        OrderCancelReplaceRequestBuilder orderCancelReplaceRequestBuilder = new OrderCancelReplaceRequestBuilder();
        orderCancelReplaceRequestBuilder.compID(1);
        orderCancelReplaceRequestBuilder.traderId(1);

        String clientOrderId = BuilderUtil.fill("2", OrderCancelReplaceRequestEncoder.clientOrderIdLength());
        orderCancelReplaceRequestBuilder.clientOrderId(clientOrderId.getBytes());

        String origClientOrderId = BuilderUtil.fill("1",OrderCancelReplaceRequestEncoder.origClientOrderIdLength());
        orderCancelReplaceRequestBuilder.origClientOrderId(origClientOrderId.getBytes());
        orderCancelReplaceRequestBuilder.securityId(1);

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