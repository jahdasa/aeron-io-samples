package io.aeron.samples.matchingengine.crossing.strategy;

import com.carrotsearch.hppc.ObjectArrayList;
import common.OrderType;
import io.aeron.samples.matchingengine.crossing.OrderData;
import io.aeron.samples.matchingengine.crossing.OrderLoader;
import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import orderBook.Stock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import unsafe.UnsafeUtil;

import java.io.IOException;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 25/07/15.
 */
public class AuctionStrategyTest {

    private static final int STOCK_ID = 1;
    private OrderBook orderBook;
    private OrderBook expectedOrderBook;
    private PriceTimePriorityStrategy priceTimePriorityStrategy;
    private AuctionStrategy auctionStrategy;

    @BeforeEach
    public void setup(){
        orderBook = new OrderBook(STOCK_ID);
        expectedOrderBook = new OrderBook(STOCK_ID);
        priceTimePriorityStrategy = new PriceTimePriorityStrategy();
        auctionStrategy = new AuctionStrategy();
    }

    @AfterEach
    public void tearDown(){
        orderBook.freeAll();
        expectedOrderBook.freeAll();
    }

    @Disabled
//    @Test
//    @Parameters(method = "provideAuctionData")
    public void testProcess(OrderData orderData) throws Exception {
        ObjectArrayList<OrderEntry> initStateList = null;
        ObjectArrayList<OrderEntry> aggOrderList = null;
        ObjectArrayList<OrderEntry> expStateList = null;

        try {

            Stock stock = orderData.getStock();
            orderBook.setStock(stock);
            expectedOrderBook.setStock(stock);

            initStateList = orderData.getInitState();
            Object[] initArr = initStateList.buffer;
            for(int i=0; i<initStateList.size(); i++){
                OrderEntry oe = (OrderEntry)initArr[i];

                if(oe.getType() == OrderType.MARKET.getOrderType()) {
                    long marketPrice;
                    if(oe.getSide() == 1){
                        marketPrice = orderBook.getBestBid();
                    }else{
                        marketPrice = orderBook.getBestOffer();
                    }
                    oe.setPrice(marketPrice);
                }

                priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER,orderBook,oe);
            }

            auctionStrategy.process(priceTimePriorityStrategy,orderBook);

            expStateList = orderData.getExpState();
            Object[] expArr = expStateList.buffer;
            for(int i=0; i<expStateList.size(); i++){
                OrderEntry oe = (OrderEntry)expArr[i];
                priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER,expectedOrderBook,oe);
            }

            assertEquals(expectedOrderBook, orderBook, "Test Order Book " + orderData.getTestNumber() + " failed");
            assertEquals( orderData.getTrades(), orderBook.getTrades(), "Test Trades" + orderData.getTestNumber() + " failed");
            System.out.println("Static Price Reference: " + orderBook.getStaticPriceReference() + "     " + "Dynamic Price Reference: " + orderBook.getDynamicPriceReference());
        }finally{
            UnsafeUtil.freeOrderEntryMemory(initStateList);
            UnsafeUtil.freeOrderEntryMemory(aggOrderList);
            UnsafeUtil.freeOrderEntryMemory(expStateList);
        }
    }

    public static Object[] provideAuctionData() {
        OrderLoader orderLoader = new OrderLoader();
        try {
            ObjectArrayList<OrderData> orderDataList = orderLoader.getAuctionOrders();
            //TODO:Remove this. Only used for testing
            orderDataList.trimToSize();
            Object[] arr = orderDataList.buffer;
            //org.apache.commons.lang3.ArrayUtils.reverse(arr);
            return arr;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}