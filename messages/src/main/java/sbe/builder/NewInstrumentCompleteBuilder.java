package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.MessageHeaderEncoder;
import sbe.msg.NewInstrumentCompleteEncoder;
import sbe.msg.NewInstrumentCompleteStatus;
import sbe.msg.NewInstrumentEncoder;

import java.nio.ByteBuffer;

public class NewInstrumentCompleteBuilder
{
    private int bufferIndex;

    private NewInstrumentCompleteEncoder encoder;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private int compID;
    private int securityId;
    private UnsafeBuffer code;

    private NewInstrumentCompleteStatus status;

    int messageLength = 0;

    public static int BUFFER_SIZE = 106;

    public NewInstrumentCompleteBuilder()
    {
        encoder = new NewInstrumentCompleteEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));

        code = new UnsafeBuffer(ByteBuffer.allocateDirect(NewInstrumentCompleteEncoder.codeLength()));
    }

    public NewInstrumentCompleteBuilder compID(int value)
    {
        this.compID = value;
        return this;
    }

    public NewInstrumentCompleteBuilder securityId(int value)
    {
        this.securityId = value;
        return this;
    }

    public NewInstrumentCompleteBuilder code(String value)
    {
        value = BuilderUtil.fill(value, NewInstrumentEncoder.codeLength());
        this.code.wrap(value.getBytes());

        return this;
    }

    public NewInstrumentCompleteBuilder status(NewInstrumentCompleteStatus value)
    {
        this.status = value;
        return this;
    }

    public void reset()
    {
        compID = 0;
        securityId = 0;
        status = NewInstrumentCompleteStatus.NULL_VAL;
    }

    public DirectBuffer build()
    {
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
            .blockLength(encoder.sbeBlockLength())
            .templateId(encoder.sbeTemplateId())
            .schemaId(encoder.sbeSchemaId())
            .version(encoder.sbeSchemaVersion())
            .compID(compID);

        bufferIndex += messageHeader.encodedLength();

        encoder.wrap(encodeBuffer, bufferIndex)
            .securityId(securityId)
            .putCode(code.byteArray(),0)
            .status(status);

        messageLength = messageHeader.encodedLength() + encoder.encodedLength();

        return encodeBuffer;
    }

    public int getMessageLength()
    {
        return messageLength;
    }

}
