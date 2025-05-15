package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.*;

import java.nio.ByteBuffer;

public class NewInstrumentBuilder
{
    private int bufferIndex;
    private NewInstrumentEncoder newInstrument;
    private MessageHeaderEncoder messageHeader;

    private int messageEncodedLength;

    private int compID;
    private int securityId;
    private UnsafeBuffer code;
    private UnsafeBuffer name;

    public static int BUFFER_SIZE = 114;

    public NewInstrumentBuilder()
    {
        newInstrument = new NewInstrumentEncoder();
        messageHeader = new MessageHeaderEncoder();

        code = new UnsafeBuffer(ByteBuffer.allocateDirect(NewInstrumentEncoder.codeLength()));
        name = new UnsafeBuffer(ByteBuffer.allocateDirect(NewInstrumentEncoder.nameLength()));
    }

    public int messageEncodedLength(){
        return messageEncodedLength;
    }

    public NewInstrumentBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public NewInstrumentBuilder code(String value)
    {
        value = BuilderUtil.fill(value, NewInstrumentEncoder.codeLength());
        this.code.wrap(value.getBytes());

        return this;
    }


    public NewInstrumentBuilder name(String value)
    {
        value = BuilderUtil.fill(value, NewInstrumentEncoder.nameLength());
        this.name.wrap(value.getBytes());

        return this;
    }

    public NewInstrumentBuilder securityId(int value){
        this.securityId = value;
        return this;
    }

    public DirectBuffer build(final MutableDirectBuffer buffer, final int offset){
        bufferIndex = offset;
        messageHeader.wrap(buffer, bufferIndex)
                .blockLength(newInstrument.sbeBlockLength())
                .templateId(newInstrument.sbeTemplateId())
                .schemaId(newInstrument.sbeSchemaId())
                .version(newInstrument.sbeSchemaVersion())
                .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        newInstrument.wrap(buffer, bufferIndex)
                .securityId(securityId)
                .putCode(code.byteArray(),0)
                .putName(name.byteArray(),0);

        messageEncodedLength = messageHeader.encodedLength() + newInstrument.encodedLength();

        return buffer;
    }

}
