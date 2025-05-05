package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.LogonResponseBuilder;
import sbe.msg.RejectCode;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by dharmeshsing on 12/08/15.
 */
public class LogonResponseReaderTest {

    @Test
    public void testRead() throws Exception {
        LogonResponseReader logonResponseReader = new LogonResponseReader();
        DirectBuffer buffer = build();

        StringBuilder sb = logonResponseReader.read(buffer);
        assertEquals("RejectCode=0PasswordExpiry=1",sb.toString());
    }

    private DirectBuffer build(){
        LogonResponseBuilder logonResponseBuilder = new LogonResponseBuilder();
        return logonResponseBuilder.compID(1)
                            .rejectCode(RejectCode.LoginSuccessful)
                            .passwordExpiry(1)
                            .build();

    }
}