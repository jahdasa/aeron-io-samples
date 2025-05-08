package io.aeron.samples.matchingengine.crossing.postProcessor;

import bplusTree.BPlusTree;
import io.aeron.samples.matchingengine.crossing.expireRule.ExpireRule;
import io.aeron.samples.matchingengine.crossing.expireRule.MarketOrderExpireRule;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import leafNode.OrderList;
import leafNode.OrderListCursor;
import orderBook.OrderBook;

import java.util.Iterator;
import java.util.Map;

public class ExpireOrderPostProcessor implements MatchingPostProcessor{

    private MarketOrderExpireRule marketOrderExpireRule = new MarketOrderExpireRule();

    public void postProcess(PriceTimePriorityStrategy priceTimePriorityStrategy,OrderBook orderBook) {
        postProcess(orderBook, marketOrderExpireRule);
    }

    public void postProcess(PriceTimePriorityStrategy priceTimePriorityStrategy,OrderBook orderBook,ExpireRule expireRule) {
        postProcess(orderBook, expireRule);
    }

    public void postProcess(OrderBook orderBook,ExpireRule expireRule) {
        execute(orderBook, OrderBook.SIDE.BID,expireRule);
        execute(orderBook,OrderBook.SIDE.OFFER,expireRule);
    }

    private void execute(OrderBook orderBook,OrderBook.SIDE side,ExpireRule expireRule){
        //todo: no GTT
        if(true) return;
        BPlusTree.BPlusTreeIterator iterator  = orderBook.getPriceIterator(side);

        while (iterator.hasNext()) {
            Map.Entry<Long, OrderList> entry = iterator.next();
            OrderList orderList = entry.getValue();

//            System.out.println(side + "@" + entry.getKey() + "@" + orderList.size());

            Iterator<OrderListCursor> orderListIterator = orderList.iterator();
            while (orderListIterator.hasNext()) {
                if(expireRule.isOrderExpired(orderListIterator.next().value)) {
                    orderListIterator.remove();
                }
            }

            if(orderList.total() == 0){
                orderBook.removePrice(entry.getKey(), side);
            }
        }
    }

}
