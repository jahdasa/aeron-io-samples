package io.aeron.samples.matchingengine.crossing.expireRule;

import common.OrderType;
import common.TimeInForce;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import leafNode.OrderEntry;

public class OpeningAuctionExpireRule implements ExpireRule {
    @Override
    public boolean isOrderExpired(OrderEntry orderEntry) {
        if(orderEntry.getType() == OrderType.MARKET.getOrderType() ||
           orderEntry.getTimeInForce() == TimeInForce.OPG.getValue() ||
           (orderEntry.getTimeInForce() == TimeInForce.GTT.getValue() && MatchingUtil.isPastExpiryDateTime(orderEntry.getExpireTime()))){
            return true;
        }

        return false;
    }
}
