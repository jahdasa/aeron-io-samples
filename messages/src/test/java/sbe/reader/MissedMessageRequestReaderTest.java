package sbe.reader;

import org.junit.jupiter.api.Test;
import sbe.builder.MissedMessageRequestBuilder;
import org.agrona.DirectBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 29/10/15.
 */
public class MissedMessageRequestReaderTest {
    @Test
    public void testRead() throws Exception {
        MissedMessageRequestReader missedMessageRequestReader = new MissedMessageRequestReader();
        DirectBuffer buffer = build();

        StringBuilder sb = missedMessageRequestReader.read(buffer);
        assertEquals("PartitionId=1SequenceNumber=1",sb.toString());

    }

    private DirectBuffer build(){
        MissedMessageRequestBuilder missedMessageRequestBuilder = new MissedMessageRequestBuilder();

        return missedMessageRequestBuilder.compID(1)
                .partitionId((short)1)
                .sequenceNumber(1)
                .build();
    }

}