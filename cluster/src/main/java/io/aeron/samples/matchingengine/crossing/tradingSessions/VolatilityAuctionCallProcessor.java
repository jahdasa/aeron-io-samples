package io.aeron.samples.matchingengine.crossing.tradingSessions;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.ObjectArrayList;
import common.TimeInForce;
import io.aeron.samples.matchingengine.crossing.MatchingContext;
import io.aeron.samples.matchingengine.crossing.MatchingUtil;
import io.aeron.samples.matchingengine.crossing.expireRule.VolatilityAuctionExpireRule;
import io.aeron.samples.matchingengine.crossing.postProcessor.ExpireOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.postProcessor.StopOrderPostProcessor;
import io.aeron.samples.matchingengine.crossing.preProcessor.HawkesSimulationPreProcessor;
import io.aeron.samples.matchingengine.crossing.preProcessor.MatchingPreProcessor;
import io.aeron.samples.matchingengine.crossing.strategy.AuctionStrategy;
import io.aeron.samples.matchingengine.crossing.strategy.PriceTimePriorityStrategy;
import io.aeron.samples.matchingengine.data.ExecutionReportData;
import io.aeron.samples.matchingengine.data.MarketData;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import sbe.msg.ExecutionTypeEnum;
import sbe.msg.OrderStatusEnum;
import io.aeron.samples.matchingengine.validation.VolatilityAuctionCallValidator;

import java.util.ArrayList;
import java.util.List;

public class VolatilityAuctionCallProcessor implements TradingSessionProcessor {
    private VolatilityAuctionCallValidator validator;
    private PriceTimePriorityStrategy priceTimePriorityStrategy;
    private AuctionStrategy auctionStrategy;
    private LongObjectHashMap<OrderBook> orderBooks;
    private StopOrderPostProcessor stopOrderPostProcessor;
    private ExpireOrderPostProcessor expireOrderPostProcessor;
    private VolatilityAuctionExpireRule volatilityAuctionExpireRule;
    private List<TimeInForce> timeInForceList;
    private MatchingContext matchingContext = MatchingContext.INSTANCE;
    private ExecutionReportData executionReportData = ExecutionReportData.INSTANCE;
    private OrderEntry oe = new OrderEntry();
    private ObjectArrayList<MatchingPreProcessor> preProcessors;

    public VolatilityAuctionCallProcessor(LongObjectHashMap<OrderBook> orderBooks,
                                          PriceTimePriorityStrategy priceTimePriorityStrategy,
                                          AuctionStrategy auctionStrategy,
                                          StopOrderPostProcessor stopOrderPostProcessor,
                                          ExpireOrderPostProcessor expireOrderPostProcessor){

        this.validator = new VolatilityAuctionCallValidator();
        this.priceTimePriorityStrategy = priceTimePriorityStrategy;
        this.auctionStrategy = auctionStrategy;
        this.orderBooks = orderBooks;
        this.stopOrderPostProcessor = stopOrderPostProcessor;
        this.expireOrderPostProcessor = expireOrderPostProcessor;
        this.volatilityAuctionExpireRule = new VolatilityAuctionExpireRule();

        timeInForceList = new ArrayList<>();
        timeInForceList.add(TimeInForce.GFA);

        initPreProcessors();
    }

    private void initPreProcessors(){
        preProcessors = new ObjectArrayList<>(1);
        preProcessors.add(new HawkesSimulationPreProcessor());
    }

    private void preProcess() {
        for (int i=0; i<preProcessors.size(); i++) {
            preProcessors.get(i).preProcess(matchingContext);

            if (matchingContext.getAction() == MatchingPreProcessor.MATCHING_ACTION.PARK_ORDER || matchingContext.getAction() == MatchingPreProcessor.MATCHING_ACTION.NO_ACTION) {
                break;
            }
        }
    }


    @Override
    public void startSession(OrderBook orderBook) {
        System.out.println("Volatility trading session started");
        MatchingUtil.injectOrders(orderBook, priceTimePriorityStrategy, timeInForceList);
    }

    @Override
    public void process(OrderBook orderBook,OrderEntry orderEntry) {
        orderEntry.setOrderStatus((byte) OrderStatusEnum.New.value());
        if(orderEntry.getOrderId() == 0){
            orderEntry.setOrderId(MatchingUtil.getNextOrderId());
        }


        ExecutionReportData.INSTANCE.buildOrderView(orderEntry,orderBook.getSecurityId());
        matchingContext.setOrderEntry(orderEntry);
        executionReportData.setOrderId((int) orderEntry.getOrderId());
        executionReportData.setExecutionType(ExecutionTypeEnum.New);
        MarketData.INSTANCE.setSecurityId(orderBook.getSecurityId());
        matchingContext.setOrderBook(orderBook);

        preProcess();

        if(matchingContext.getAction() != MatchingPreProcessor.MATCHING_ACTION.NO_ACTION) {
            priceTimePriorityStrategy.process(MatchingPreProcessor.MATCHING_ACTION.ADD_ORDER, orderBook, orderEntry);
        }

        MatchingUtil.publishBestBidOffer(orderBook,oe);
    }

    @Override
    public void endSession(OrderBook orderBook) {
        auctionStrategy.process(priceTimePriorityStrategy, orderBook);
        stopOrderPostProcessor.postProcess(priceTimePriorityStrategy,orderBook);
        expireOrderPostProcessor.postProcess(priceTimePriorityStrategy,orderBook,volatilityAuctionExpireRule);
        MatchingUtil.parkGFAOrders(orderBook);
        MatchingUtil.publishBestBidOffer(orderBook,oe);
    }

    @Override
    public boolean isOrderValid(OrderEntry orderEntry, int template) {
        return validator.isMessageValidForSession(orderEntry,template);
    }
}
