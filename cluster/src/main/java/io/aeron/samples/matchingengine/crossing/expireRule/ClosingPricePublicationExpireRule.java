package io.aeron.samples.matchingengine.crossing.expireRule;

import common.TimeInForce;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import leafNode.OrderEntry;

public class ClosingPricePublicationExpireRule implements ExpireRule {
    @Override
    public boolean isOrderExpired(OrderEntry orderEntry) {
        if(orderEntry.getTimeInForce() == TimeInForce.GTT.getValue() && MatchingUtil.isPastExpiryDateTime(orderEntry.getExpireTime())) {
            return true;
        }

        return false;
    }
}
