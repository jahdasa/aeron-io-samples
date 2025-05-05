package io.aeron.samples.matchingengine.crossing;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.ObjectArrayList;
import dao.OrderBookDAO;
import dao.TraderDAO;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import orderBook.Stock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import unsafe.UnsafeUtil;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CrossingProcessorTest {

    private static final int STOCK_ID = 1;
    private CrossingProcessor crossingProcessor;
    private CrossingProcessor expectedCrossingProcessor;

    @BeforeEach
    public void setup() throws IOException {
        String dataPath = Paths.get("").toAbsolutePath().getParent() + "/data";
        LongObjectHashMap<OrderBook> orderBooks = OrderBookDAO.loadOrderBooks(dataPath);
        TraderDAO.loadTraders(dataPath);
        crossingProcessor = new CrossingProcessor(orderBooks);
        expectedCrossingProcessor = new CrossingProcessor(orderBooks);
    }

    @AfterEach
    public void tearDown(){
        crossingProcessor.getOrderBook(STOCK_ID).freeAll();
        expectedCrossingProcessor.getOrderBook(STOCK_ID).freeAll();
    }

    @Disabled
//    @Parameters(source = OrderProvider.class)
    public void testCrossingProcessor(OrderData orderData) {
        ObjectArrayList<OrderEntry> initStateList = null;
        ObjectArrayList<OrderEntry> aggOrderList = null;
        ObjectArrayList<OrderEntry> expStateList = null;


        try {
            Stock stock = orderData.getStock();
            crossingProcessor.getOrderBook(STOCK_ID).setStock(stock);
            expectedCrossingProcessor.getOrderBook(STOCK_ID).setStock(stock);

            if (orderData.getType().equals("Market Order") && orderData.getTestNumber() == 4) {
                System.out.println("test");
            }

            initStateList = orderData.getInitState();
            if (initStateList != null) {
                Object[] initArr = initStateList.buffer;
                for (int i = 0; i < initStateList.size(); i++) {
                    OrderEntry oe = (OrderEntry) initArr[i];
                    crossingProcessor.processOrder(1,STOCK_ID, oe);
                }
            }

            aggOrderList = orderData.getAggOrder();
            Object[] aggArr = aggOrderList.buffer;
            for (int i = 0; i < aggOrderList.size(); i++) {
                OrderEntry oe = (OrderEntry) aggArr[i];
                crossingProcessor.processOrder(1,STOCK_ID,oe);
            }


            expStateList = orderData.getExpState();
            Object[] expArr = expStateList.buffer;
            for (int i = 0; i < expStateList.size(); i++) {
                OrderEntry oe = (OrderEntry) expArr[i];
                expectedCrossingProcessor.processOrder(1,STOCK_ID,oe);
            }

            OrderBook orderBook = crossingProcessor.getOrderBook(STOCK_ID);
            OrderBook expectedOrderBook = expectedCrossingProcessor.getOrderBook(STOCK_ID);

            assertEquals(expectedOrderBook, orderBook, "Test Type = " + orderData.getType() + " , Test Order Book " + orderData.getTestNumber() + " failed");
            assertEquals(orderData.getTrades(), orderBook.getTrades(), "Test Type = " + orderData.getType() + " , Test Trades " + orderData.getTestNumber() + " failed");
        }finally{
            UnsafeUtil.freeOrderEntryMemory(initStateList);
            UnsafeUtil.freeOrderEntryMemory(aggOrderList);
            UnsafeUtil.freeOrderEntryMemory(expStateList);

        }
    }
}
