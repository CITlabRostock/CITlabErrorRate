package de.uros.citlab.errorrate.types;

import javafx.scene.paint.Stop;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StopWatch {

    private static int count = 0;
    private long sum = 0;
    private long actual = 0;
    private long start = 0;
    private long cnt;
    private final String name;
    private static HashMap<String, StopWatch> staticWatches = new HashMap<>();

    public StopWatch() {
        this("StopWatch " + count++);
    }

    public StopWatch(String name) {
        this.name = name;
    }

    public static void start(String key) {
        StopWatch stopWatch = staticWatches.get(key);
        if (stopWatch == null) {
            stopWatch = new StopWatch(key);
            staticWatches.put(key, stopWatch);
        }
        stopWatch.start();
    }
    public static void stop(String key) {
        staticWatches.get(key).stop();
    }
    public static String getStats(){
        StringBuilder sb = new StringBuilder();
        for(StopWatch sw : staticWatches.values()){
            sb.append(sw.toString()).append('\n');
        }
        return sb.toString();
    }

    public void start() {
        start = System.nanoTime();
    }

    public void stop() {
        long end = System.nanoTime();
        actual = end - start;
        cnt++;
        sum += actual;
    }

    public long getSum() {
        return sum;
    }

    public long getActual() {
        return actual;
    }

    @Override
    public String toString() {
        return String.format("%10s: sum = %.4f ms avg = %.4f ms last = %.4f ms count = %d", name, ((double) sum) / 1000000, ((double) sum) / cnt / 1000000, ((double) actual) / 1000000, cnt);
    }
}
