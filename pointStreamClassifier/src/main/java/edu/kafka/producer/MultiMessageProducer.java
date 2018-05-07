package edu.kafka.producer;

import edu.generator.StreamGenerator;

public class MultiMessageProducer extends MessageProducer {

    public MultiMessageProducer(final String zookeeperHosts, final StreamGenerator streamGenerator,
                                final int streamLength, final int multiCount, final int batchSize) {
        super(zookeeperHosts, streamGenerator, streamLength / multiCount, batchSize);
    }
}
