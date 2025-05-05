package io.aeron.samples.matchingengine.crossing.postProcessor;

import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import orderBook.OrderBook;

public interface MatchingPostProcessor {
    void postProcess(PriceTimePriorityStrategy priceTimePriorityStrategy,OrderBook orderBook);
}
