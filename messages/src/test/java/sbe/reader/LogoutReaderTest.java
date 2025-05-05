package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.BuilderUtil;
import sbe.builder.LogoutBuilder;
import sbe.msg.LogoutEncoder;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by dharmeshsing on 28/10/15.
 */
public class LogoutReaderTest {
    @Test
    public void testRead() throws Exception {
        LogoutReader logoutReader = new LogoutReader();
        DirectBuffer buffer = build();

        StringBuilder sb = logoutReader.read(buffer);
        assertEquals("Reason=test5678901234567890",sb.toString());

    }

    private DirectBuffer build(){
        LogoutBuilder logoutBuilder = new LogoutBuilder();
        String reason = BuilderUtil.fill("test5678901234567890", LogoutEncoder.reasonLength());

        return logoutBuilder.compID(1)
                .reason(reason.getBytes())
                .build();
    }

}