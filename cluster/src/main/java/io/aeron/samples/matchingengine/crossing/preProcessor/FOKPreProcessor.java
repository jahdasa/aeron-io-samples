package io.aeron.samples.matchingengine.crossing.preProcessor;

import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor.MATCHING_ACTION;
import io.aeron.samples.matchingengine.crossing.strategy.TimeInForceStrategy;
import orderBook.OrderBook;

public class FOKPreProcessor {

    public static MATCHING_ACTION preProcess(OrderBook orderBook,int aggOrderQuantity,long aggOrderPrice,int aggOrderMES,OrderBook.SIDE side) {
        if (TimeInForceStrategy.canFOKOrderBeFilled(orderBook.getContraSidePriceIterator(side), aggOrderQuantity, aggOrderPrice, aggOrderMES, side)) {
            return MATCHING_ACTION.AGGRESS_ORDER;
        } else {
            return MATCHING_ACTION.NO_ACTION;
        }
    }
}
