package io.aeron.samples.matchingengine.crossing.strategy;

import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor.MATCHING_ACTION;
import leafNode.OrderEntry;
import orderBook.OrderBook;

public interface MatchingLogic {
    void process(MATCHING_ACTION action, OrderBook orderBook, OrderEntry orderEntry);
}
