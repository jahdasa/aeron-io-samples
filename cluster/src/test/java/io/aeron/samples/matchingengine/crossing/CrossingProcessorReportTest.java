package io.aeron.samples.matchingengine.crossing;

import com.carrotsearch.hppc.LongObjectHashMap;
import common.MessageGenerator;
import dao.OrderBookDAO;
import dao.TraderDAO;
import io.aeron.samples.matchingengine.crossing.tradingSessions.TradingSessionFactory;
import orderBook.OrderBook;
import org.agrona.DirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import sbe.reader.BusinessRejectReader;
import sbe.reader.ExecutionReportReader;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class CrossingProcessorReportTest {

    private static final int STOCK_ID = 1;
    private CrossingProcessor crossingProcessor;

    @BeforeEach
    public void setup() throws IOException {
        String dataPath = Paths.get("").toAbsolutePath().getParent() + "/data";
        LongObjectHashMap<OrderBook> orderBooks = OrderBookDAO.loadOrderBooks(dataPath);
        TraderDAO.loadTraders(dataPath);
        TradingSessionFactory.initTradingSessionProcessors(orderBooks);
        crossingProcessor = new CrossingProcessor(orderBooks);
    }

    @Disabled
    @Test
    public void testOrderCancelRequest() throws Exception {

        DirectBuffer msg = MessageGenerator.buildOrderCancelRequest();
        DirectBuffer response = crossingProcessor.processOrder(msg);

        ExecutionReportReader executionReportReader = new ExecutionReportReader();
        StringBuilder sb = executionReportReader.read(response);
        System.out.println(sb.toString());
        assertNotNull(sb);
    }

    @Disabled
    @Test
    public void testOrderCancelRequestRejected() throws Exception {

        DirectBuffer msg = MessageGenerator.buildOrderCancelRequestInvalidSecurity();
        DirectBuffer response = crossingProcessor.processOrder(msg);

        BusinessRejectReader businessRejectReader = new BusinessRejectReader();
        StringBuilder sb = businessRejectReader.read(response);
        System.out.println(sb.toString());
        assertNotNull(sb);
    }


}
