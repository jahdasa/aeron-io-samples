package sbe.builder;

import org.agrona.DirectBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by dharmeshsing on 12/08/15.
 */
public class LogonBuilderTest{

    @Test
    public void testLogonBuilder(){
        LogonBuilder logonBuilder = new LogonBuilder();
        DirectBuffer buffer = logonBuilder.compID(1)
                .password("password12".getBytes())
                .newPassword("1234567890".getBytes())
                .build();

        Assertions.assertNotNull(buffer);
    }
}