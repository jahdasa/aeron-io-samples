package sbe.builder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sbe.msg.*;
import org.agrona.DirectBuffer;

/**
 * Created by dharmeshsing on 18/02/17.
 */
public class NewOrderBuilderTest {
    private NewOrderBuilder newOrderBuilder = new NewOrderBuilder();

    @Test
    public void testNewOrder(){
        LogonBuilder logonBuilder = new LogonBuilder();
        DirectBuffer buffer = createNewOrder(1200, 2500, SideEnum.Buy, OrdTypeEnum.Limit);
        Assertions.assertNotNull(buffer);
    }

    public DirectBuffer createNewOrder(long volume, long price,SideEnum side,OrdTypeEnum orderType){
        String clientOrderId = "1234";

        DirectBuffer directBuffer = newOrderBuilder.compID(1)
                .clientOrderId(clientOrderId)
                .account("account123".getBytes())
                .capacity(CapacityEnum.Agency)
                .cancelOnDisconnect(CancelOnDisconnectEnum.DoNotCancel)
                .orderBook(OrderBookEnum.Regular)
                .securityId(1)
                .traderId(1)
                .orderType(orderType)
                .timeInForce(TimeInForceEnum.Day)
                .expireTime("20150813-23:00:00".getBytes())
                .side(side)
                .orderQuantity((int) volume)
                .displayQuantity((int) volume)
                .minQuantity(0)
                .limitPrice(price)
                .stopPrice(0)
                .build();

        return directBuffer;
    }

}