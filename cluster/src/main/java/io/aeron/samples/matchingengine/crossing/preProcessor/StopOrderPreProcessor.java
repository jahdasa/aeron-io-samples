package io.aeron.samples.matchingengine.crossing.preProcessor;

import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor.MATCHING_ACTION;
import leafNode.OrderEntry;
import orderBook.OrderBook;

public class StopOrderPreProcessor {

    public static MATCHING_ACTION preProcess(long lastTradedPrice, OrderBook.SIDE side, OrderEntry orderEntry) {
        if(MatchingUtil.canConverStopOrder(lastTradedPrice,side,orderEntry.getStopPrice())){
            MatchingUtil.convertStopOrderToMarketOrLimitOrder(orderEntry);
            return MATCHING_ACTION.AGGRESS_ORDER;
        }

        return MATCHING_ACTION.PARK_ORDER;
    }
}
