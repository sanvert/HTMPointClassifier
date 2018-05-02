package edu.spark.accumulator;

import org.apache.spark.util.AccumulatorV2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MapAccumulator extends AccumulatorV2<Map<String, Long>, Map<String, Long>> {

    private volatile long lastBatchStartTimestamp;
    private volatile long previousUpdateTimestamp;
    private volatile long updateTimestamp;
    private Map<String, Long> initialValue;

    public MapAccumulator() {
        initialValue = new ConcurrentHashMap<>();
    }

    public MapAccumulator(Map<String, Long> map) {
        this();
        add(map);
    }

    @Override
    public boolean isZero() {
        return initialValue == null || initialValue.size() == 0;
    }

    @Override
    public AccumulatorV2<Map<String, Long>, Map<String, Long>> copy() {
        return new MapAccumulator(value());
    }

    @Override
    public void reset() {
        initialValue = new ConcurrentHashMap<>();
        updateTimestamp = System.currentTimeMillis();
        previousUpdateTimestamp = updateTimestamp;
        lastBatchStartTimestamp = updateTimestamp;
    }

    @Override
    public void add(Map<String, Long> v) {
        if (v.size() > 0) {
            v.forEach((key, value) -> initialValue.merge(key, value, (v1, v2) -> v1 + v2));

            long currentTimestamp = System.currentTimeMillis();

            if (currentTimestamp - previousUpdateTimestamp > 35000) {
                lastBatchStartTimestamp = updateTimestamp;
            }

            previousUpdateTimestamp = updateTimestamp;
            updateTimestamp = currentTimestamp;


        }
    }

    @Override
    public void merge(AccumulatorV2<Map<String, Long>, Map<String, Long>> other) {
        add(other.value());
    }

    @Override
    public Map<String, Long> value() {
        return initialValue;
    }


    public long getLastBatchStartTime() {
        return lastBatchStartTimestamp;
    }

    public long getLastUpdateTime() {
        return updateTimestamp;
    }
}