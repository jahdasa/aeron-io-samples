package sbe.builder.marketData;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.marketData.BestBidOfferEncoder;
import sbe.msg.marketData.MessageHeaderEncoder;
import sbe.msg.marketData.MessageTypeEnum;

import java.nio.ByteBuffer;

public class BestBidOfferBuilder {
    private int bufferIndex;
    private BestBidOfferEncoder bestBidOffer;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private MessageTypeEnum messageType;
    private long instrumentId;
    private long bid;
    private long bidQuantity;
    private long offer;
    private long offerQuantity;

    public static int BUFFER_SIZE = 39;

    int messageLength = 0;

    public BestBidOfferBuilder(){
        bestBidOffer = new BestBidOfferEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public BestBidOfferBuilder messageType(MessageTypeEnum value){
        this.messageType = value;
        return this;
    }

    public BestBidOfferBuilder instrumentId(long value){
        this.instrumentId = value;
        return this;
    }

    public BestBidOfferBuilder bid(long value){
        this.bid = value;
        return this;
    }

    public BestBidOfferBuilder offer(long value){
        this.offer = value;
        return this;
    }

    public BestBidOfferBuilder bidQuantity(long value){
        this.bidQuantity = value;
        return this;
    }

    public BestBidOfferBuilder offerQuantity(long value){
        this.offerQuantity = value;
        return this;
    }

    public DirectBuffer build(){
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(bestBidOffer.sbeBlockLength())
                .templateId(bestBidOffer.sbeTemplateId())
                .schemaId(bestBidOffer.sbeSchemaId())
                .version(bestBidOffer.sbeSchemaVersion());

        bufferIndex += messageHeader.encodedLength();
        bestBidOffer.wrap(encodeBuffer, bufferIndex);

        bestBidOffer.messageType(messageType)
                .instrumentId(instrumentId);

        bestBidOffer.bid().mantissa(bid);
        bestBidOffer.offer().mantissa(offer);

        bestBidOffer.bidQuantity(bidQuantity);
        bestBidOffer.offerQuantity(offerQuantity);

        messageLength = messageHeader.encodedLength() + bestBidOffer.encodedLength();

        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }
}
