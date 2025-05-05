package io.aeron.samples.matchingengine.crossing.tradingSessions;

import com.carrotsearch.hppc.LongObjectHashMap;
import common.TimeInForce;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import io.aeron.samples.matchingengine.crossing.expireRule.ClosingAuctionExpireRule;
import io.aeron.samples.matchingengine.crossing.postProcessor.ExpireOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.postProcessor.StopOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor;
import io.aeron.samples.matchingengine.crossing.strategy.AuctionStrategy;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import io.aeron.samples.matchingengine.validation.ClosingAuctionCallValidator;

import java.util.ArrayList;
import java.util.List;

public class ClosingAuctionCallProcessor implements TradingSessionProcessor {
    private ClosingAuctionCallValidator validator;
    private PriceTimePriorityStrategy priceTimePriorityStrategy;
    private AuctionStrategy auctionStrategy;
    private LongObjectHashMap<OrderBook> orderBooks;
    private StopOrderPostProcessor stopOrderPostProcessor;
    private ExpireOrderPostProcessor expireOrderPostProcessor;
    private ClosingAuctionExpireRule closingAuctionExpireRule;
    private List<TimeInForce> timeInForceList;

    public ClosingAuctionCallProcessor(LongObjectHashMap<OrderBook> orderBooks,
                                       PriceTimePriorityStrategy priceTimePriorityStrategy,
                                       AuctionStrategy auctionStrategy,
                                       StopOrderPostProcessor stopOrderPostProcessor,
                                       ExpireOrderPostProcessor expireOrderPostProcessor){

        this.validator = new ClosingAuctionCallValidator();
        this.priceTimePriorityStrategy = priceTimePriorityStrategy;
        this.auctionStrategy = auctionStrategy;
        this.orderBooks = orderBooks;
        this.stopOrderPostProcessor = stopOrderPostProcessor;
        this.expireOrderPostProcessor = expireOrderPostProcessor;
        this.closingAuctionExpireRule = new ClosingAuctionExpireRule();

        timeInForceList = new ArrayList<>();
        timeInForceList.add(TimeInForce.GFA);
        timeInForceList.add(TimeInForce.ATC);
    }


    @Override
    public void startSession(OrderBook orderBook) {
        MatchingUtil.injectOrders(orderBook, priceTimePriorityStrategy, timeInForceList);
    }

    @Override
    public void process(OrderBook orderBook,OrderEntry orderEntry) {
        priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER, orderBook, orderEntry);
    }

    @Override
    public void endSession(OrderBook orderBook) {
        auctionStrategy.process(priceTimePriorityStrategy, orderBook);
        stopOrderPostProcessor.postProcess(priceTimePriorityStrategy,orderBook);
        expireOrderPostProcessor.postProcess(priceTimePriorityStrategy,orderBook,closingAuctionExpireRule);
        MatchingUtil.parkGFAOrders(orderBook);
    }

    @Override
    public boolean isOrderValid(OrderEntry orderEntry, int template) {
        return validator.isMessageValidForSession(orderEntry,template);
    }
}
