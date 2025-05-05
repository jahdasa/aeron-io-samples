package io.aeron.samples.matchingengine.crossing.preProcessor;

import com.carrotsearch.hppc.ObjectArrayList;
import io.aeron.samples.matchingengine.crossing.MatchingContext;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import io.aeron.samples.matchingengine.crossing.OrderData;
import io.aeron.samples.matchingengine.crossing.OrderLoader;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import orderBook.Stock;
import org.junit.jupiter.api.*;
import sbe.msg.OrderCancelRequestEncoder;
import unsafe.UnsafeUtil;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 25/07/15.
 */
public class CancelOrderPreProcessorTest {

    private static final int STOCK_ID = 1;
    private OrderBook orderBook;
    private OrderBook expectedOrderBook;
    private CancelOrderPreProcessor cancelOrderPreProcessor;
    private PriceTimePriorityStrategy priceTimePriorityStrategy;

    @BeforeEach
    public void setup(){
        orderBook = new OrderBook(STOCK_ID);
        expectedOrderBook = new OrderBook(STOCK_ID);
        priceTimePriorityStrategy = new PriceTimePriorityStrategy();
        cancelOrderPreProcessor = new CancelOrderPreProcessor();
    }

    @AfterEach
    public void tearDown(){
        orderBook.freeAll();
        expectedOrderBook.freeAll();
    }

    @Disabled
//    @Test
//    @Parameters(method = "provideCancelOrderData")
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
                if(MatchingUtil.isParkedOrder(oe)){
                    priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.PARK_ORDER, orderBook, oe);
                }else {
                    priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER, orderBook, oe);
                }
            }

            aggOrderList = orderData.getAggOrder();
            Object[] aggArr = aggOrderList.buffer;
            for (int i = 0; i < aggOrderList.size(); i++) {
                OrderEntry oe = (OrderEntry) aggArr[i];

                MatchingContext mc = MatchingContext.INSTANCE;
                mc.setTemplateId(OrderCancelRequestEncoder.TEMPLATE_ID);
                mc.setOrderBook(orderBook);
                mc.setOrderEntry(oe);

                cancelOrderPreProcessor.preProcess(mc);
            }

            expStateList = orderData.getExpState();
            Object[] expArr = expStateList.buffer;
            for(int i=0; i<expStateList.size(); i++){
                OrderEntry oe = (OrderEntry)expArr[i];
                priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER,expectedOrderBook,oe);
            }

            assertEquals(expectedOrderBook, orderBook, "Test Order Book " + orderData.getTestNumber() + " failed");
            assertEquals(orderData.getTrades(), orderBook.getTrades(), "Test Trades" + orderData.getTestNumber() + " failed");

        }finally{
            UnsafeUtil.freeOrderEntryMemory(initStateList);
            UnsafeUtil.freeOrderEntryMemory(aggOrderList);
            UnsafeUtil.freeOrderEntryMemory(expStateList);
        }
    }

    public static Object[] provideCancelOrderData() {
        OrderLoader orderLoader = new OrderLoader();
        try {
            ObjectArrayList<OrderData> orderDataList = orderLoader.getCancelOrders();
            //TODO:Remove this. Only used for testing
            orderDataList.trimToSize();
            Object[] arr = orderDataList.buffer;
            org.apache.commons.lang3.ArrayUtils.reverse(arr);
            return arr;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}