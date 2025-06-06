package sbe.builder.marketData;

import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.marketData.MessageHeaderEncoder;
import sbe.msg.marketData.UnitHeaderEncoder;
import org.agrona.DirectBuffer;
import java.nio.ByteBuffer;

public class UnitHeaderBuilder {
    private int bufferIndex;
    private UnitHeaderEncoder unitHeader;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private int messageCount;
    private byte marketDataGroup;
    private int sequenceNumber;

    public static int BUFFER_SIZE = 15;

    int messageLength = 0;

    public UnitHeaderBuilder(){
        unitHeader = new UnitHeaderEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public UnitHeaderBuilder messageCount(int value){
        this.messageCount = value;
        return this;
    }

    public UnitHeaderBuilder marketDataGroup(byte value){
        this.marketDataGroup = value;
        return this;
    }

    public UnitHeaderBuilder sequenceNumber(int value){
        this.sequenceNumber = value;
        return this;
    }


    public DirectBuffer build(){
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(unitHeader.sbeBlockLength())
                .templateId(unitHeader.sbeTemplateId())
                .schemaId(unitHeader.sbeSchemaId())
                .version(unitHeader.sbeSchemaVersion());

        bufferIndex += messageHeader.encodedLength();
        unitHeader.wrap(encodeBuffer, bufferIndex);

        unitHeader.messageCount(messageCount)
                  .marketDataGroup(marketDataGroup)
                  .sequenceNumber(sequenceNumber);

        messageLength = unitHeader.encodedLength();
        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }

}
