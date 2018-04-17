package edu.kafka.producer.parallelized;

import com.sun.tools.javac.util.Pair;
import edu.generator.RandomCoordinateGenerator;
import edu.generator.StreamGenerator;
import edu.kafka.producer.MessageProducer;
import edu.util.PropertySetting;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MessageProducerRecursiveAction<T> extends RecursiveAction {

    private static final int SUBTASK_COUNT = 4;
    private StreamGenerator<T> streamGenerator;
    private int streamLength;
    private int threshold;
    private int batchSize;
    private String zookeeperHosts;

    private static Logger logger = Logger.getAnonymousLogger();

    public MessageProducerRecursiveAction(String zookeeperHosts, StreamGenerator<T> streamGenerator, int streamLength,
                                          int threshold, int batchSize) {
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

                    return new MessageProducerRecursiveAction(zookeeperHosts, streamGenerator, localSubStreamLength, threshold,
                            batchSize);
                })
                .collect(Collectors.toList());
    }

    private void process() {
        MessageProducer producer = new MessageProducer(zookeeperHosts, streamGenerator, streamLength, batchSize);
        producer.startSending();
        logger.info("The length of " + streamLength + " processed stream is done by "
                + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        //Region to generate random coordinates
        double minLatitude = 40.780000;
        double maxLatitude = 41.339800;
        double minLongitude = 28.507700;
        double maxLongitude = 29.441900;

        String zookeeperHosts = PropertySetting.defaults().get("zookeeper.host.list");

        StreamGenerator<Pair> generator = new RandomCoordinateGenerator(0.9, minLatitude, maxLatitude, minLongitude, maxLongitude);

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        RecursiveAction recursiveAction = new MessageProducerRecursiveAction(zookeeperHosts,
                generator, 1000000, 10000, 100);

        forkJoinPool.invoke(recursiveAction);
    }
}

