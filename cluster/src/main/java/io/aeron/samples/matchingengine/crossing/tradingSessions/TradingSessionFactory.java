package io.aeron.samples.matchingengine.crossing.tradingSessions;

import com.carrotsearch.hppc.LongObjectHashMap;
import io.aeron.samples.matchingengine.crossing.postProcessor.ExpireOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.postProcessor.StopOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.strategy.AuctionStrategy;
import io.aeron.samples.matchingengine.crossing.strategy.FilterAndUncrossStrategy;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import orderBook.OrderBook;
import sbe.msg.marketData.TradingSessionEnum;

public class TradingSessionFactory {

    private static PriceTimePriorityStrategy priceTimePriorityStrategy  = new PriceTimePriorityStrategy();
    private static FilterAndUncrossStrategy filterAndUncrossStrategy  = new FilterAndUncrossStrategy();
    private static AuctionStrategy auctionStrategy = new AuctionStrategy();

    private static StartOfTradingProcessor startOfTradingProcessor;
    private static OpeningAuctionCallProcessor openingAuctionCallProcessor;
    private static ContinuousTradingProcessor continuousTradingProcessor;
    private static FutureClosingAuctionCallProcessor futureClosingAuctionCallProcessor;
    private static VolatilityAuctionCallProcessor volatilityAuctionCallProcessor;
    private static IntraDayAuctionCallProcessor intraDayAuctionCallProcessor;
    private static ClosingPriceCrossProcessor closingPriceCrossProcessor;
    private static ClosingAuctionCallProcessor closingAuctionCallProcessor;
    private static ClosingPricePublicationProcessor closingPricePublicationProcessor;
    private static PostCloseProcessor postCloseProcessor;

    private static StopOrderPostProcessor stopOrderPostProcessor  = new StopOrderPostProcessor();
    private static ExpireOrderPostProcessor expireOrderPostProcessor  = new ExpireOrderPostProcessor();


    public static void initTradingSessionProcessors(LongObjectHashMap<OrderBook> orderBooks){
        startOfTradingProcessor = new StartOfTradingProcessor();
        openingAuctionCallProcessor = new OpeningAuctionCallProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,stopOrderPostProcessor,expireOrderPostProcessor);
        continuousTradingProcessor = new ContinuousTradingProcessor(orderBooks,priceTimePriorityStrategy,filterAndUncrossStrategy,expireOrderPostProcessor);
        futureClosingAuctionCallProcessor = new FutureClosingAuctionCallProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,stopOrderPostProcessor,expireOrderPostProcessor);
        volatilityAuctionCallProcessor = new VolatilityAuctionCallProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,stopOrderPostProcessor,expireOrderPostProcessor);
        intraDayAuctionCallProcessor = new IntraDayAuctionCallProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,stopOrderPostProcessor,expireOrderPostProcessor);
        closingPriceCrossProcessor = new ClosingPriceCrossProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,expireOrderPostProcessor);
        closingAuctionCallProcessor = new ClosingAuctionCallProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,stopOrderPostProcessor,expireOrderPostProcessor);
        closingPricePublicationProcessor = new ClosingPricePublicationProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,expireOrderPostProcessor);
        postCloseProcessor = new PostCloseProcessor(orderBooks,priceTimePriorityStrategy,auctionStrategy,expireOrderPostProcessor);

    }

    public static TradingSessionProcessor getTradingSessionProcessor(final TradingSessionEnum tradingSessionEnum)
    {
        if(tradingSessionEnum == null)
        {
            return null;
        }
        return switch (tradingSessionEnum)
        {
            case StartOfTrading -> startOfTradingProcessor;
            case OpeningAuctionCall -> openingAuctionCallProcessor;
            case ContinuousTrading -> continuousTradingProcessor;
            case FCOAuctionCall -> futureClosingAuctionCallProcessor;
            case VolatilityAuctionCall -> volatilityAuctionCallProcessor;
            case IntraDayAuctionCall -> intraDayAuctionCallProcessor;
            case ClosingAuctionCall -> closingPriceCrossProcessor;
            case ClosingPricePublication -> closingAuctionCallProcessor;
            case ClosingPriceCross -> closingPricePublicationProcessor;
            case PostClose -> postCloseProcessor;
            default -> startOfTradingProcessor;
        };

    }

    public static void reset(){
        priceTimePriorityStrategy  = new PriceTimePriorityStrategy();
        filterAndUncrossStrategy  = new FilterAndUncrossStrategy();
        auctionStrategy = new AuctionStrategy();
        stopOrderPostProcessor  = new StopOrderPostProcessor();
        expireOrderPostProcessor  = new ExpireOrderPostProcessor();
    }
}
