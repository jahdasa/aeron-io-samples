package io.aeron.samples.matchingengine.crossing.tradingSessions;

import com.carrotsearch.hppc.LongObjectHashMap;
import io.aeron.samples.matchingengine.crossing.expireRule.ClosingPricePublicationExpireRule;
import io.aeron.samples.matchingengine.crossing.postProcessor.ExpireOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.strategy.AuctionStrategy;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import io.aeron.samples.matchingengine.validation.ClosingPricePublicationValidator;

public class ClosingPriceCrossProcessor implements TradingSessionProcessor {
    private ClosingPricePublicationValidator validator;
    private PriceTimePriorityStrategy priceTimePriorityStrategy;
    private AuctionStrategy auctionStrategy;
    private LongObjectHashMap<OrderBook> orderBooks;
    private ExpireOrderPostProcessor expireOrderPostProcessor;
    private ClosingPricePublicationExpireRule closingPricePublicationExpireRule;

    public ClosingPriceCrossProcessor(LongObjectHashMap<OrderBook> orderBooks,
                                      PriceTimePriorityStrategy priceTimePriorityStrategy,
                                      AuctionStrategy auctionStrategy,
                                      ExpireOrderPostProcessor expireOrderPostProcessor){

        this.validator = new ClosingPricePublicationValidator();
        this.priceTimePriorityStrategy = priceTimePriorityStrategy;
        this.auctionStrategy = auctionStrategy;
        this.orderBooks = orderBooks;
        this.expireOrderPostProcessor = expireOrderPostProcessor;
        this.closingPricePublicationExpireRule = new ClosingPricePublicationExpireRule();

    }


    @Override
    public void startSession(OrderBook orderBook) {

    }

    @Override
    public void process(OrderBook orderBook,OrderEntry orderEntry) {
        //No executions during this session
    }

    @Override
    public void endSession(OrderBook orderBook) {
        auctionStrategy.process(priceTimePriorityStrategy, orderBook);
        expireOrderPostProcessor.postProcess(priceTimePriorityStrategy, orderBook, closingPricePublicationExpireRule);
    }

    @Override
    public boolean isOrderValid(OrderEntry orderEntry, int template) {
        return validator.isMessageValidForSession(orderEntry,template);
    }
}
