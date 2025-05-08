package io.aeron.samples.matchingengine.data;

import io.aeron.samples.matchingengine.engine.MatchingEngine;
import io.aeron.shadow.org.HdrHistogram.Histogram;

import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public enum HDRData {
    INSTANCE;
    //private static Histogram histogram = new Histogram(3600000000000L, 3); //1 hour
    private static Histogram histogram = new Histogram(TimeUnit.HOURS.toNanos(15), 3);
    private String dataPath;
    private int requests;

    public void setDataPath(String dataPath){
        this.dataPath = dataPath;
    }
    public void reset(){
        histogram.reset();
        requests = 0;
        MatchingEngine.setStartTime(System.currentTimeMillis());
    }

    public void updateHDR(long startTime){
        requests++;
        if(startTime == 0){
            startTime = System.nanoTime();
        }
        histogram.recordValue(System.nanoTime() - startTime);
    }

    public void storeHDRStats() {

        if (requests != 1_000 && requests != 5_000 && requests != 40_000 && requests != 100_000 && requests != 130_000) {
            return;
        }
        File hdrLatency = new File(dataPath + File.separator + "hdrLatency-"+ requests + ".txt");
        if (hdrLatency.exists()) {
            hdrLatency.delete();
        }

        try (PrintStream out = new PrintStream(hdrLatency)) {
            histogram.outputPercentileDistribution(out, 1000.0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Requests count is " + requests);
        System.out.println("Start time is " + MatchingEngine.getStartTime());
        System.out.println("End time is " + System.currentTimeMillis());
        System.out.println("Duration is" + (System.currentTimeMillis() - MatchingEngine.getStartTime()));
    }
}
