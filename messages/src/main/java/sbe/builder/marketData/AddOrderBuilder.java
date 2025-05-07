package sbe.builder.marketData;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.marketData.*;

import java.nio.ByteBuffer;

public class AddOrderBuilder {
    private int bufferIndex;
    private AddOrderEncoder addOrder;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private MessageTypeEnum messageType;
    private long nanosecond;
    private long orderId;
    private SideEnum side;
    private int quantity;
    private long instrumentId;
    private long price;
    private Flags isMarketOrder;

    public static int BUFFER_SIZE = 39;

    int messageLength = 0;

    public AddOrderBuilder(){
        addOrder = new AddOrderEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public AddOrderBuilder messageType(MessageTypeEnum value){
        this.messageType = value;
        return this;
    }

    public AddOrderBuilder nanosecond(long value){
        this.nanosecond = value;
        return this;
    }

    public AddOrderBuilder orderId(long value){
        this.orderId = value;
        return this;
    }

    public AddOrderBuilder side(SideEnum value){
        this.side = value;
        return this;
    }

    public AddOrderBuilder quantity(int value){
        this.quantity = value;
        return this;
    }

    public AddOrderBuilder instrumentId(long value){
        this.instrumentId = value;
        return this;
    }
    public AddOrderBuilder price(long value){
        this.price = value;
        return this;
    }

    public AddOrderBuilder isMarketOrder(Flags value){
        this.isMarketOrder = value;
        return this;
    }


    public DirectBuffer build(){
        messageLength = 0;
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(addOrder.sbeBlockLength())
                .templateId(addOrder.sbeTemplateId())
                .schemaId(addOrder.sbeSchemaId())
                .schemaId(addOrder.sbeSchemaId())
                .version(addOrder.sbeSchemaVersion());

        bufferIndex += messageHeader.encodedLength();
        addOrder.wrap(encodeBuffer, bufferIndex);

        addOrder.messageType(messageType)
                .nanosecond(nanosecond)
                .orderId(orderId)
                .side(side)
                .quantity(quantity)
                .instrumentId(instrumentId);

        addOrder.price().mantissa(price);
        addOrder.flags(isMarketOrder);

        messageLength = addOrder.encodedLength() + messageHeader.encodedLength();
        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }

}
