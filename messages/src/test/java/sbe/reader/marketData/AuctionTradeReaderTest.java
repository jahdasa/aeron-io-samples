package sbe.reader.marketData;

import org.junit.jupiter.api.Test;
import sbe.builder.marketData.TradeBuilder;
import sbe.msg.marketData.MessageTypeEnum;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by dharmeshsing on 1/11/15.
 */
public class AuctionTradeReaderTest {
    @Test
    public void testRead() throws Exception {
        TradeReader tradeReader = new TradeReader();
        DirectBuffer buffer = build();

        StringBuilder sb = tradeReader.read(buffer);
        assertEquals("MessageType=AuctionTradeNanosecond=913353552ExecutedQuantity=1000InstrumentId=1TradeId=1001Price=1000",sb.toString());
    }


    private DirectBuffer build(){
        TradeBuilder tradeBuilder = new TradeBuilder();
        return tradeBuilder.messageType(MessageTypeEnum.AuctionTrade)
                .nanosecond(913353552)
                .executedQuantity(1000)
                .instrumentId(1)
                .tradeId(1001)
                .price(1000)
                .build();

    }

}