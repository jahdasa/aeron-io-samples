package sbe.builder;

import com.carrotsearch.hppc.ObjectArrayList;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.ListInstrumentsMessageResponseEncoder;
import sbe.msg.MessageHeaderEncoder;
import sbe.msg.NewInstrumentEncoder;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

public class ListInstrumentsResponseBuilder
{
    private int bufferIndex;
    private ListInstrumentsMessageResponseEncoder list;

    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private int compID;
    private UnsafeBuffer correlationId;
    private UnsafeBuffer code;
    private UnsafeBuffer name;

    private ObjectArrayList<Instrument> instruments;

    int messageLength = 0;
    public static int BUFFER_SIZE = 17000;

    public ListInstrumentsResponseBuilder(){
        list = new ListInstrumentsMessageResponseEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        instruments = new ObjectArrayList<>();

        correlationId = new UnsafeBuffer(ByteBuffer.allocateDirect(ListInstrumentsMessageResponseEncoder.correlationIdLength()));
        code = new UnsafeBuffer(ByteBuffer.allocateDirect(ListInstrumentsMessageResponseEncoder.InstrumentsEncoder.codeLength()));
        name = new UnsafeBuffer(ByteBuffer.allocateDirect(ListInstrumentsMessageResponseEncoder.InstrumentsEncoder.nameLength()));
    }

    public void reset(){
        instruments.clear();
        instruments.trimToSize();
        compID = 0;
        bufferIndex = 0;
        messageLength = 0;
    }

    public ListInstrumentsResponseBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public ListInstrumentsResponseBuilder correlation(String value)
    {
        value = BuilderUtil.fill(value, ListInstrumentsMessageResponseEncoder.correlationIdLength());
        this.correlationId.wrap(value.getBytes());

        return this;
    }

    public ListInstrumentsResponseBuilder addInstrument(int securityId, String code, String name)
    {
        instruments.add(new Instrument(securityId, code, name));
        return this;
    }

    public ListInstrumentsResponseBuilder addInstruments(final List<Instrument> value)
    {
        value.forEach(instruments::add);

        return this;
    }

    public DirectBuffer build()
    {
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
            .blockLength(list.sbeBlockLength())
            .templateId(list.sbeTemplateId())
            .schemaId(list.sbeSchemaId())
            .version(list.sbeSchemaVersion())
            .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        int size = instruments.size();

        final ListInstrumentsMessageResponseEncoder.InstrumentsEncoder instrumentsEncoder = list.wrap(encodeBuffer, bufferIndex)
            .instrumentsCount(size);

        list.putCorrelationId(correlationId.byteArray(), 0);

        for (int i = 0; i < size; i++)
        {
            final Instrument instrument = instruments.get(i);

            if (instrument != null)
            {
                final ListInstrumentsMessageResponseEncoder.InstrumentsEncoder ie = instrumentsEncoder.next();

                ie.securityId(instrument.getSecurityId());

                final String codeFill = BuilderUtil.fill(instrument.getCode(), ListInstrumentsMessageResponseEncoder.InstrumentsEncoder.codeLength());
                code.wrap(codeFill.getBytes());
                ie.putCode(code.byteArray(), 0);

                final String nameFill = BuilderUtil.fill(instrument.getName(), ListInstrumentsMessageResponseEncoder.InstrumentsEncoder.nameLength());

                name.wrap(nameFill.getBytes());
                ie.putName(name.byteArray(), 0);
            }
        }

        messageLength = messageHeader.encodedLength() + list.encodedLength();

        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public static class Instrument implements Serializable
    {
        private int securityId;
        private String code;
        private String name;

        public Instrument(){}

        public Instrument(int securityId, String code, String name){
            this.securityId = securityId;
            this.code = code;
            this.name = name;
        }

        public int getSecurityId() {
            return securityId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

}
