package io.aeron.samples.matchingengine.engine;

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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class  MatchingEngine {

    private LOBManager lobManager;
    private UnsafeBuffer temp = new UnsafeBuffer(ByteBuffer.allocate(106));
    private LongObjectHashMap<OrderBook>  orderBooks;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private static long startTime;


    public static long getStartTime(){
        return startTime;
    }

    public static void setStartTime(long value){
        startTime = value;
    }

    public void initialize()
    {
        try
        {
            orderBooks = new LongObjectHashMap<>();
            initCrossingProcessor(orderBooks);
            initHDR();
            TradingSessionFactory.initTradingSessionProcessors(orderBooks);
            initOrderBookTradingSessions();

            startTime = System.currentTimeMillis();
        }
        catch (final Exception e)
        {
            //TODO:Handle Exception
            e.printStackTrace();
        }
    }

    private void initHDR()
    {
        String dir = System.getenv("HDR_DIR");

        if (null == dir || dir.isEmpty())
        {
            dir = System.getProperty("user.dir");
        }
        HDRData.INSTANCE.setDir(dir);
    }

    public boolean start()
    {
        System.out.println("Matching Engine Started");
        running.set(true);
        return true;
    }

    public static void setRunning(boolean value){
        running.set(value);
    }

    private void initCrossingProcessor(final LongObjectHashMap<OrderBook> orderBooks)
    {
        lobManager = new CrossingProcessor(orderBooks);
    }

    private void initOrderBookTradingSessions()
    {
        final Iterator<LongObjectCursor<OrderBook>> iterator = orderBooks.iterator();
        while (iterator.hasNext())
        {
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

    public boolean stop()
    {
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
        } catch (final Exception e)
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

    private void publishToMarketDataGateway(final SessionMessageContext context)
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

    private void publishClientMktData(final SessionMessageContext context)
    {
        final DirectBuffer header = MarketData.INSTANCE.buildUnitHeader();

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
}
