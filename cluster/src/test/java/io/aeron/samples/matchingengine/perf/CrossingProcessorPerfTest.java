package io.aeron.samples.matchingengine.perf;

import com.carrotsearch.hppc.LongObjectHashMap;
import io.aeron.samples.matchingengine.crossing.CrossingProcessor;
import dao.OrderBookDAO;
import dao.TraderDAO;
import orderBook.OrderBook;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.builder.NewOrderBuilder;
import sbe.msg.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by dharmeshsing on 17/08/15.
 */
public class CrossingProcessorPerfTest {
    public Properties properties;
    public NewOrderBuilder newOrderBuilder = new NewOrderBuilder();
    UnsafeBuffer encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(114));

    public  void loadProperties(String propertiesFile) throws IOException {
        //try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
        try(InputStream inputStream = new FileInputStream(Paths.get("").toAbsolutePath().getParent() + "/MatchingEngine/build/install/MatchingEngine/resources/" + propertiesFile)) {

            if (inputStream != null) {
                properties = new Properties();
                properties.load(inputStream);
            } else {
                throw new IOException("Unable to load properties file " + propertiesFile);
            }
        }
    }

    public DirectBuffer createNewOrder(){
        return newOrderBuilder.compID(1)
                .clientOrderId("1                   ".getBytes())
                .account("account123".getBytes())
                .capacity(CapacityEnum.Agency)
                .cancelOnDisconnect(CancelOnDisconnectEnum.DoNotCancel)
                .orderBook(OrderBookEnum.Regular)
                .securityId(1)
                .traderId(1)
                .orderType(OrdTypeEnum.Limit)
                .timeInForce(TimeInForceEnum.Day)
                .expireTime("20150813-23:00:00".getBytes())
                .side(SideEnum.BUY)
                .orderQuantity(10)
                .displayQuantity(10)
                .minQuantity(1000)
                .limitPrice(1000)
                .stopPrice(0)
                .build(encodeBuffer, 0);
    }

    public static void main(String[] args) throws Exception {
        CrossingProcessorPerfTest test = new CrossingProcessorPerfTest();
        test.loadProperties("MatchingEngine.properties");

        String dataPath = test.properties.getProperty("DATA_PATH");
        LongObjectHashMap<OrderBook> orderBooks = OrderBookDAO.loadOrderBooks(dataPath);
        TraderDAO.loadTraders(dataPath);
        CrossingProcessor crossingProcessor = new CrossingProcessor(orderBooks);

        for(int i=0 ;i< 1_000_000; i++){
            crossingProcessor.processOrder(test.createNewOrder());
        }
    }
}
