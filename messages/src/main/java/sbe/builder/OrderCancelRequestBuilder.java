package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.MessageHeaderEncoder;
import sbe.msg.OrderBookEnum;
import sbe.msg.OrderCancelRequestEncoder;
import sbe.msg.SideEnum;

import java.nio.ByteBuffer;

public class OrderCancelRequestBuilder {
    private int bufferIndex;
    private OrderCancelRequestEncoder orderCancelRequest;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;
    private int messageEncodedLength;

    private int compID;
    private int securityId;
    private UnsafeBuffer clientOrderId;
    private UnsafeBuffer origClientOrderId;
    private int orderId;
    private int traderId;
    private SideEnum side;
    private long limitPrice;
    private OrderBookEnum orderBook;

    public static int BUFFER_SIZE = 256;

    public OrderCancelRequestBuilder(){
        orderCancelRequest = new OrderCancelRequestEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));

        clientOrderId = new UnsafeBuffer(ByteBuffer.allocateDirect(OrderCancelRequestEncoder.clientOrderIdLength()));
        origClientOrderId = new UnsafeBuffer(ByteBuffer.allocateDirect(OrderCancelRequestEncoder.origClientOrderIdLength()));
    }

    public OrderCancelRequestBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public OrderCancelRequestBuilder clientOrderId(byte[] value){
        this.clientOrderId.wrap(value);
        return this;
    }

    public OrderCancelRequestBuilder origClientOrderId(byte[] value){
        this.origClientOrderId.wrap(value);
        return this;
    }

    public OrderCancelRequestBuilder securityId(int value){
        this.securityId = value;
        return this;
    }

    public OrderCancelRequestBuilder orderId(int value){
        this.orderId = value;
        return this;
    }

    public OrderCancelRequestBuilder traderId(int value){
        this.traderId = value;
        return this;
    }

    public OrderCancelRequestBuilder side(SideEnum value){
        this.side = value;
        return this;
    }

    public OrderCancelRequestBuilder limitPrice(long value){
        this.limitPrice = value;
        return this;
    }

    public OrderCancelRequestBuilder orderBook(OrderBookEnum value){
        this.orderBook = value;
        return this;
    }

    public DirectBuffer build(){
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(orderCancelRequest.sbeBlockLength())
                .templateId(orderCancelRequest.sbeTemplateId())
                .schemaId(orderCancelRequest.sbeSchemaId())
                .version(orderCancelRequest.sbeSchemaVersion())
                .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        orderCancelRequest.wrap(encodeBuffer, bufferIndex)
                .putClientOrderId(clientOrderId.byteArray(),0)
                .putOrigClientOrderId(origClientOrderId.byteArray(),0)
                .orderId(orderId)
                .securityId(securityId)
                .traderId(traderId)
                .side(side)
                .orderBook(orderBook);
        orderCancelRequest.limitPrice().mantissa(limitPrice);

        messageEncodedLength = messageHeader.encodedLength() + orderCancelRequest.encodedLength();

        return encodeBuffer;
    }

    public int getMessageEncodedLength() {
        return messageEncodedLength;
    }
}
