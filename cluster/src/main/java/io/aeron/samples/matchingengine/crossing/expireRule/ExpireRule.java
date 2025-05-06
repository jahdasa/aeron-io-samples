package io.aeron.samples.matchingengine.crossing.expireRule;

import leafNode.OrderEntry;

public interface ExpireRule {
    boolean isOrderExpired(OrderEntry orderEntry);
}
