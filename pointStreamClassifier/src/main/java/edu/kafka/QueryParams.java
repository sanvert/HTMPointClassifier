package edu.kafka;

public class QueryParams {
    private int streamLength;
    private int multiCount;
    private int batchSize;

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
