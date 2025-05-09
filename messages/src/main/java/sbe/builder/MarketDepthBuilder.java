package sbe.builder;

import com.carrotsearch.hppc.ObjectArrayList;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.*;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class MarketDepthBuilder {
    private int bufferIndex;
    private MarketDepthEncoder marketDepthEncoder;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;

    private int compID;
    private int securityId;
    private ObjectArrayList<Depth> depths;

    int messageLength = 0;
    public static int BUFFER_SIZE = 17000;

    public MarketDepthBuilder(){
        marketDepthEncoder = new MarketDepthEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        depths = new ObjectArrayList<>();
    }

    public void reset(){
        depths.clear();
        depths.trimToSize();
        compID = 0;
        securityId = 0;
        bufferIndex = 0;
    }

    public MarketDepthBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public MarketDepthBuilder securityId(int value){
        this.securityId = value;
        return this;
    }

    public MarketDepthBuilder addDepth(SideEnum side, long price, int count,long quantity){
        depths.add(new Depth(side, price,count,quantity));
        return this;
    }


    public DirectBuffer build(){
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(marketDepthEncoder.sbeBlockLength())
                .templateId(marketDepthEncoder.sbeTemplateId())
                .schemaId(marketDepthEncoder.sbeSchemaId())
                .version(marketDepthEncoder.sbeSchemaVersion())
                .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        int size = depths.size();
        MarketDepthEncoder.DepthEncoder depthsEncoder = marketDepthEncoder.wrap(encodeBuffer, bufferIndex)
                .securityId(securityId)
                .depthCount(size);

        for(int i=0; i<size; i++){
            Depth depth = depths.get(i);
            if(depth != null) {
                MarketDepthEncoder.DepthEncoder de = depthsEncoder.next();

                de.side(depth.getSide());
                de.price().mantissa(depth.getPrice());
                de.orderCount(depth.getCount());
                de.quantity(depth.getQuantity());
            }
        }

        messageLength = messageHeader.encodedLength() + marketDepthEncoder.encodedLength();
        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public static class Depth implements Serializable{
        private SideEnum side;
        private long price;
        private int count;
        private long quantity;

        public Depth(){}

        public Depth(SideEnum side, long price,int count,long quantity){
            this.side = side;
            this.price = price;
            this.count = count;
            this.quantity = quantity;
        }

        public SideEnum getSide() {
            return side;
        }

        public void setSide(SideEnum side) {
            this.side = side;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "Depth{" +
                    "side=" + side +
                    ", price=" + price +
                    ", count=" + count +
                    ", quantity=" + quantity +
                    '}';
        }
    }

}
