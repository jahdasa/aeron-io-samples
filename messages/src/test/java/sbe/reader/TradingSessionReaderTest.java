package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.TradingSessionBuilder;
import sbe.msg.marketData.TradingSessionEnum;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 26/08/15.
 */
public class TradingSessionReaderTest {

    @Test
    public void testRead() throws Exception {
        TradingSessionReader tradingSessionReader = new TradingSessionReader();
        DirectBuffer buffer = build();

        StringBuilder sb = tradingSessionReader.read(buffer);
        assertEquals("TradingSession=StartOfTradingsecurityId=1",sb.toString());
    }

    private DirectBuffer build(){
        TradingSessionBuilder tradingSessionBuilder = new TradingSessionBuilder();
        tradingSessionBuilder.tradingSessionEnum(TradingSessionEnum.StartOfTrading);
        tradingSessionBuilder.securityId(1);

        return tradingSessionBuilder.build();

    }

}