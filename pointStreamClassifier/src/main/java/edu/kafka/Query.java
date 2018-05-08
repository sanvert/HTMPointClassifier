package edu.kafka;

import com.sun.tools.javac.util.Pair;
import edu.generator.MultiRandomCoordinateGenerator;
import edu.generator.RandomCoordinateGenerator;
import edu.generator.StreamGenerator;
import edu.kafka.consumer.MessageConsumer;
import edu.kafka.producer.MessageProducer;
import edu.kafka.producer.MultiMessageProducer;
import edu.kafka.producer.RegionBox;
import edu.kafka.producer.parallelized.MessageProducerRecursiveAction;
import edu.util.PropertyMapper;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Query {

    public static long sendMultiMessageQuery(String zookeeperHosts) {
        int streamLength = 500000;
        int multiCount = 20;
        int batchSize = 40;

        StreamGenerator<Pair> generator
                = new MultiRandomCoordinateGenerator(0.9, getIstanbulRegionBox(), multiCount);

        MessageProducer mp = new MultiMessageProducer(zookeeperHosts, generator, streamLength,
                multiCount, batchSize);
        mp.startSendingWithKey();
        return System.currentTimeMillis();
    }

    public static void sendMessagesMultiThreaded(String zookeeperHosts) {
        StreamGenerator<Pair> generator = new RandomCoordinateGenerator(0.9, getIstanbulRegionBox());

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        RecursiveAction recursiveAction = new MessageProducerRecursiveAction(zookeeperHosts,
                generator, 1000000, 10000, 100);

        forkJoinPool.invoke(recursiveAction);
    }

    public static long sendSingleMessageQuery(String zookeeperHosts) {
        int streamLength = 1000;
        int batchSize = 50;

        StreamGenerator<Pair> generator
                = new RandomCoordinateGenerator(1.0, getIstanbulRegionBox());
        MessageProducer producer = new MessageProducer(zookeeperHosts, generator, streamLength, batchSize);

        producer.startSendingWithKey();
        return System.currentTimeMillis();
    }

    public static void receiveResultsAsync(String zookeeperHosts) {
        Runnable messageConsumer = new MessageConsumer(MessageConsumer.KAFKA_TOPIC_GATHERING_TOPICS_PREFIX,
                zookeeperHosts);
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
        long startTime = sendMultiMessageQuery(zookeeperHosts);
        System.out.println(startTime);
        receiveResultsAsync(zookeeperHosts);
    }
}
