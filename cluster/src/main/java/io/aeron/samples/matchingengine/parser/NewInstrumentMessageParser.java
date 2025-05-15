package io.aeron.samples.matchingengine.parser;

import org.agrona.DirectBuffer;
import sbe.msg.NewInstrumentDecoder;

public class NewInstrumentMessageParser
{
    private final NewInstrumentDecoder decoder = new NewInstrumentDecoder();
    private int securityId;
    private String code;
    private String name;

    public void decode(
        DirectBuffer buffer,
        int bufferOffset,
        int actingBlockLength,
        int actingVersion)
    {
        decoder.wrap(buffer, bufferOffset, actingBlockLength, actingVersion);
        code =  decoder.code();
        name = decoder.name();
        securityId = decoder.securityId();
    }


    public int getSecurityId()
    {
        return securityId;
    }


    public String getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }
}
