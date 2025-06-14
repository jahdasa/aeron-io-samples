package io.aeron.samples.matchingengine.crossing.tradingSessions;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.ObjectArrayList;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import io.aeron.samples.matchingengine.crossing.OrderData;
import io.aeron.samples.matchingengine.crossing.OrderProvider;
import dao.OrderBookDAO;
import dao.TraderDAO;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import orderBook.Stock;
import org.junit.jupiter.api.*;
import sbe.msg.marketData.TradingSessionEnum;
import unsafe.UnsafeUtil;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 4/12/15.
 */
public class ContinuousTradingProcessorTest {
    private static final int STOCK_ID = 1;
    private TradingSessionProcessor continuousTradingProcessor;
    private LongObjectHashMap<OrderBook> orderBooks;
    private LongObjectHashMap<OrderBook> expectedOrderBooks;

    @BeforeEach
    public void setup() throws IOException
    {
        String dataPath = Paths.get("").toAbsolutePath().getParent() + "/data";
        orderBooks = OrderBookDAO.loadOrderBooks(dataPath);
        expectedOrderBooks = OrderBookDAO.loadOrderBooks(dataPath);
        TraderDAO.loadTraders(dataPath);
        TradingSessionFactory.initTradingSessionProcessors(orderBooks);
        continuousTradingProcessor = TradingSessionFactory.getTradingSessionProcessor(TradingSessionEnum.ContinuousTrading);
    }

    @AfterEach
    public void tearDown(){
        orderBooks.get(STOCK_ID).freeAll();
        TradingSessionFactory.reset();
    }

    @Disabled
//    @Test
//    @Parameters(source = OrderProvider.class)
    public void testCrossingProcessor(OrderData orderData) {
        ObjectArrayList<OrderEntry> initStateList = null;
        ObjectArrayList<OrderEntry> aggOrderList = null;
        ObjectArrayList<OrderEntry> expStateList = null;
        MatchingUtil.setEnableCircuitBreaker(false);

        try {
            Stock stock = orderData.getStock();
            OrderBook orderBook = orderBooks.get(STOCK_ID);
            orderBook.setStock(stock);

            OrderBook expectedOrderBook = expectedOrderBooks.get(STOCK_ID);
            expectedOrderBook.setStock(stock);

            //expectedCrossingProcessor.getOrderBook(STOCK_ID).setStock(stock);

            if (orderData.getType().equals("Hidden Order") && orderData.getTestNumber() == 7) {
                System.out.println("test");
            }

            initStateList = orderData.getInitState();
            if (initStateList != null) {
                Object[] initArr = initStateList.buffer;
                for (int i = 0; i < initStateList.size(); i++) {
                    OrderEntry oe = (OrderEntry) initArr[i];
                    continuousTradingProcessor.process(orderBook,oe);
                }
            }

            aggOrderList = orderData.getAggOrder();
            Object[] aggArr = aggOrderList.buffer;
            for (int i = 0; i < aggOrderList.size(); i++) {
                OrderEntry oe = (OrderEntry) aggArr[i];
                continuousTradingProcessor.process(orderBook,oe);
            }


            expStateList = orderData.getExpState();
            Object[] expArr = expStateList.buffer;
            for (int i = 0; i < expStateList.size(); i++) {
                OrderEntry oe = (OrderEntry) expArr[i];
                continuousTradingProcessor.process(expectedOrderBook,oe);
            }


            assertEquals( expectedOrderBook, orderBook, "Test Type = " + orderData.getType() + " , Test Order Book " + orderData.getTestNumber() + " failed");
            assertEquals(orderData.getTrades(), orderBook.getTrades(), "Test Type = " + orderData.getType() + " , Test Trades " + orderData.getTestNumber() + " failed");
        }finally{
            UnsafeUtil.freeOrderEntryMemory(initStateList);
            UnsafeUtil.freeOrderEntryMemory(aggOrderList);
            UnsafeUtil.freeOrderEntryMemory(expStateList);

        }
    }
}
