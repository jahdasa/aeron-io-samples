package aeron;


import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.SystemUtil;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.SigIntBarrier;

public class LowLatencyMediaDriver {
    public static void main(final String[] args) throws Exception {
        SystemUtil.loadPropertiesFiles("mediaDriver.properties");

        MediaDriver.Context ctx = new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
//                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
//                .receiverIdleStrategy(new BusySpinIdleStrategy())
//                .senderIdleStrategy(new BusySpinIdleStrategy())
                .aeronDirectoryName(args[0]);

        ctx.driverTimeoutMs(1000000);

        try (final MediaDriver ignored = MediaDriver.launch(ctx)) {
            new SigIntBarrier().await();
            System.out.println("Shutdown Driver...");
        }
    }

    public static MediaDriver.Context getLowLatencyMediaDriver(String dirName){
        MediaDriver.Context ctx = new MediaDriver.Context()
                .threadingMode(ThreadingMode.DEDICATED)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new BusySpinIdleStrategy())
                .senderIdleStrategy(new BusySpinIdleStrategy())
                .aeronDirectoryName(dirName);

        ctx.driverTimeoutMs(1000000);

        return ctx;

    }

    public static MediaDriver startMediaDriver(String dirName){
        MediaDriver.Context ctx = new MediaDriver.Context()
                .threadingMode(ThreadingMode.DEDICATED)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new BusySpinIdleStrategy())
                .senderIdleStrategy(new BusySpinIdleStrategy())
                .aeronDirectoryName(dirName);

        ctx.driverTimeoutMs(1000000);

       return MediaDriver.launchEmbedded(ctx);
    }

    public static MediaDriver startMediaDriver(){
        SystemUtil.loadPropertiesFiles("mediaDriver.properties");

        MediaDriver.Context ctx = new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED);
//                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
//                .receiverIdleStrategy(new BusySpinIdleStrategy())
//                .senderIdleStrategy(new BusySpinIdleStrategy());

        ctx.driverTimeoutMs(1000000);

        return MediaDriver.launchEmbedded(ctx);
    }
}
