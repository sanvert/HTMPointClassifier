package edu.kafka;

import com.sun.tools.javac.util.Pair;
import edu.generator.MultiRandomCoordinateGenerator;
import edu.generator.RandomCoordinateGenerator;
import edu.generator.StreamGenerator;
import edu.kafka.consumer.MessageConsumer;
import edu.kafka.producer.MessageProducer;
import edu.kafka.producer.MessageSenderFactory;
import edu.kafka.producer.MultiMessageProducer;
import edu.kafka.producer.RegionBox;
import edu.kafka.producer.parallelized.MessageProducerRecursiveAction;
import edu.util.ArgumentUtils;
import edu.util.PropertyMapper;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Query {

    private static final String KAFKA_PRODUCER_TOPICS_PREFIX = "p-";

    private final String producerTopic;
    private final String consumerTopic;

    public Query(final String producerTopic, final String consumerTopic) {
        this.producerTopic = producerTopic;
        this.consumerTopic = consumerTopic;
    }

    public long sendMultiMessageQuery(String zookeeperHosts, QueryParams params) {
        StreamGenerator<Pair> generator
                = new MultiRandomCoordinateGenerator(0.9, getBBAroundIstanbulRegion(),
                                                    params.getMultiCount());

        MessageProducer mp = new MultiMessageProducer(producerTopic, zookeeperHosts, generator, params.getStreamLength(),
                params.getMultiCount(), params.getBatchSize());
        mp.startSendingWithKey();
        return System.currentTimeMillis();
    }

    public long sendMessagesMultiThreaded(String zookeeperHosts, QueryParams params) {
        StreamGenerator<Pair> generator = new RandomCoordinateGenerator(0.9, getBBAroundIstanbulRegion());

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        RecursiveAction recursiveAction = new MessageProducerRecursiveAction(producerTopic, zookeeperHosts,
                generator, params.getStreamLength(), params.getMultiCount(), params.getBatchSize());

        forkJoinPool.invoke(recursiveAction);
        return System.currentTimeMillis();
    }

    public long sendSingleMessageQuery(String zookeeperHosts, QueryParams params) {
        StreamGenerator<Pair> generator
                = new RandomCoordinateGenerator(1.0, getBBAroundIstanbulRegion());
        MessageProducer producer = new MessageProducer(producerTopic,
                zookeeperHosts, generator, params.getStreamLength(), params.getBatchSize());

        producer.startSendingWithKey();
        return System.currentTimeMillis();
    }

    public void receiveResultsAsync(String zookeeperHosts) {
        Runnable messageConsumer = new MessageConsumer(consumerTopic, zookeeperHosts);
        new Thread(messageConsumer).start();

        Runnable messageConsumer2 = new MessageConsumer(consumerTopic, zookeeperHosts);
        new Thread(messageConsumer2).start();
    }

    private static RegionBox getBBAroundIstanbulRegion() {
        //Region to generate random coordinates - Istanbul
        double minLatitude = 40.780000;
        double maxLatitude = 41.339800;
        double minLongitude = 28.507700;
        double maxLongitude = 29.441900;

        return new RegionBox(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    public static void main(String[] args) {
        String zookeeperHosts = PropertyMapper.readDefaultProps().get("zookeeper.host.list");
        String clientId = "1";
        String producerTopic = KAFKA_PRODUCER_TOPICS_PREFIX + clientId;
        String consumerTopic = MessageSenderFactory.KAFKA_CONSUMER_TOPICS_PREFIX + clientId;
        Query q = new Query(producerTopic, consumerTopic);
        q.receiveResultsAsync(zookeeperHosts);

        int streamLength = Integer.parseInt(ArgumentUtils.readArgumentSilently(args, 0, "1000000"));
        int multiCount = Integer.parseInt(ArgumentUtils.readArgumentSilently(args, 1, "25000"));
        int batchSize = Integer.parseInt(ArgumentUtils.readArgumentSilently(args, 2, "2048"));
        QueryParams params = new QueryParams(streamLength, multiCount, batchSize);
        long startTime = q.sendMultiMessageQuery(zookeeperHosts, params);

        System.out.println("QUERY SENT - " + startTime + "L");
    }
}
