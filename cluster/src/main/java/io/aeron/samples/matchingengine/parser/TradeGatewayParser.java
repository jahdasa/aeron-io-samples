package io.aeron.samples.matchingengine.parser;

import com.carrotsearch.hppc.LongObjectHashMap;
import io.aeron.samples.matchingengine.data.BusinessRejectReportData;
import io.aeron.samples.matchingengine.data.ExecutionReportData;
import io.aeron.samples.matchingengine.data.MarketData;
import leafNode.OrderEntry;
import leafNode.OrderEntryFactory;
import orderBook.OrderBook;
import org.agrona.DirectBuffer;
import sbe.msg.*;
import sbe.msg.marketData.TradingSessionDecoder;
import sbe.msg.marketData.TradingSessionEnum;

import java.io.UnsupportedEncodingException;

public class TradeGatewayParser
{
    private MessageHeaderDecoder messageHeader = new MessageHeaderDecoder();

    private OrderEntry orderEntry;
    private NewOrderParser newOrderParser;
    private OrderCancelRequestParser orderCancelRequestParser;
    private OrderCancelReplaceRequestParser orderCancelReplaceRequestParser;
    private TradingSessionParser tradingSessionParser;
    private AdminMessageParser adminMessageParser;
    private NewInstrumentMessageParser newInstrumentMessageParser;
    private ListInstrumentsMessageParser listInstrumentsMessageParser;

    private TradingSessionEnum tradingSessionEnum;
    private AdminTypeEnum adminTypeEnum;

    private int securityId;
    private String instrumentCode;
    private String instrumentName;
    private String correlationId;

    private int bufferIndex;
    private int templateId;
    private int actingBlockLength;
    private int actingVersion;

    public TradeGatewayParser(){
        messageHeader = new MessageHeaderDecoder();
        orderEntry = OrderEntryFactory.getOrderEntry();
        newOrderParser = new NewOrderParser();
        orderCancelRequestParser = new OrderCancelRequestParser();
        orderCancelReplaceRequestParser = new OrderCancelReplaceRequestParser();
        tradingSessionParser = new TradingSessionParser();
        adminMessageParser = new AdminMessageParser();
        newInstrumentMessageParser = new NewInstrumentMessageParser();
        listInstrumentsMessageParser = new ListInstrumentsMessageParser();
    }

    private void init(DirectBuffer buffer){
        bufferIndex = 0;
        messageHeader.wrap(buffer, 0);
        templateId = messageHeader.templateId();
        actingBlockLength = messageHeader.blockLength();
        actingVersion = messageHeader.version();
        bufferIndex += messageHeader.encodedLength();

        initOrderEntry();
        setReportData();
    }

    private void initOrderEntry(){
        orderEntry.clear();
        orderEntry.setSubmittedTime(System.nanoTime());
    }

    public void parse(DirectBuffer buffer) throws UnsupportedEncodingException {
        init(buffer);

        if (templateId == NewOrderEncoder.TEMPLATE_ID)
        {
            parseNewOrder(buffer);
        }
        else if (templateId == OrderCancelRequestEncoder.TEMPLATE_ID)
        {
            parseCancelOrderRequest(buffer);
        }
        else if (templateId == OrderCancelReplaceRequestEncoder.TEMPLATE_ID)
        {
            parseCancelReplaceOrderRequest(buffer);
        }
        else if (templateId == TradingSessionDecoder.TEMPLATE_ID)
        {
            parseTradingSession(buffer);
        }
        else if (templateId == AdminDecoder.TEMPLATE_ID)
        {
            parseAdminMessage(buffer);
        }
        else if (templateId == LOBEncoder.TEMPLATE_ID)
        {
            parseAdminMessage(buffer);
        }
        else if (templateId == NewInstrumentEncoder.TEMPLATE_ID)
        {
            parseNewInstrumentMessage(buffer);
        }
        else if (templateId == ListInstrumentsMessageRequestDecoder.TEMPLATE_ID)
        {
            parseListInstrumentsMessage(buffer);
        }
    }

    private void parseNewOrder(DirectBuffer buffer) throws UnsupportedEncodingException {
        newOrderParser.decode(buffer,orderEntry,bufferIndex,actingBlockLength,actingVersion);
        securityId = newOrderParser.getSecurityId();
    }

    private void parseCancelOrderRequest(DirectBuffer buffer) throws UnsupportedEncodingException {
        orderCancelRequestParser.decode(buffer,orderEntry,bufferIndex,actingBlockLength,actingVersion);
        securityId = orderCancelRequestParser.getSecurityId();
    }

    private void parseCancelReplaceOrderRequest(DirectBuffer buffer) throws UnsupportedEncodingException {
        orderCancelReplaceRequestParser.decode(buffer,orderEntry,bufferIndex,actingBlockLength,actingVersion);
        securityId = orderCancelReplaceRequestParser.getSecurityId();
    }

    private void parseTradingSession(DirectBuffer buffer) throws UnsupportedEncodingException {
        tradingSessionParser.decode(buffer,bufferIndex,actingBlockLength,actingVersion);
        tradingSessionEnum = tradingSessionParser.getTradingSessionEnum();
        securityId = tradingSessionParser.getSecurityId();
    }

    private void parseAdminMessage(DirectBuffer buffer) throws UnsupportedEncodingException {
        adminMessageParser.decode(buffer, bufferIndex, actingBlockLength, actingVersion);
        adminTypeEnum = adminMessageParser.getAdminTypeEnum();
        securityId = adminMessageParser.getSecurityId();
    }

    private void parseNewInstrumentMessage(DirectBuffer buffer) throws UnsupportedEncodingException
    {
        newInstrumentMessageParser.decode(buffer, bufferIndex, actingBlockLength, actingVersion);
        securityId = newInstrumentMessageParser.getSecurityId();
        instrumentCode = newInstrumentMessageParser.getCode();
        instrumentName = newInstrumentMessageParser.getName();
    }

    private void parseListInstrumentsMessage(DirectBuffer buffer)
    {
        listInstrumentsMessageParser.decode(buffer, bufferIndex, actingBlockLength, actingVersion);
        correlationId = listInstrumentsMessageParser.getCorrelationId();
    }


    public BusinessRejectEnum isValid(LongObjectHashMap<OrderBook> orderBooks){
        if(orderBooks.get(securityId) == null){
            return BusinessRejectEnum.UnknownInstrument;
        }

        return BusinessRejectEnum.NULL_VAL;
    }

    private void setReportData(){
        ExecutionReportData.INSTANCE.setCompID(messageHeader.compID());
        BusinessRejectReportData.INSTANCE.setCompID(messageHeader.compID());
        MarketData.INSTANCE.setCompID(messageHeader.compID());
    }


    public OrderEntry getOrderEntry(){
        return orderEntry;
    }

    public int getTemplateId() {
        return templateId;
    }

    public int getSecurityId() {
        return securityId;
    }

    public TradingSessionEnum getTradingSessionEnum(){
        return this.tradingSessionEnum;
    }

    public AdminTypeEnum getAdminTypeEnum() {
        return adminTypeEnum;
    }

    public String getInstrumentCode()
    {
        return instrumentCode;
    }

    public String getInstrumentName()
    {
        return instrumentName;
    }

    public String getCorrelationId()
    {
        return correlationId;
    }
}
