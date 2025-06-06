package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.LogonBuilder;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by dharmeshsing on 12/08/15.
 */
public class LogonReaderTest {

    @Test
    public void testRead() throws Exception {
        LogonReader logonReader = new LogonReader();
        DirectBuffer buffer = build();

        StringBuilder sb = logonReader.read(buffer);
        assertEquals("CompID=1Password=password12NewPassword=1234567890",sb.toString());

    }

    private DirectBuffer build(){
        LogonBuilder logonBuilder = new LogonBuilder();
        return logonBuilder.compID(1)
                .password("password12".getBytes())
                .newPassword("1234567890".getBytes())
                .build();
    }
}