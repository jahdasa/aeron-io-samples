package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.*;

import java.nio.ByteBuffer;

public class NewOrderBuilder
{
    private int bufferIndex;

    private NewOrderEncoder newOrder;
    private MessageHeaderEncoder messageHeader;

    private int messageEncodedLength;

    private int compID;
    private int securityId;
    private int orderQuantity;
    private int displayQuantity;
    private int minQuantity;
    private String clientOrderId;
    private UnsafeBuffer account;
    private OrdTypeEnum orderType;
    private TimeInForceEnum timeInForce;
    private UnsafeBuffer expireTime;
    private SideEnum side;
    private long limitPrice;
    private long stopPrice;
    private CapacityEnum capacity;
    private CancelOnDisconnectEnum cancelOnDisconnect;
    private OrderBookEnum orderBook;
    private int traderId;

    public static int BUFFER_SIZE = 114;

    public NewOrderBuilder()
    {
        newOrder = new NewOrderEncoder();
        messageHeader = new MessageHeaderEncoder();

        account = new UnsafeBuffer(ByteBuffer.allocateDirect(NewOrderEncoder.accountLength()));
        expireTime = new UnsafeBuffer(ByteBuffer.allocateDirect(NewOrderEncoder.expireTimeLength()));
    }

    public int messageEncodedLength()
    {
        return messageEncodedLength;
    }

    public NewOrderBuilder compID(int value)
    {
        this.compID = value;
        return this;
    }

    public NewOrderBuilder clientOrderId(String value)
    {
        clientOrderId = BuilderUtil.fill(value, NewOrderEncoder.clientOrderIdLength());
        return this;
    }

    public NewOrderBuilder securityId(int value)
    {
        this.securityId = value;
        return this;
    }

    public NewOrderBuilder account(byte[] value)
    {
        this.account.wrap(value);
        return this;
    }

    public NewOrderBuilder orderType(OrdTypeEnum value){
        this.orderType = value;
        return this;
    }

    public NewOrderBuilder timeInForce(TimeInForceEnum value){
        this.timeInForce = value;
        return this;
    }
    public NewOrderBuilder expireTime(byte[] value){
        this.expireTime.wrap(value);
        return this;
    }

    public NewOrderBuilder side(SideEnum value)
    {
        this.side = value;
        return this;
    }

    public NewOrderBuilder orderQuantity(int value){
        this.orderQuantity = value;
        return this;
    }

    public NewOrderBuilder displayQuantity(int value){
        this.displayQuantity = value;
        return this;
    }

    public NewOrderBuilder minQuantity(int value){
        this.minQuantity = value;
        return this;
    }

    public NewOrderBuilder traderId(int value){
        this.traderId = value;
        return this;
    }

    public NewOrderBuilder limitPrice(long value){
        this.limitPrice = value;
        return this;
    }

    public NewOrderBuilder stopPrice(long value){
        this.stopPrice = value;
        return this;
    }

    public NewOrderBuilder capacity(CapacityEnum value){
        this.capacity = value;
        return this;
    }

    public NewOrderBuilder cancelOnDisconnect(CancelOnDisconnectEnum value){
        this.cancelOnDisconnect = value;
        return this;
    }

    public NewOrderBuilder orderBook(OrderBookEnum value){
        this.orderBook = value;
        return this;
    }

    public DirectBuffer build(final MutableDirectBuffer buffer, final int offset)
    {
        bufferIndex = offset;
        messageHeader.wrap(buffer, bufferIndex)
            .blockLength(newOrder.sbeBlockLength())
            .templateId(newOrder.sbeTemplateId())
            .schemaId(newOrder.sbeSchemaId())
            .version(newOrder.sbeSchemaVersion())
            .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        newOrder.wrap(buffer, bufferIndex)
            .putClientOrderId(clientOrderId.getBytes(),0)
            .securityId(securityId)
            .traderId(traderId)
            .putAccount(account.byteArray(),0)
            .orderType(orderType)
            .timeInForce(timeInForce)
            .putExpireTime(expireTime.byteArray(),0)
            .side(side)
            .orderQuantity(orderQuantity)
            .displayQuantity(displayQuantity)
            .minQuantity(minQuantity);

        newOrder.limitPrice().mantissa(limitPrice);
        newOrder.stopPrice().mantissa(stopPrice);

        newOrder.capacity(capacity)
                .cancelOnDisconnect(cancelOnDisconnect)
                .orderBook(orderBook);

        messageEncodedLength = messageHeader.encodedLength() + newOrder.encodedLength();

        return buffer;
    }

}
