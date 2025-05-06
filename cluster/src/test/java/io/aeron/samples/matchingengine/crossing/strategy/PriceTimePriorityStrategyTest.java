package io.aeron.samples.matchingengine.crossing.strategy;

import com.carrotsearch.hppc.ObjectArrayList;
import io.aeron.samples.matchingengine.crossing.OrderData;
import io.aeron.samples.matchingengine.crossing.OrderLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by dharmeshsing on 7/07/15.
 */
public class PriceTimePriorityStrategyTest {

    PriceTimePriorityStrategy priceTimePriorityStrategy;

    @BeforeEach
    public void setup(){
        priceTimePriorityStrategy = new PriceTimePriorityStrategy();
    }

    @Test
    public void testAddOrder() throws Exception {

    }

    public static Object[] provideFilterData() {
        OrderLoader orderLoader = new OrderLoader();
        try {
            ObjectArrayList<OrderData> orderDataList = orderLoader.getFilterAndUncrossOrders();
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