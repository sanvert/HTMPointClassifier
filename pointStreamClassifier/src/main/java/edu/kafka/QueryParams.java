package edu.kafka;

public class QueryParams {
    private int streamLength = 1000000;
    private int multiCount = 10000;
    private int batchSize = 50;

    public QueryParams(final int streamLength, final int multiCount, final int batchSize) {
        this.streamLength = streamLength;
        this.multiCount = multiCount;
        this.batchSize = batchSize;
    }

    public int getStreamLength() {
        return streamLength;
    }

    public int getMultiCount() {
        return multiCount;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
