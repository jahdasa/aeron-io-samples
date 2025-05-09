package io.aeron.samples.matchingengine.data;

import aeron.AeronPublisher;
import bplusTree.BPlusTree;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import io.aeron.samples.infra.SessionMessageContext;
import io.aeron.samples.matchingengine.crossing.strategy.AuctionData;
import leafNode.OrderEntry;
import leafNode.OrderList;
import leafNode.OrderListCursor;
import orderBook.OrderBook;
import org.agrona.DirectBuffer;
import org.joda.time.Instant;
import sbe.builder.AdminBuilder;
import sbe.builder.LOBBuilder;
import sbe.builder.MarketDepthBuilder;
import sbe.builder.VWAPBuilder;
import sbe.builder.marketData.*;
import sbe.msg.AdminTypeEnum;
import sbe.msg.marketData.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public enum MarketData {
    INSTANCE;

    private ObjectArrayList<DirectBuffer> mktData = new ObjectArrayList<>();
    private IntArrayList mktDataLength = new IntArrayList();
    private AddOrderBuilder addOrderBuilder = new AddOrderBuilder();
    private BestBidOfferBuilder bestBidAskBuilder = new BestBidOfferBuilder();
    private UnitHeaderBuilder unitHeaderBuilder = new UnitHeaderBuilder();
    private SymbolStatusBuilder symbolStatusBuilder = new SymbolStatusBuilder();
    private LOBBuilder lobBuilder = new LOBBuilder();
    private AdminBuilder adminBuilder = new AdminBuilder();
    private VWAPBuilder vwapBuilder = new VWAPBuilder();
    private MarketDepthBuilder marketDepthBuilder = new MarketDepthBuilder();
    private int sequenceNumber;
    private long securityId;
    private int compID;
    private boolean snapShotRequest;
    private OrderBook orderBook;

    private boolean marketDepthRequest;


    public void reset(){
        mktData.release();
        mktDataLength.release();
        lobBuilder.reset();
        vwapBuilder.reset();
        marketDepthBuilder.reset();
    }

/*    public void add(DirectBuffer buffer){
        mktData.add(buffer);
    }*/

    public void setSecurityId(long securityId){
        this.securityId = securityId;
    }

    public void buildAddOrder(OrderEntry orderEntry,boolean isMarketOrder){
        addOrderBuilder.messageType(MessageTypeEnum.AddOrder)
                .nanosecond(Instant.now().getMillis())
                .orderId(orderEntry.getOrderId());

        SideEnum sideEnum = orderEntry.getSide() == 1 ? SideEnum.Buy : SideEnum.Sell;
        addOrderBuilder.side(sideEnum)
                       .quantity(orderEntry.getDisplayQuantity())
                       .instrumentId(securityId)
                       .price(orderEntry.getPrice());

        Flags flags = isMarketOrder ? Flags.MarketOrder : Flags.B;
        addOrderBuilder.isMarketOrder(flags);
//                       .build();

        mktData.add(addOrderBuilder.build());
        mktDataLength.add(addOrderBuilder.getMessageLength());
    }


    public void addTrade(long orderId, long tradeId,long clientOrderId,long price,long quantity,long executedTime){
        OrderExecutedWithPriceSizeBuilder orderExecutedBuilder = new OrderExecutedWithPriceSizeBuilder();
        mktData.add(orderExecutedBuilder.messageType(MessageTypeEnum.OrderExecutedPriceSize)
                .orderId(orderId)
                .tradeId((int) tradeId)
                .clientOrderId(clientOrderId)
                .price((int) price)
                .executedQuantity((int) quantity)
                .printable(PrintableEnum.Printable)
                .instrumentId((int)securityId)
                .executedTime(executedTime)
                .build());

        mktDataLength.add(orderExecutedBuilder.getMessageLength());
    }

    public void addBestBidOffer(long bid, long bidQuantity, long offer, long offerQuantity){
        bestBidAskBuilder.messageType(MessageTypeEnum.BestBidAsk)
                         .instrumentId(securityId)
                         .bid(bid)
                         .bidQuantity(bidQuantity)
                         .offer(offer)
                         .offerQuantity(offerQuantity)
                         .build();

        mktData.add(bestBidAskBuilder.build());
        mktDataLength.add(bestBidAskBuilder.getMessageLength());
    }

    public void addSymbolStatus(int securityId, SessionChangedReasonEnum sessionChangedReason, TradingSessionEnum tradingSession, long staticPriceReference, long dynamicPriceReference) {
        symbolStatusBuilder.messageType(MessageTypeEnum.SymbolStatus)
                .sessionChangedReason(sessionChangedReason)
                .haltReason(HaltReasonEnum.ReasonNotAvailable)
                .instrumentId(securityId)
                .tradingSession(tradingSession)
                .staticPriceReference(staticPriceReference)
                .dynamicPriceReference(dynamicPriceReference);

        mktData.add(symbolStatusBuilder.build());
        mktDataLength.add(symbolStatusBuilder.getMessageLength());
    }

    public void addShutDownRequest() {
        adminBuilder.adminMessage(AdminTypeEnum.ShutDown)
                    .compID(999)
                    .securityId(0);

        mktData.add(adminBuilder.build());
        mktDataLength.add(adminBuilder.getMessageLength());
    }


    public DirectBuffer buildUnitHeader(){
        return unitHeaderBuilder.messageCount(mktData.elementsCount)
                         .marketDataGroup((byte)1)
                         .sequenceNumber(sequenceNumber++)
                         .build();


    }

    private DirectBuffer getAdminMessage(AdminTypeEnum adminTypeEnum, long securityId, int clientId){
        return adminBuilder.adminMessage(adminTypeEnum)
                .compID(clientId)
                .securityId((int)securityId)
                .build();
    }

    public void addEndMessage(int securityId){
        mktData.add(adminBuilder.adminMessage(AdminTypeEnum.EndMessage)
                .compID(compID)
                .securityId(securityId)
                .build());

        mktDataLength.add(adminBuilder.getMessageLength());
    }

    public void addStartMessage(int securityId){
        mktData.add(adminBuilder.adminMessage(AdminTypeEnum.StartMessage)
                .compID(compID)
                .securityId(securityId)
                .build());

        mktDataLength.add(adminBuilder.getMessageLength());
    }

    private void resetLOBBuilder(long securityId){
        lobBuilder.reset();
        lobBuilder.securityId((int) securityId);
        lobBuilder.compID(compID);
    }

    private void resetMarketDepthBuilder(long securityId){
        marketDepthBuilder.reset();
        marketDepthBuilder.securityId((int) securityId);
        marketDepthBuilder.compID(compID);
    }

    private void publishLOBSnapShot(SessionMessageContext context){
        mktData.add(lobBuilder.build());
        mktDataLength.add(lobBuilder.getMessageLength());

        for(ObjectCursor<DirectBuffer> cursor : mktData){
//            marketDataPublisher.send(cursor.value);
            context.reply(cursor.value, 0 , mktDataLength.get(cursor.index));
        }
        resetLOBBuilder(orderBook.getSecurityId());

//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private void publishMarketDepth(SessionMessageContext context){
        mktData.add(marketDepthBuilder.build());
        mktDataLength.add(marketDepthBuilder.getMessageLength());

        for(ObjectCursor<DirectBuffer> cursor : mktData){
            context.reply(cursor.value, 0 , mktDataLength.get(cursor.index));
        }
        resetMarketDepthBuilder(orderBook.getSecurityId());
    }

    public void lobSnapShot(SessionMessageContext context) {
        DirectBuffer adminMessage = getAdminMessage(AdminTypeEnum.StartLOB, orderBook.getSecurityId(), compID);
        context.reply(adminMessage, 0, adminBuilder.getMessageLength());

//        marketDataPublisher.send(getAdminMessage(AdminTypeEnum.StartLOB, orderBook.getSecurityId(), compID));
        //System.out.println("Publihed start snapshot");

        int count = 0;
        resetLOBBuilder(orderBook.getSecurityId());

        Iterator<Map.Entry<Long, OrderList>> bidIterator = (BPlusTree.BPlusTreeIterator) orderBook.getBidTree().iterator();
        while (bidIterator.hasNext()) {
            Map.Entry<Long, OrderList> orderList = bidIterator.next();
            if (orderList != null) {
                Iterator<OrderListCursor> iterator = orderList.getValue().iterator();
                while (iterator.hasNext()) {
                    OrderEntry currentOrder = iterator.next().value;

                    lobBuilder.addOrder(currentOrder.getClientOrderId(), (int) currentOrder.getOrderId(),
                            currentOrder.getQuantity(),
                            sbe.msg.SideEnum.get(currentOrder.getSide()),
                            currentOrder.getPrice());

                    count++;
                    if(count == 100){
                        publishLOBSnapShot(context);
                        count = 0;
                        System.out.println("Published large LOB");
                    }
                }
            }
        }

        Iterator<Map.Entry<Long, OrderList>> offerIterator = (BPlusTree.BPlusTreeIterator) orderBook.getOfferTree().iterator();
        while (offerIterator.hasNext()) {
            Map.Entry<Long, OrderList> orderList = offerIterator.next();
            if (orderList != null) {
                Iterator<OrderListCursor> iterator = orderList.getValue().iterator();
                while (iterator.hasNext()) {
                    OrderEntry currentOrder = iterator.next().value;

                    lobBuilder.addOrder(currentOrder.getClientOrderId(), (int) currentOrder.getOrderId(),
                            currentOrder.getQuantity(),
                            sbe.msg.SideEnum.get(currentOrder.getSide()),
                            currentOrder.getPrice());

                    count++;
                    if(count == 100){
                        publishLOBSnapShot(context);
                        count = 0;
                    }
                }
            }
        }

        if(count != 0) {
            publishLOBSnapShot(context);
        }

//        marketDataPublisher.send(getAdminMessage(AdminTypeEnum.EndLOB, orderBook.getSecurityId(), compID));
        DirectBuffer adminMessage1 = getAdminMessage(AdminTypeEnum.EndLOB, orderBook.getSecurityId(), compID);
        context.reply(adminMessage1, 0, adminBuilder.getMessageLength());

        setSnapShotRequest(false);
        setMarketDepthRequest(false);
        setOrderBook(null);
        reset();
        //System.out.println("Publish end orders");
    }



    public void calcVWAP(OrderBook orderBook){
        vwapBuilder.securityId((int) orderBook.getSecurityId());
        vwapBuilder.compID(compID);


        long totalVolume = 0;
        long totalPrice = 0;

        Iterator<Map.Entry<Long, OrderList>> bidIterator = (BPlusTree.BPlusTreeIterator) orderBook.getBidTree().iterator();
        while (bidIterator.hasNext()) {
            Map.Entry<Long, OrderList> orderList = bidIterator.next();
            if (orderList != null) {
                Iterator<OrderListCursor> iterator = orderList.getValue().iterator();
                while (iterator.hasNext()) {
                    OrderEntry currentOrder = iterator.next().value;

                    totalPrice += currentOrder.getPrice() * currentOrder.getQuantity();
                    totalVolume += currentOrder.getQuantity();
                }
            }
        }

        if(totalVolume > 0) {
            vwapBuilder.bidVWAP((int)(totalPrice / totalVolume));
        }

        totalVolume = 0;
        totalPrice = 0;

        Iterator<Map.Entry<Long, OrderList>> offerIterator = (BPlusTree.BPlusTreeIterator) orderBook.getOfferTree().iterator();
        while (offerIterator.hasNext()) {
            Map.Entry<Long, OrderList> orderList = offerIterator.next();
            if (orderList != null) {
                Iterator<OrderListCursor> iterator = orderList.getValue().iterator();
                while (iterator.hasNext()) {
                    OrderEntry currentOrder = iterator.next().value;

                    totalPrice += currentOrder.getPrice() * currentOrder.getQuantity();
                    totalVolume += currentOrder.getQuantity();
                }
            }
        }

        if(totalVolume > 0) {
            vwapBuilder.offerVWAP((int) (totalPrice / totalVolume));
        }

        mktData.add(vwapBuilder.build());
        mktDataLength.add(vwapBuilder.getMessageLength());
    }

    public void calcMarketDepth(SessionMessageContext context){
        marketDepthBuilder.securityId((int) orderBook.getSecurityId());
        marketDepthBuilder.compID(compID);

        DirectBuffer adminMessage = getAdminMessage(AdminTypeEnum.StartMarketDepth, orderBook.getSecurityId(), compID);
        context.reply(adminMessage, 0, adminBuilder.getMessageLength());

        int count = 0;
        resetMarketDepthBuilder(orderBook.getSecurityId());

        long[] prices = orderBook.getPriceList().toArray();
        Arrays.sort(prices);

        BPlusTree<Long, OrderList> bidTree = orderBook.getBidTree();

        for(int i=prices.length - 1; i>=0; i--){
            if(prices[i] == 0){
                continue;
            }

            OrderList bidList = bidTree.get(prices[i]);
            int bidCount = 0;
            long bidTotalVolume = 0L;

            if(bidList != null){

                bidCount = bidList.size();
                if(bidCount == 0L)
                {
                    continue;
                }
                bidTotalVolume = bidList.total();
                marketDepthBuilder.addDepth(sbe.msg.SideEnum.Buy, prices[i], bidCount, bidTotalVolume);
                count++;
            }

            if(count == 1000){
                publishMarketDepth(context);
                count = 0;
                System.out.println("Published large MarketDepth");
            }
        }

        if(count != 0) {
            publishMarketDepth(context);
        }

        BPlusTree<Long, OrderList> offerTree = orderBook.getOfferTree();

        for(int i=prices.length - 1; i>=0; i--){
            if(prices[i] == 0){
                continue;
            }

            OrderList offerList = offerTree.get(prices[i]);
            int offerCount = 0;
            long offerTotalVolume = 0L;

            if(offerList != null){
                offerCount = offerList.size();
                if(offerCount == 0L)
                {
                    continue;
                }

                offerTotalVolume = offerList.total();
                marketDepthBuilder.addDepth(sbe.msg.SideEnum.Sell, prices[i], offerCount, offerTotalVolume);
                count++;
            }

            if(count == 1000){
                publishMarketDepth(context);
                count = 0;
                System.out.println("Published large MarketDepth");
            }
        }


        if(count != 0) {
            publishMarketDepth(context);
        }

        DirectBuffer adminMessage1 = getAdminMessage(AdminTypeEnum.EndMarketDepth, orderBook.getSecurityId(), compID);
        context.reply(adminMessage1, 0, adminBuilder.getMessageLength());

        setSnapShotRequest(false);
        setMarketDepthRequest(false);
        setOrderBook(null);
        reset();
    }

    public ObjectArrayList<DirectBuffer> getMktDataMessages(){
        return mktData;
    }

    public int getCompID() {
        return compID;
    }

    public void setCompID(int compID) {
        this.compID = compID;
    }

    public boolean isSnapShotRequest() {
        return snapShotRequest;
    }

    public void setSnapShotRequest(boolean snapShotRequest) {
        this.snapShotRequest = snapShotRequest;
    }

    public long getSecurityId() {
        return securityId;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    public void setOrderBook(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public IntArrayList getMktDataLength() {
        return mktDataLength;
    }

    public boolean isMarketDepthRequest() {
        return marketDepthRequest;
    }

    public void setMarketDepthRequest(boolean marketDepthRequest) {
        this.marketDepthRequest = marketDepthRequest;
    }
}
