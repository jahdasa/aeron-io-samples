package io.aeron.samples.matchingengine.parser;

import dao.TraderDAO;
import io.aeron.samples.matchingengine.data.ExecutionReportData;
import leafNode.OrderEntry;
import org.agrona.DirectBuffer;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import sbe.msg.NewOrderDecoder;
import sbe.msg.OrderCancelReplaceRequestDecoder;

import java.io.UnsupportedEncodingException;

public class OrderCancelReplaceRequestParser {
    private OrderCancelReplaceRequestDecoder orderCancelReplaceRequest = new OrderCancelReplaceRequestDecoder();
    private DateTimeFormatter dateTimeFormatter =  DateTimeFormat.forPattern("yyyyMMdd-HH:mm:ss");
    private int securityId;
    private byte[] expireTime = new byte[NewOrderDecoder.expireTimeLength()];

    public void decode(DirectBuffer buffer, OrderEntry orderEntry, int bufferOffset, int actingBlockLength, int actingVersion) throws UnsupportedEncodingException {
        orderCancelReplaceRequest.wrap(buffer, bufferOffset, actingBlockLength, actingVersion);

        orderEntry.setOrderId(orderCancelReplaceRequest.orderId());
        orderEntry.setClientOrderId(Long.parseLong(orderCancelReplaceRequest.clientOrderId().trim()));
        orderEntry.setOrigClientOrderId(Long.parseLong(orderCancelReplaceRequest.origClientOrderId().trim()));

        securityId = orderCancelReplaceRequest.securityId();
        orderEntry.setTrader(orderCancelReplaceRequest.traderId());

        orderEntry.setType((byte) orderCancelReplaceRequest.orderType().value());
        orderEntry.setTimeInForce((byte) orderCancelReplaceRequest.timeInForce().value());

        String expireTimeText = new  String(expireTime, 0, orderCancelReplaceRequest.getExpireTime(expireTime, 0), OrderCancelReplaceRequestDecoder.expireTimeCharacterEncoding());
        long eTime = dateTimeFormatter.parseMillis(expireTimeText);
        orderEntry.setExpireTime(eTime);

        orderEntry.setSide((byte) orderCancelReplaceRequest.side().value());
        orderEntry.setQuantity(orderCancelReplaceRequest.orderQuantity());
        orderEntry.setDisplayQuantity(orderCancelReplaceRequest.displayQuantity());
        orderEntry.setMinExecutionSize(orderCancelReplaceRequest.minQuantity());
        orderEntry.setPrice(orderCancelReplaceRequest.limitPrice().mantissa());
        orderEntry.setStopPrice(orderCancelReplaceRequest.stopPrice().mantissa());

        populateExecutionData();
    }

    public int getSecurityId(){
        return securityId;
    }

    private void populateExecutionData(){
        ExecutionReportData executionReportData = ExecutionReportData.INSTANCE;
        orderCancelReplaceRequest.getClientOrderId(executionReportData.getClientOrderId(),0);
    }
}
