package io.aeron.samples.matchingengine.crossing.tradingSessions;

import com.carrotsearch.hppc.LongObjectHashMap;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import io.aeron.samples.matchingengine.crossing.expireRule.OpeningAuctionExpireRule;
import io.aeron.samples.matchingengine.crossing.postProcessor.ExpireOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.postProcessor.StopOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor;
import io.aeron.samples.matchingengine.crossing.strategy.AuctionStrategy;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import io.aeron.samples.matchingengine.validation.OpeningAuctionCallValidator;

public class FutureClosingAuctionCallProcessor implements TradingSessionProcessor {
    private OpeningAuctionCallValidator validator;
    private PriceTimePriorityStrategy priceTimePriorityStrategy;
    private AuctionStrategy auctionStrategy;
    private LongObjectHashMap<OrderBook> orderBooks;
    private StopOrderPostProcessor stopOrderPostProcessor;
    private ExpireOrderPostProcessor expireOrderPostProcessor;
    private OpeningAuctionExpireRule openingAuctionExpireRule;

    public FutureClosingAuctionCallProcessor(LongObjectHashMap<OrderBook> orderBooks,
                                             PriceTimePriorityStrategy priceTimePriorityStrategy,
                                             AuctionStrategy auctionStrategy,
                                             StopOrderPostProcessor stopOrderPostProcessor,
                                             ExpireOrderPostProcessor expireOrderPostProcessor){
        this.validator = new OpeningAuctionCallValidator();
        this.priceTimePriorityStrategy = priceTimePriorityStrategy;
        this.auctionStrategy = auctionStrategy;
        this.orderBooks = orderBooks;
        this.stopOrderPostProcessor = stopOrderPostProcessor;
        this.expireOrderPostProcessor = expireOrderPostProcessor;
        this.openingAuctionExpireRule = new OpeningAuctionExpireRule();
    }


    @Override
    public void startSession(OrderBook orderBook) {

    }

    @Override
    public void process(OrderBook orderBook,OrderEntry orderEntry) {
        priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER, orderBook, orderEntry);
    }

    @Override
    public void endSession(OrderBook orderBook) {
        auctionStrategy.process(priceTimePriorityStrategy, orderBook);
        stopOrderPostProcessor.postProcess(priceTimePriorityStrategy,orderBook);
        expireOrderPostProcessor.postProcess(priceTimePriorityStrategy,orderBook,openingAuctionExpireRule);
        MatchingUtil.parkGFAOrders(orderBook);
    }

    @Override
    public boolean isOrderValid(OrderEntry orderEntry, int template) {
        return validator.isMessageValidForSession(orderEntry,template);
    }
}
