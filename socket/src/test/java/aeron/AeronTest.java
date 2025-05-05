package aeron;


import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.SystemUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dharmeshsing on 7/08/15.
 */
public class AeronTest {

    private MediaDriver driver;

    @BeforeEach
    public void setup(){
        SystemUtil.loadPropertiesFile("mediaDriver.properties");
        driver = LowLatencyMediaDriver.startMediaDriver();
    }

    @AfterEach
    public void tearDown(){
        driver.close();
    }

    @Test
    public void testAeron() throws Exception{
        Thread pub = new Thread(()->{
            AeronPublisher aeronPublisher = new AeronPublisher(driver.aeronDirectoryName());
            aeronPublisher.addPublication("aeron:udp?endpoint=localhost:40123", 10);

            UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(512));
            String message = "Hello World!";
            buffer.putBytes(0, message.getBytes());

            while(true) {
                aeronPublisher.send(buffer);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread sub = new Thread(()->{

            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40123", 10);
            aeronSubscriber.start();
        });
        pub.start();
        sub.start();

        Thread.sleep(10000);

        pub.interrupt();
        sub.interrupt();

        driver.close();

    }

    @Test
    public void testMulticastAeron() throws Exception{

        Thread pub = new Thread(()->{
            AeronPublisher aeronPublisher = new AeronPublisher(driver.aeronDirectoryName());
            aeronPublisher.addPublication("aeron:udp?endpoint=localhost:40456", 10);
            UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(512));

            int index = 0;
            while(true) {
                buffer.byteBuffer().clear();
                String message = "Hello World! " + index++;
                buffer.putBytes(0, message.getBytes());

                aeronPublisher.send(buffer);
                System.out.println("Sent ");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread sub1 = new Thread(()->{
            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message sub1 " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40456", 10);
            aeronSubscriber.start();
        });

        Thread sub2 = new Thread(()->{
            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message sub2 " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40456", 10);
            aeronSubscriber.start();
        });

        pub.start();
        sub1.start();
        sub2.start();

        Thread.sleep(10000);

        pub.interrupt();
        sub1.interrupt();
        sub2.interrupt();

        driver.close();

    }

    @Test
    public void testMulticastAeronDifferentDrivers() throws Exception{
        MediaDriver driver1 = LowLatencyMediaDriver.startMediaDriver();

        Thread pub = new Thread(()->{
            AeronPublisher aeronPublisher = new AeronPublisher(driver1.aeronDirectoryName());
            aeronPublisher.addPublication("aeron:udp?endpoint=localhost:40456", 10);
            UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(512));

            int index = 0;
            while(true) {
                buffer.byteBuffer().clear();
                String message = "Hello World! " + index++;
                buffer.putBytes(0, message.getBytes());

                aeronPublisher.send(buffer);
                System.out.println("Sent ");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        MediaDriver driver2 = LowLatencyMediaDriver.startMediaDriver();

        Thread sub1 = new Thread(()->{
            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message sub1 " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver2.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40456", 10);
            aeronSubscriber.start();
        });

        MediaDriver driver3 = LowLatencyMediaDriver.startMediaDriver();

        Thread sub2 = new Thread(()->{
            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message sub2 " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver3.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40456", 10);
            aeronSubscriber.start();
        });

        pub.start();
        sub1.start();
        sub2.start();

        Thread.sleep(10000);

        pub.interrupt();
        sub1.interrupt();
        sub2.interrupt();

        driver1.close();
        driver2.close();
        driver3.close();
    }


    @Test
    public void testMulticastAeronLateSubscriber() throws Exception{
        Thread pub = new Thread(()->{
            AeronPublisher aeronPublisher = new AeronPublisher(driver.aeronDirectoryName());
            aeronPublisher.addPublication("aeron:udp?endpoint=localhost:40456", 10);
            UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(512));

            int index = 0;
            while(true) {
                buffer.byteBuffer().clear();
                String message = "Hello World! " + index++;
                buffer.putBytes(0, message.getBytes());

                aeronPublisher.send(buffer);
                System.out.println("Sent ");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread sub1 = new Thread(()->{
            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message sub1 " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40456", 10);
            aeronSubscriber.start();
        });
        pub.start();
        sub1.start();
        System.out.println("Subscriber 1 started");

        Thread.sleep(10000);

        Thread sub2 = new Thread(()->{
            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message sub2 " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40456", 10);
            aeronSubscriber.start();
        });


        sub2.start();
        System.out.println("Subscriber 2 started");

        Thread.sleep(10000);

        pub.interrupt();
        sub1.interrupt();
        sub2.interrupt();

        driver.close();

    }

//    @Disabled
    @Test
    public void testAeronMutiplePublishers() throws Exception{

        List<Thread> pubs = new ArrayList<>();
        for(int i=0; i<10; i++) {
            String message = "Hello World! " + i;
            String dir = "/tmp/pub/" + i;
            pubs.add(new Thread(() -> {
                LowLatencyMediaDriver.startMediaDriver(dir);
                AeronPublisher aeronPublisher = new AeronPublisher(dir);
                aeronPublisher.addPublication("aeron:udp?endpoint=localhost:40123", 10);

                UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(512));
                buffer.putBytes(0, message.getBytes());

                while (true) {
                    System.out.println("Sent message " + message);
                    aeronPublisher.send(buffer);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }

        Thread sub = new Thread(()->{
            MediaDriver driver = LowLatencyMediaDriver.startMediaDriver("/tmp/pub/sub");

            final FragmentHandler fragmentHandler =
                    (buffer, offset, length, header) ->
                    {
                        final byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        System.out.println("Received message " + new String(data));

                    };

            AeronSubscriber aeronSubscriber = new AeronSubscriber(driver.aeronDirectoryName(),fragmentHandler);
            aeronSubscriber.addSubscriber("aeron:udp?endpoint=localhost:40123", 10);
            aeronSubscriber.start();

            System.out.println("aeronSubscriber started");
        });

        sub.start();

//        Thread.sleep(100000);

        for(Thread pub : pubs){
            System.out.println("start pub");
            pub.start();
        }

        Thread.sleep(100000);

        for(Thread pub : pubs){
            pub.interrupt();
        }

        sub.interrupt();


    }

}