package aeron;

import com.carrotsearch.hppc.ObjectArrayList;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.driver.Configuration;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.concurrent.IdleStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

public class AeronSubscriber {
    private int fragmentLimitCount;
    private AtomicBoolean running = new AtomicBoolean(true);
    private Aeron.Context ctx;
    private Aeron aeron;
    private ObjectArrayList<Subscription> subscriptions;
    private IdleStrategy idleStrategy;
    private FragmentHandler dataHandler;

    public AeronSubscriber(String contextDir, FragmentAssembler dataHandler){
        this.dataHandler = dataHandler;
        this.fragmentLimitCount = 1;
        this.subscriptions = new ObjectArrayList<>();
        initialize(contextDir);
    }

    public AeronSubscriber(String contextDir,FragmentHandler dataHandler){
        this.dataHandler = dataHandler;
        this.fragmentLimitCount = 1;
        this.subscriptions = new ObjectArrayList<>();
        initialize(contextDir);
    }

    public void addSubscriber(String channel,int streamId){
        subscriptions.add(aeron.addSubscription(channel, streamId));
    }

    private void printError(Throwable throwable){
        System.out.println(throwable.toString());
    }

    public void printAvailableImage(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.println(String.format(
                "Available image on %s streamId=%d sessionId=%d from %s",
                subscription.channel(), subscription.streamId(), image.sessionId(), image.sourceIdentity()));
    }

    public void printUnavailableImage(final Image image) {
        final Subscription subscription = image.subscription();
        System.out.println(String.format(
                "Unavailable image on %s streamId=%d sessionId=%d",
                subscription.channel(), subscription.streamId(), image.sessionId()));
    }

    private void initialize(String contextDir){
        ctx = new Aeron.Context();
        ctx.driverTimeoutMs(1000000);
        ctx.aeronDirectoryName(contextDir);
        ctx.errorHandler(this::printError);
        ctx.availableImageHandler(this::printAvailableImage);
        ctx.unavailableImageHandler(this::printUnavailableImage);

        aeron = Aeron.connect(ctx);
        idleStrategy = Configuration.agentIdleStrategy("org.agrona.concurrent.BackoffIdleStrategy", null);

    }

    public void start() {
        int fragmentsRead;
        int size = subscriptions.size();
        while (running.get()) {
            fragmentsRead = 0;
            for(int i=0; i<size; i++) {
                fragmentsRead += subscriptions.get(i).poll(dataHandler, fragmentLimitCount);
            }
            idleStrategy.idle(fragmentsRead);
        }
    }

    public void stop() {
        running.set(false);
        CloseHelper.close(aeron);

        int size = subscriptions.size();
        for(int i=0; i<size; i++) {
            CloseHelper.close(subscriptions.get(i));
        }
    }
}
