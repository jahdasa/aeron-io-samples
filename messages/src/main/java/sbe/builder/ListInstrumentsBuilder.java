package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.ListInstrumentsMessageRequestEncoder;
import sbe.msg.MessageHeaderEncoder;
import sbe.msg.NewInstrumentEncoder;

import java.nio.ByteBuffer;

public class ListInstrumentsBuilder
{
    private int bufferIndex;
    private ListInstrumentsMessageRequestEncoder listInstruments;
    private MessageHeaderEncoder messageHeader;

    private int messageEncodedLength;

    private int compID;
    private UnsafeBuffer correlationId;

    public static int BUFFER_SIZE = 114;

    public ListInstrumentsBuilder()
    {
        listInstruments = new ListInstrumentsMessageRequestEncoder();
        messageHeader = new MessageHeaderEncoder();

        correlationId = new UnsafeBuffer(ByteBuffer.allocateDirect(ListInstrumentsMessageRequestEncoder.correlationIdLength()));
    }

    public int messageEncodedLength(){
        return messageEncodedLength;
    }

    public ListInstrumentsBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public ListInstrumentsBuilder correlationId(String value)
    {
        value = BuilderUtil.fill(value, ListInstrumentsMessageRequestEncoder.correlationIdLength());
        this.correlationId.wrap(value.getBytes());

        return this;
    }


    public DirectBuffer build(final MutableDirectBuffer buffer, final int offset){
        bufferIndex = offset;
        messageHeader.wrap(buffer, bufferIndex)
                .blockLength(listInstruments.sbeBlockLength())
                .templateId(listInstruments.sbeTemplateId())
                .schemaId(listInstruments.sbeSchemaId())
                .version(listInstruments.sbeSchemaVersion())
                .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        listInstruments.wrap(buffer, bufferIndex)
            .putCorrelationId(correlationId.byteArray(),0);

        messageEncodedLength = messageHeader.encodedLength() + listInstruments.encodedLength();

        return buffer;
    }

}
