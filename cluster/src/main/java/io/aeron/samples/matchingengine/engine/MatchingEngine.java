package io.aeron.samples.matchingengine.engine;

import aeron.AeronPublisher;
import aeron.AeronSubscriber;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.cursors.LongObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import io.aeron.logbuffer.Header;
import io.aeron.samples.infra.SessionMessageContext;
import io.aeron.samples.matchingengine.crossing.CrossingProcessor;
import io.aeron.samples.matchingengine.crossing.LOBManager;
import io.aeron.samples.matchingengine.crossing.MatchingContext;
import io.aeron.samples.matchingengine.crossing.tradingSessions.TradingSessionFactory;
import dao.OrderBookDAO;
import dao.TraderDAO;
import io.aeron.samples.matchingengine.data.ExecutionReportData;
import io.aeron.samples.matchingengine.data.HDRData;
import io.aeron.samples.matchingengine.data.MarketData;
import leafNode.OrderListImpl;
import orderBook.OrderBook;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.marketData.MessageHeaderDecoder;
import sbe.msg.marketData.TradingSessionEnum;
import sbe.msg.marketData.UnitHeaderDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class  MatchingEngine {

    protected Properties properties;
    private static final String PROPERTIES_FILE =  "MatchingEngine.properties";
    private AeronSubscriber subscriber;
    private AeronPublisher tradingGatewayPublisher;
    private AeronPublisher marketDataPublisher;
    private LOBManager lobManager;
    private UnsafeBuffer temp = new UnsafeBuffer(ByteBuffer.allocate(106));
    private LongObjectHashMap<OrderBook>  orderBooks;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private static long startTime;

    protected void loadProperties(String propertiesFile) throws IOException {
        try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {

            if (inputStream != null) {
                properties = new Properties();
                properties.load(inputStream);
            } else {
                throw new IOException("Unable to load properties file " + propertiesFile);
            }
        }
    }

    public static long getStartTime(){
        return startTime;
    }

    public static void setStartTime(long value){
        startTime = value;
    }

    public void initialize(){
        try {
            loadProperties(PROPERTIES_FILE);
            initTraders();
//            orderBooks = initOrderBooks();
            orderBooks = new LongObjectHashMap<>();
            initCrossingProcessor(orderBooks);
//            initGatewaySubscriber();
//            initTradingGatewayPublisher();
//            initMarketDataPublisher();
            initHDR();
            TradingSessionFactory.initTradingSessionProcessors(orderBooks);
            initOrderBookTradingSessions();
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            //TODO:Handle Exception
            e.printStackTrace();
        }
    }

    private void initHDR(){
        HDRData.INSTANCE.setDataPath(properties.getProperty("DATA_PATH"));
    }

    public boolean start() {
        System.out.println("Matching Engine Started");
        running.set(true);
//        subscriber.start();
        return true;
    }

    public static void setRunning(boolean value){
        running.set(value);
    }

/*    private void initGatewaySubscriber(){
        String mediaDriverConextDir = properties.getProperty("MEDIA_DRIVER_DIR");
        subscriber = new AeronSubscriber(mediaDriverConextDir,this);

        String url = properties.getProperty("SUB_GATEWAY_URL");
        int streamId = Integer.parseInt(properties.getProperty("SUB_GATEWAY_STREAM_ID"));

        subscriber.addSubscriber(url, streamId);
    }*/

/*    private void initTradingGatewayPublisher(){
        String mediaDriverConextDir = properties.getProperty("MEDIA_DRIVER_DIR");
        tradingGatewayPublisher = new AeronPublisher(mediaDriverConextDir);

        String url = properties.getProperty("PUB_TRADING_GATEWAY_URL");
        int streamId = Integer.parseInt(properties.getProperty("PUB_TRADING_GATEWAY_STREAM_ID"));

        tradingGatewayPublisher.addPublication(url, streamId);
    }*/

/*    private void initMarketDataPublisher(){
        String mediaDriverConextDir = properties.getProperty("MEDIA_DRIVER_DIR");
        marketDataPublisher = new AeronPublisher(mediaDriverConextDir);

        String url = properties.getProperty("PUB_MARKET_DATA_URL");
        int streamId = Integer.parseInt(properties.getProperty("PUB_MARKET_DATA_STREAM_ID"));

        marketDataPublisher.addPublication(url, streamId);
    }*/

    private LongObjectHashMap<OrderBook> initOrderBooks() throws IOException {
        String dataPath = properties.getProperty("DATA_PATH");
        return OrderBookDAO.loadOrderBooks(dataPath);
    }

    private void initTraders() throws IOException {
        String dataPath = properties.getProperty("DATA_PATH");
        TraderDAO.loadTraders(dataPath);
    }

    private void initCrossingProcessor(LongObjectHashMap<OrderBook> orderBooks){
        lobManager = new CrossingProcessor(orderBooks);
    }

    private void initOrderBookTradingSessions(){
        Iterator<LongObjectCursor<OrderBook>> iterator = orderBooks.iterator();
        while (iterator.hasNext()) {
            LongObjectCursor<OrderBook> orderBook = iterator.next();
            MatchingContext.INSTANCE.setOrderBookTradingSession(orderBook.value.getSecurityId(), TradingSessionEnum.ContinuousTrading);
        }
    }

    private void clearOrderBooks(){
        Iterator<LongObjectCursor<OrderBook>> iterator = orderBooks.iterator();
        while (iterator.hasNext())
        {
           iterator.next().value.freeAll();
        }
    }

    public boolean stop() {
        subscriber.stop();
        tradingGatewayPublisher.stop();
        marketDataPublisher.stop();
        clearOrderBooks();
        return true;
    }


    public void onFragment(DirectBuffer buffer, int offset, int length, Header header, SessionMessageContext context)
    {
        long startTime = System.nanoTime();

        try {
            temp.wrap(buffer, offset, length);

            final DirectBuffer report = lobManager.processOrder(temp);

            if (lobManager.isClientMarketDataRequest() || lobManager.isClientMarketDepthRequest())
            {
                publishClientMktData(context);
            }
            else if(lobManager.isAdminRequest())
            {
                publishReportToTradingGateway(report, context, lobManager.reportMessageLength());
            }
            else
            {
                publishReportToTradingGateway(report, context, lobManager.reportMessageLength());
                publishToMarketDataGateway(context);
            }

            HDRData.INSTANCE.updateHDR(startTime);
            HDRData.INSTANCE.storeHDRStats();

            if (running.get() == false)
            {
                stop();
            }
            System.out.println("Time taken to process order: " + (System.nanoTime() - startTime) + " ns" + ", l/i " + OrderListImpl.counter + "@" + OrderListImpl.OrderListIterator.counter);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void publishReportToTradingGateway(
        final DirectBuffer buffer,
        final SessionMessageContext context,
        final int length)
    {
        context.reply(buffer, 0 , length);
    }

    private void publishToMarketDataGateway(SessionMessageContext context)
    {
        DirectBuffer header = MarketData.INSTANCE.buildUnitHeader();

        unitHeaderDecoder.wrapAndApplyHeader(header, 0 , messageHeaderDecoder);
        context.reply(header, 0 , messageHeaderDecoder.encodedLength() + unitHeaderDecoder.encodedLength());

        ObjectArrayList<DirectBuffer> messages = MarketData.INSTANCE.getMktDataMessages();
        IntArrayList mktDataLength = MarketData.INSTANCE.getMktDataLength();

        for(ObjectCursor<DirectBuffer> cursor : messages)
        {
            context.reply(cursor.value, 0 , mktDataLength.get(cursor.index));
        }

        final DirectBuffer orderViewBuffer = ExecutionReportData.INSTANCE.getOrderView();

        if(orderViewBuffer != null)
        {
            int messageLength = ExecutionReportData.INSTANCE.getOrderViewMessageLength();
            context.reply(orderViewBuffer, 0 , messageLength);
        }
    }

    UnitHeaderDecoder unitHeaderDecoder = new UnitHeaderDecoder();
    MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();

    private void publishClientMktData(SessionMessageContext context)
    {
        DirectBuffer header = MarketData.INSTANCE.buildUnitHeader();

        unitHeaderDecoder.wrapAndApplyHeader(header, 0 , messageHeaderDecoder);
        context.reply(header, 0 , messageHeaderDecoder.encodedLength() + unitHeaderDecoder.encodedLength());

        if(MarketData.INSTANCE.isSnapShotRequest())
        {
            MarketData.INSTANCE.lobSnapShot(context);
        }

        if(MarketData.INSTANCE.isMarketDepthRequest())
        {
            MarketData.INSTANCE.calcMarketDepth(context);
        }

        final ObjectArrayList<DirectBuffer> messages = MarketData.INSTANCE.getMktDataMessages();
        final IntArrayList mktDataLength = MarketData.INSTANCE.getMktDataLength();

        for(final ObjectCursor<DirectBuffer> cursor : messages)
        {
            context.reply(cursor.value, 0 , mktDataLength.get(cursor.index));
        }
    }

    public static void main(String[] args){
        MatchingEngine matchingEngine = new MatchingEngine();
        matchingEngine.initialize();
        matchingEngine.start();
    }
}
