package io.aeron.samples.matchingengine.crossing.preProcessor;

import common.OrderType;
import io.aeron.samples.matchingengine.crossing.TestOrderEntryFactory;
import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor.MATCHING_ACTION;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unsafe.UnsafeUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by dharmeshsing on 17/07/15.
 */
public class StopOrderPreProcessorTest {

    private StopOrderPreProcessor stopOrderPreProcessor;

    @BeforeEach
    public void setup(){
        stopOrderPreProcessor = new StopOrderPreProcessor();
    }

    @Test
    public void testAggressOrder() throws Exception {
        OrderEntry orderEntry = TestOrderEntryFactory.createOrderEntry("10:00");
        orderEntry.setType(OrderType.STOP.getOrderType());
        orderEntry.setStopPrice(200L);

        MATCHING_ACTION result = stopOrderPreProcessor.preProcess(300L, OrderBook.SIDE.BID, orderEntry);
        assertEquals(MATCHING_ACTION.AGGRESS_ORDER,result);

        UnsafeUtil.freeOrderEntryMemory(orderEntry);
    }

    @Test
    public void testParkOrder() throws Exception {
        OrderEntry orderEntry = TestOrderEntryFactory.createOrderEntry("10:00");
        orderEntry.setType(OrderType.STOP.getOrderType());
        orderEntry.setStopPrice(200L);

        MATCHING_ACTION result = stopOrderPreProcessor.preProcess(0L, OrderBook.SIDE.BID, orderEntry);
        assertEquals(MATCHING_ACTION.PARK_ORDER,result);

        UnsafeUtil.freeOrderEntryMemory(orderEntry);
    }
}