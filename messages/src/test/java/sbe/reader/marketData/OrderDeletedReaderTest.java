package sbe.reader.marketData;

import org.junit.jupiter.api.Test;
import sbe.builder.marketData.OrderDeletedBuilder;
import sbe.msg.marketData.MessageTypeEnum;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 1/11/15.
 */
public class OrderDeletedReaderTest {
    @Test
    public void testRead() throws Exception {
        OrderDeletedReader orderDeletedReader = new OrderDeletedReader();
        DirectBuffer buffer = build();

        StringBuilder sb = orderDeletedReader.read(buffer);
        assertEquals("MessageType=OrderDeletedNanosecond=913353552OrderId=1",sb.toString());

    }


    private DirectBuffer build(){
        OrderDeletedBuilder orderDeletedBuilder = new OrderDeletedBuilder();
        return orderDeletedBuilder.messageType(MessageTypeEnum.OrderDeleted)
                .nanosecond(913353552)
                .orderId(1)
                .build();

    }

}