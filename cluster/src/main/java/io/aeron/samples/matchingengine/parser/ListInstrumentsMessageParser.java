package io.aeron.samples.matchingengine.parser;

import org.agrona.DirectBuffer;
import sbe.msg.ListInstrumentsMessageRequestDecoder;

public class ListInstrumentsMessageParser
{
    private final ListInstrumentsMessageRequestDecoder decoder = new ListInstrumentsMessageRequestDecoder();

    private String correlationId;

    public void decode(
        DirectBuffer buffer,
        int bufferOffset,
        int actingBlockLength,
        int actingVersion)
    {
        decoder.wrap(buffer, bufferOffset, actingBlockLength, actingVersion);
        correlationId = decoder.correlationId();
    }

    public String getCorrelationId()
    {
        return correlationId;
    }
}
