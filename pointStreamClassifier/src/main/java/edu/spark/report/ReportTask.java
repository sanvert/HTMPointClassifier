package edu.spark.report;

import edu.spark.accumulator.MapAccumulator;
import org.apache.spark.util.AccumulatorV2;

import java.util.TimerTask;

public final class ReportTask extends TimerTask {

    private final MapAccumulator mapAccumulator;

    public ReportTask(AccumulatorV2 accumulator) {
        mapAccumulator = (MapAccumulator) accumulator;
    }

    @Override
    public void run() {
        try {
            if (mapAccumulator.value().size() > 0) {
                System.out.println("Batch Start Time:"
                        + mapAccumulator.getLastBatchStartTime()
                        + ", Last Update Time: "
                        + mapAccumulator.getLastUpdateTime()
                        + ", Diff:" + (mapAccumulator.getLastUpdateTime() - mapAccumulator.getLastBatchStartTime()));
                mapAccumulator.value().forEach((k, v) -> System.out.println(k + " " + v));
            }
        } catch (Exception e) {
        }
    }
}