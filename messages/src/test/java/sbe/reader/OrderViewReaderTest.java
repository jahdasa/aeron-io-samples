package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.BuilderUtil;
import sbe.builder.OrderViewBuilder;
import sbe.msg.OrderViewEncoder;
import sbe.msg.SideEnum;
import org.agrona.DirectBuffer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 27/04/16.
 */
public class OrderViewReaderTest {
    @Test
    public void testRead() throws Exception {
        OrderViewReader orderViewReader = new OrderViewReader();
        DirectBuffer buffer = build();

        orderViewReader.read(buffer);
        assertEquals(2,orderViewReader.getSecurityId());
        assertEquals(3,orderViewReader.getOrderId());
        assertEquals(1000,orderViewReader.getOrderQuantity());
        assertEquals(100,orderViewReader.getPrice());
        assertEquals(SideEnum.Buy,orderViewReader.getSide());
        assertEquals("1                   ",orderViewReader.getClientOrderId());
    }

    private DirectBuffer build(){
        OrderViewBuilder orderViewBuilder = new OrderViewBuilder();
        return orderViewBuilder.compID(1)
                .securityId(2)
                .orderId(3)
                .traderId(1)
                .submittedTime(Instant.now().toEpochMilli())
                .orderQuantity(1000)
                .price(100)
                .side(SideEnum.Buy)
                .clientOrderId(BuilderUtil.fill("1", OrderViewEncoder.clientOrderIdLength()).getBytes())
                .build();
    }

}