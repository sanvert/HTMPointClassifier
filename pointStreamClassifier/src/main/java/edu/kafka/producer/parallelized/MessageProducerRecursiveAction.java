package edu.kafka.producer.parallelized;

import edu.generator.StreamGenerator;
import edu.kafka.producer.MessageProducer;

import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MessageProducerRecursiveAction<T> extends RecursiveAction {

    private static final int SUBTASK_COUNT = 4;
    private String producerTopic;
    private String zookeeperHosts;
    private StreamGenerator<T> streamGenerator;
    private int streamLength;
    private int threshold;
    private int batchSize;

    private static Logger logger = Logger.getAnonymousLogger();

    public MessageProducerRecursiveAction(String producerTopic, String zookeeperHosts,
                                          StreamGenerator<T> streamGenerator, int streamLength,
                                          int threshold, int batchSize) {
        this.producerTopic = producerTopic;
        this.streamGenerator = streamGenerator;
        this.streamLength = streamLength;
        this.threshold = threshold;
        this.batchSize = batchSize;
        this.zookeeperHosts = zookeeperHosts;
    }

    @Override
    protected void compute() {
        if (streamLength > threshold) {
            ForkJoinTask.invokeAll(createSubTasks());
        } else {
            process();
        }
    }

    private List<MessageProducerRecursiveAction> createSubTasks() {
        final int subStreamLength = streamLength / SUBTASK_COUNT;
        final int subStreamLengthRemainder = streamLength % SUBTASK_COUNT;

        return IntStream.range(1, SUBTASK_COUNT)
                .mapToObj(idx -> {
                    int localSubStreamLength = subStreamLength;
                    if(idx==SUBTASK_COUNT)
                        localSubStreamLength += subStreamLengthRemainder;

                    return new MessageProducerRecursiveAction(producerTopic, zookeeperHosts, streamGenerator,
                            localSubStreamLength, threshold, batchSize);
                })
                .collect(Collectors.toList());
    }

    private void process() {
        MessageProducer producer = new MessageProducer(producerTopic, zookeeperHosts, streamGenerator, streamLength,
                batchSize);
        producer.startSending();
        logger.info("The length of " + streamLength + " processed stream is done by "
                + Thread.currentThread().getName());
    }
}

