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

    public long sendMultiMessageQuery(String zookeeperHosts) {
        int streamLength = 500000;
        int multiCount = 20;
        int batchSize = 40;

        StreamGenerator<Pair> generator
                = new MultiRandomCoordinateGenerator(0.9, getIstanbulRegionBox(), multiCount);

        MessageProducer mp = new MultiMessageProducer(producerTopic, zookeeperHosts, generator, streamLength,
                multiCount, batchSize);
        mp.startSendingWithKey();
        return System.currentTimeMillis();
    }

    public void sendMessagesMultiThreaded(String zookeeperHosts) {
        StreamGenerator<Pair> generator = new RandomCoordinateGenerator(0.9, getIstanbulRegionBox());

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        RecursiveAction recursiveAction = new MessageProducerRecursiveAction(producerTopic, zookeeperHosts,
                generator, 1000000, 10000, 100);

        forkJoinPool.invoke(recursiveAction);
    }

    public long sendSingleMessageQuery(String topic, String zookeeperHosts) {
        int streamLength = 1000;
        int batchSize = 50;

        StreamGenerator<Pair> generator
                = new RandomCoordinateGenerator(1.0, getIstanbulRegionBox());
        MessageProducer producer = new MessageProducer(producerTopic,
                zookeeperHosts, generator, streamLength, batchSize);

        producer.startSendingWithKey();
        return System.currentTimeMillis();
    }

    public void receiveResultsAsync(String zookeeperHosts) {
        Runnable messageConsumer = new MessageConsumer(consumerTopic, zookeeperHosts);
        new Thread(messageConsumer).start();
    }

    private static RegionBox getIstanbulRegionBox() {
        //Region to generate random coordinates - Istanbul
        double minLatitude = 40.780000;
        double maxLatitude = 41.339800;
        double minLongitude = 28.507700;
        double maxLongitude = 29.441900;

        return new RegionBox(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    public static void main(String[] args) {
        String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");
        String clientId = "1";
        String producerTopic = KAFKA_PRODUCER_TOPICS_PREFIX + clientId;
        String consumerTopic = MessageSenderFactory.KAFKA_CONSUMER_TOPICS_PREFIX + clientId;
        Query q = new Query(producerTopic, consumerTopic);
        long startTime = q.sendMultiMessageQuery(zookeeperHosts);
        System.out.println(startTime);
        q.receiveResultsAsync(zookeeperHosts);
    }
}
