package io.aeron.samples.matchingengine.parser;

import dao.TraderDAO;
import io.aeron.samples.matchingengine.data.ExecutionReportData;
import leafNode.OrderEntry;
import org.agrona.DirectBuffer;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import sbe.msg.NewOrderDecoder;

import java.io.UnsupportedEncodingException;

public class NewOrderParser {
    private NewOrderDecoder newOrder = new NewOrderDecoder();
    private DateTimeFormatter dateTimeFormatter =  DateTimeFormat.forPattern("yyyyMMdd-HH:mm:ss");
    private int securityId;
    private byte[] expireTime = new byte[NewOrderDecoder.expireTimeLength()];
    private byte[] clientOrderId = new byte[NewOrderDecoder.clientOrderIdLength()];

    public void decode(DirectBuffer buffer, OrderEntry orderEntry, int bufferOffset, int actingBlockLength, int actingVersion) throws UnsupportedEncodingException {
        newOrder.wrap(buffer, bufferOffset, actingBlockLength, actingVersion);

        securityId = newOrder.securityId();
        orderEntry.setTrader(newOrder.traderId());

        orderEntry.setType((byte) newOrder.orderType().value());
        orderEntry.setTimeInForce((byte) newOrder.timeInForce().value());

        String expireTimeText = new  String(expireTime, 0, newOrder.getExpireTime(expireTime, 0), NewOrderDecoder.expireTimeCharacterEncoding());
        long eTime = dateTimeFormatter.parseMillis(expireTimeText);
        orderEntry.setExpireTime(eTime);

        String clientOrderIdText = new String(clientOrderId, 0, newOrder.getClientOrderId(clientOrderId, 0), newOrder.clientOrderIdCharacterEncoding()).trim();
        orderEntry.setClientOrderId(Long.parseLong(clientOrderIdText));

        orderEntry.setSide((byte) newOrder.side().value());
        orderEntry.setQuantity(newOrder.orderQuantity());
        orderEntry.setDisplayQuantity(newOrder.displayQuantity());
        orderEntry.setMinExecutionSize(newOrder.minQuantity());
        orderEntry.setPrice(newOrder.limitPrice().mantissa());
        orderEntry.setStopPrice(newOrder.stopPrice().mantissa());

        populateExecutionData();

    }

    public int getSecurityId(){
        return securityId;
    }

    private void populateExecutionData(){
        newOrder.getClientOrderId(ExecutionReportData.INSTANCE.getClientOrderId(),0);
    }
}
