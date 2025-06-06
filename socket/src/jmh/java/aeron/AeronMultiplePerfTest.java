package aeron;

import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;

/**
 * Created by dharmeshsing on 1/09/15.
 */
@State(Scope.Thread)
public class AeronMultiplePerfTest {
    private MediaDriver driver;
    private AeronPublisher aeronPublisher;
    private UnsafeBuffer buffer;


    @Setup(Level.Trial)
    public void setup(){
        driver = MediaDriver.launchEmbedded();
        aeronPublisher = new AeronPublisher(driver.aeronDirectoryName());
        int index = 40123;
        for(int i=0; i<10; i++) {
            aeronPublisher.addPublication("aeron:udp?endpoint=localhost:" + index++, 10);
        }

        buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(12));
        String message = "Hello World!";
        buffer.putBytes(0, message.getBytes());
    }

    @TearDown
    public void tearDown(){
        aeronPublisher.stop();
        driver.close();
    }

    @Benchmark
    public long testMutiplePublishers() throws Exception {
        aeronPublisher.send(buffer);
        return 1L;
    }

}