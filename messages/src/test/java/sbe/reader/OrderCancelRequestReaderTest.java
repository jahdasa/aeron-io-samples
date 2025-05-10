package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.BuilderUtil;
import sbe.builder.OrderCancelRequestBuilder;
import sbe.msg.OrderBookEnum;
import sbe.msg.OrderCancelRequestEncoder;
import sbe.msg.SideEnum;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 21/08/15.
 */
public class OrderCancelRequestReaderTest {

    @Test
    public void testRead() throws Exception {
        OrderCancelRequestReader orderCancelRequestReader = new OrderCancelRequestReader();
        DirectBuffer buffer = build();

        StringBuilder sb = orderCancelRequestReader.read(buffer);
        assertEquals("ClientOrderId=2                   " +
                     "OrigClientOrderId=1                   " +
                     "OrderId=0SecurityId=1TraderId=1" +
                     "Side=BuyOrderBook=Regular" +
                     "LimitPrice=1000",sb.toString());
    }

    private DirectBuffer build(){
        OrderCancelRequestBuilder orderCancelRequestBuilder = new OrderCancelRequestBuilder();
        orderCancelRequestBuilder.compID(1);

        String clientOrderId = BuilderUtil.fill("2",OrderCancelRequestEncoder.clientOrderIdLength());
        orderCancelRequestBuilder.clientOrderId(clientOrderId.getBytes());

        String origClientOrderId = BuilderUtil.fill("1",OrderCancelRequestEncoder.origClientOrderIdLength());
        orderCancelRequestBuilder.origClientOrderId(origClientOrderId.getBytes());
        orderCancelRequestBuilder.securityId(1);

        orderCancelRequestBuilder.tradeId(1)
                                 .side(SideEnum.Buy)
                                 .orderBook(OrderBookEnum.Regular)
                                 .limitPrice(1000);

        return orderCancelRequestBuilder.build();

    }

}