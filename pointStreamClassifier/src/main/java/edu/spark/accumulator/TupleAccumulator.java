package edu.spark.accumulator;

import org.apache.spark.util.AccumulatorV2;
import scala.Tuple2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TupleAccumulator extends AccumulatorV2<Tuple2<String, Long>, Map<String, Long>> {

    private volatile long lastBatchStartTimestamp;
    private volatile long previousUpdateTimestamp;
    private volatile long updateTimestamp;
    private Map<String, Long> initialValue;

    public TupleAccumulator() {
        reset();
    }

    public TupleAccumulator(Map<String, Long> map) {
        this();
        map.entrySet().forEach(entry -> add(Tuple2.apply(entry.getKey(), entry.getValue())));
    }

    @Override
    public boolean isZero() {
        return initialValue == null || initialValue.size() == 0;
    }

    @Override
    public AccumulatorV2<Tuple2<String, Long>, Map<String, Long>> copy() {
        return new TupleAccumulator(value());
    }

    @Override
    public void reset() {
        initialValue = new ConcurrentHashMap<>();
        updateTimestamp = System.currentTimeMillis();
        previousUpdateTimestamp = updateTimestamp;
        lastBatchStartTimestamp = updateTimestamp;
    }

    @Override
    public void add(Tuple2<String, Long> v) {
        if (v != null) {
            initialValue.merge(v._1, v._2, (v1, v2) -> v1 + v2);

            long currentTimestamp = System.currentTimeMillis();

            if (currentTimestamp - previousUpdateTimestamp > 35000) {
                lastBatchStartTimestamp = updateTimestamp;
            }

            previousUpdateTimestamp = updateTimestamp;
            updateTimestamp = currentTimestamp;
        }
    }

    @Override
    public void merge(AccumulatorV2<Tuple2<String, Long>, Map<String, Long>> other) {
        other.value().entrySet().forEach(entry -> add(Tuple2.apply(entry.getKey(), entry.getValue())));
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