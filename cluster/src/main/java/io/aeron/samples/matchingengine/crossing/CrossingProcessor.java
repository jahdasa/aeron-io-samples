package io.aeron.samples.matchingengine.crossing;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.cursors.LongObjectCursor;
import io.aeron.samples.matchingengine.crossing.tradingSessions.TradingSessionFactory;
import io.aeron.samples.matchingengine.crossing.tradingSessions.TradingSessionProcessor;
import io.aeron.samples.matchingengine.data.BusinessRejectReportData;
import io.aeron.samples.matchingengine.data.ExecutionReportData;
import io.aeron.samples.matchingengine.data.HDRData;
import io.aeron.samples.matchingengine.data.MarketData;
import io.aeron.samples.matchingengine.engine.MatchingEngine;
import io.aeron.samples.matchingengine.parser.TradeGatewayParser;
import leafNode.OrderEntry;
import orderBook.OrderBook;
import org.agrona.DirectBuffer;
import sbe.builder.ListInstrumentsResponseBuilder;
import sbe.msg.*;
import sbe.msg.marketData.SessionChangedReasonEnum;
import sbe.msg.marketData.TradingSessionDecoder;
import sbe.msg.marketData.TradingSessionEnum;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CrossingProcessor implements LOBManager {

    public static AtomicInteger sequenceNumber = new AtomicInteger();
    private TradeGatewayParser tradeGatewayParser;
    private LongObjectHashMap<OrderBook> orderBooks;
    private boolean clientMarketDataRequest;
    private boolean clientMarketDepthRequest;
    private boolean adminRequest;

    public CrossingProcessor(LongObjectHashMap<OrderBook> orderBooks){
        this.orderBooks = orderBooks;
        this.tradeGatewayParser = new TradeGatewayParser();
    }


    public OrderBook getOrderBook(int stockid){
        return orderBooks.get(stockid);
    }

    @Override
    public DirectBuffer processOrder(DirectBuffer message)
    {
        ExecutionReportData.INSTANCE.reset();
        MarketData.INSTANCE.reset();
        BusinessRejectReportData.INSTANCE.reset();
        BusinessRejectEnum rejectEnum = BusinessRejectEnum.NULL_VAL;

        clientMarketDataRequest = false;
        clientMarketDepthRequest = false;
        adminRequest = false;

        try
        {
            tradeGatewayParser.parse(message);
        }
        catch (final UnsupportedEncodingException e)
        {
            //TODO: handle exception
            e.printStackTrace();
            return null;
        }

        int template = tradeGatewayParser.getTemplateId();

        if (template == AdminDecoder.TEMPLATE_ID)
        {
            processAdminMessage(tradeGatewayParser.getAdminTypeEnum(), tradeGatewayParser.getSecurityId());
        }
        else if (template == TradingSessionDecoder.TEMPLATE_ID)
        {
            changeTradingSession(
                tradeGatewayParser.getSecurityId(),
                tradeGatewayParser.getTradingSessionEnum(),
                SessionChangedReasonEnum.ScheduledTransition);
        }
        else if (template == NewInstrumentDecoder.TEMPLATE_ID)
        {
            adminRequest = true;

            final NewInstrumentCompleteStatus status = processNewInstrumentMessage(
                tradeGatewayParser.getSecurityId(),
                tradeGatewayParser.getInstrumentCode(),
                tradeGatewayParser.getInstrumentName());

            return ExecutionReportData.INSTANCE.buildNewInstrumentReport(
                tradeGatewayParser.getSecurityId(),
                tradeGatewayParser.getInstrumentCode(),
                status);
        }
        else if (template == ListInstrumentsMessageRequestDecoder.TEMPLATE_ID)
        {
            adminRequest = true;

            final List<ListInstrumentsResponseBuilder.Instrument> instruments = processListInstrumentsMessage();

            return ExecutionReportData.INSTANCE.buildListInstrumentsReport(
                tradeGatewayParser.getCorrelationId(),
                instruments);
        }
        else
        {
            final OrderEntry orderEntry = tradeGatewayParser.getOrderEntry();
            final int securityId = tradeGatewayParser.getSecurityId();
            processOrder(template, securityId, orderEntry);

            return ExecutionReportData.INSTANCE.buildExecutionReport(orderEntry, securityId);
        }

        //Return correct trading report here
        return null;
    }

    @Override
    public boolean isClientMarketDataRequest() {
        return clientMarketDataRequest;
    }

    @Override
    public boolean isClientMarketDepthRequest() {
        return clientMarketDepthRequest;
    }

    @Override
    public boolean isAdminRequest() {
        return adminRequest;
    }

    private void changeTradingSession(int securityId, TradingSessionEnum newTradingSession, SessionChangedReasonEnum sessionChangedReason){
        OrderBook orderBook = orderBooks.get(securityId);
        if(sessionChangedReason == null){
            sessionChangedReason = SessionChangedReasonEnum.ScheduledTransition;
        }

        TradingSessionProcessor tradingSessionProcessor = getOrderBookTradingSession(securityId);
        tradingSessionProcessor.endSession(orderBook);

        tradingSessionProcessor = TradingSessionFactory.getTradingSessionProcessor(newTradingSession);

        MatchingContext.INSTANCE.setOrderBookTradingSession(securityId,newTradingSession);
        MarketData.INSTANCE.addSymbolStatus(securityId, sessionChangedReason,newTradingSession, orderBook.getStaticPriceReference(), orderBook.getDynamicPriceReference());

        tradingSessionProcessor.startSession(orderBook);
    }

    public void processOrder(final int template, final int securityId, final OrderEntry orderEntry)
    {
        OrderBook orderBook = orderBooks.get(securityId);
        MatchingContext.INSTANCE.setTemplateId(template);

        TradingSessionProcessor tradingSessionProcessor = getOrderBookTradingSession(securityId);
        if(tradingSessionProcessor.isOrderValid(orderEntry,template)) {
            tradingSessionProcessor.process(orderBook, orderEntry);
        }

        if(orderBook.isCircuitBreakerBreached())
        {
            changeTradingSession(securityId, TradingSessionEnum.VolatilityAuctionCall, SessionChangedReasonEnum.CircuitBreakerTripped);
        }
    }

    private TradingSessionProcessor getOrderBookTradingSession(long securityId){
        TradingSessionEnum tradingSessionEnum = MatchingContext.INSTANCE.getOrderBookTradingSession(securityId);
        return TradingSessionFactory.getTradingSessionProcessor(tradingSessionEnum);
    }

    private void processAdminMessage(final AdminTypeEnum adminTypeEnum, final int securityId)
    {
        switch (adminTypeEnum)
        {
            case WarmUpComplete:
                warmupComplete();
                break;
            case SimulationComplete:
                simulationComplete();
                break;
            case LOB:
                lobSnapShot(securityId);
                break;
            case VWAP:
                calculateVWAP(securityId);
                break;
            case MarketDepth:
                calculateMarketDepth(securityId);
                break;
            case ShutDown:
            {
                MarketData.INSTANCE.addShutDownRequest();
                MatchingEngine.setRunning(false);
            }
            break;
            case BestBidOfferRequest:
                resendBBO(securityId);
                break;
            case StartMessage:
                MarketData.INSTANCE.addStartMessage(securityId);
                break;
            case EndMessage:
                MarketData.INSTANCE.addEndMessage(securityId);
                break;
            default:
                return;
        }
    }

    private NewInstrumentCompleteStatus processNewInstrumentMessage(
        final int securityId,
        final String instrumentCode,
        final String instrumentName)
    {
        final boolean exist = orderBooks.containsKey(securityId);

        if (exist)
        {
            return NewInstrumentCompleteStatus.DuplicatedSecurityIdOrCode;
        }

        final OrderBook orderBook = new OrderBook(securityId, instrumentCode, instrumentName);
        orderBooks.put(securityId, orderBook);

        MatchingContext.INSTANCE.setOrderBookTradingSession(securityId, TradingSessionEnum.ContinuousTrading);

        clientMarketDataRequest = false;
        clientMarketDepthRequest = false;

        return NewInstrumentCompleteStatus.Successful;
    }

    private List<ListInstrumentsResponseBuilder.Instrument> processListInstrumentsMessage()
    {
        final List<ListInstrumentsResponseBuilder.Instrument> instruments = new ArrayList<>();

        final Iterator<LongObjectCursor<OrderBook>> iterator = orderBooks.iterator();
        while (iterator.hasNext())
        {
            final LongObjectCursor<OrderBook> orderBook = iterator.next();
            instruments.add(new ListInstrumentsResponseBuilder.Instrument(
                    (int) orderBook.value.getSecurityId(),
                    orderBook.value.getCode(),
                    orderBook.value.getName()));
        }

        clientMarketDataRequest = false;
        clientMarketDepthRequest = false;

        return instruments;
    }

    private void warmupComplete(){
        HDRData.INSTANCE.reset();
        for(int i=0; i<orderBooks.size(); i++){
            if(orderBooks.get(i) != null) {
                orderBooks.get(i).freeAll();
            }
        }
    }

    private void simulationComplete(){
        HDRData.INSTANCE.storeHDRStats();
    }

    private void lobSnapShot(int securityId)
    {
        clientMarketDataRequest = true;
        MarketData.INSTANCE.setSnapShotRequest(true);
        MarketData.INSTANCE.setSecurityId(securityId);
        MarketData.INSTANCE.setOrderBook(orderBooks.get(securityId));
    }

    private void calculateVWAP(final int securityId)
    {
        final OrderBook orderBook = orderBooks.get(securityId);

        clientMarketDataRequest = true;
        MarketData.INSTANCE.calcVWAP(orderBook);
    }

    private void calculateMarketDepth(int securityId)
    {
        clientMarketDepthRequest = true;
        MarketData.INSTANCE.setMarketDepthRequest(true);
        MarketData.INSTANCE.setOrderBook(orderBooks.get(securityId));
    }

    private void resendBBO(int securityId)
    {
        clientMarketDataRequest = true;
        MatchingUtil.publishBestBidOffer(orderBooks.get(securityId));
    }

    @Override
    public int reportMessageLength()
    {
        int templateId = tradeGatewayParser.getTemplateId();

        if(templateId == NewInstrumentDecoder.TEMPLATE_ID)
        {
            return ExecutionReportData.INSTANCE.getNewInstrumentCompleteMessageLength();
        }
        if(templateId == ListInstrumentsMessageRequestDecoder.TEMPLATE_ID)
        {
            return ExecutionReportData.INSTANCE.getListInstrumentResponseMessageLength();
        }

        return ExecutionReportData.INSTANCE.getExecutionReportMessageLength();
    }
}
