package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.MessageHeaderEncoder;
import sbe.msg.VWAPEncoder;

import java.nio.ByteBuffer;

public class VWAPBuilder {
    private int bufferIndex;
    private VWAPEncoder vwap;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private int compID;
    private int securityId;
    private long bidVWAP;
    private long offerVWAP;

    int messageLength = 0;
    public static int BUFFER_SIZE = 32;

    public VWAPBuilder(){
        vwap = new VWAPEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public void reset(){
        compID = 0;
        securityId = 0;
        bufferIndex = 0;
        bidVWAP = 0;
        offerVWAP = 0;

    }

    public VWAPBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public VWAPBuilder securityId(int value){
        this.securityId = value;
        return this;
    }

    public VWAPBuilder bidVWAP(int value){
        this.bidVWAP = value;
        return this;
    }

    public VWAPBuilder offerVWAP(int value){
        this.offerVWAP = value;
        return this;
    }

    public DirectBuffer build(){
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(vwap.sbeBlockLength())
                .templateId(vwap.sbeTemplateId())
                .schemaId(vwap.sbeSchemaId())
                .version(vwap.sbeSchemaVersion())
                .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        vwap.wrap(encodeBuffer, bufferIndex);

        vwap.securityId(securityId);
        vwap.bidVWAP().mantissa(bidVWAP);
        vwap.offerVWAP().mantissa(offerVWAP);

        messageLength = messageHeader.encodedLength() + vwap.encodedLength();
        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }
}
