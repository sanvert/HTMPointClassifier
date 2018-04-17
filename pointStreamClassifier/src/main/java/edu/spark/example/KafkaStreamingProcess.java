package main.java.edu.spark.example;

import edu.kafka.ZooKeeperClientProxy;
import kafka.cluster.Broker;
import kafka.serializer.StringDecoder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import scala.Tuple2;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 * argument 1 - zookeeper address
 * argument 2 - num of streams
 *
 */
public class KafkaStreamingProcess {
    private static final Logger LOGGER = LogManager.getLogger("coordinate.stream");

    private static String[] readArgumentsSilently(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.contains("app"))
                .map(arg -> arg.replaceFirst("app=", "").split(";"))
                .findFirst()
                .orElse(new String[]{});
    }

    private static String readFromArgumentListSilently(String[] arr, int idx) {
        try {
            return arr[idx];
        } catch(ArrayIndexOutOfBoundsException e) {

        }
        return null;
    }

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(Level.WARN);
        String[] appConfig = readArgumentsSilently(args);
        String zookeeperHosts = readFromArgumentListSilently(appConfig, 0);
        //"bb-system-0652.home:2181"; // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181"
        zookeeperHosts = zookeeperHosts == null ? "localhost:2181" : zookeeperHosts;

        String numOfStreamsStr = readFromArgumentListSilently(appConfig, 1);
        int numOfStreams = numOfStreamsStr == null ? 4 : Integer.parseInt(numOfStreamsStr);

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        List<String> topics = zooKeeperClientProxy.getKafkaTopics();
        topics.stream().forEach(LOGGER::info);

        List<Broker> brokers = zooKeeperClientProxy.getKafkaBrokers();
        brokers.stream().forEach(LOGGER::info);

        // Create context with a 0.05 seconds batch interval
        SparkConf sparkConf = new SparkConf()
                .setAppName("KafkaStreamingTweetCoordinates")
                .set("spark.storage.memoryFraction", "0.5");
                //.set("spark.eventLog.enabled", "true");

//        SparkSession sparkSession = SparkSession
//                .builder()
//                .config(sparkConf)
//                .getOrCreate();

        JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);

        DecimalFormat df = new DecimalFormat("#.00");
        Broadcast<DecimalFormat> broadcast = javaSparkContext.broadcast(df);

        try(JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, Durations.milliseconds(1000))) {
            jssc.sparkContext().setLogLevel("WARN");
            Map<String, String> kafkaParams = new HashMap<>();
            kafkaParams.put("metadata.broker.list", zooKeeperClientProxy.getKafkaBrokerListAsString());
            Set<String> topicsSet = new HashSet<>(topics);

            // Performance improvement - stream from multiple channels
            List<JavaPairDStream<String, String>> kafkaStreams = new ArrayList<>(numOfStreams);
            for (int i = 0; i < numOfStreams; i++) {
                // Create direct kafka stream with brokers and topics
                kafkaStreams.add(KafkaUtils.createDirectStream(
                        jssc,
                        String.class,
                        String.class,
                        StringDecoder.class,
                        StringDecoder.class,
                        kafkaParams,
                        topicsSet
                ));
            }
            JavaPairDStream<String, String> unifiedMessageStream = jssc.union(kafkaStreams.get(0),
                    kafkaStreams.subList(1, kafkaStreams.size()));
            //DEBUG purpose
            //unifiedMessageStream.print();
            JavaDStream<String> coordinates = unifiedMessageStream.map(Tuple2::_2);
            JavaPairDStream<String, String> coordinatePair = coordinates
                    .mapToPair(coordinate -> {
                        String[] coordinateArray = coordinate.split(";");
                        String latitude = "-999";
                        String longitude = "-999";
                        if(coordinateArray.length == 2) {
                            latitude = coordinateArray[0];
                            longitude = coordinateArray[1];
                            LOGGER.error(latitude + ";" + longitude);
                            System.out.println(latitude + ";" + longitude);
                        } else {
                            LOGGER.error("ERROR in kafka stream reading operation.");
                            //System.out.println("ERROR reading stream");
                        }
                        return new Tuple2<>(latitude, longitude);
                    });

            //coordinatePair.print();

            JavaPairDStream<Double, Long> sumCoordinates = coordinatePair
                    .map(pair -> Double.valueOf(broadcast.value().format(Double.valueOf(pair._1)))
                            + Double.valueOf(broadcast.value().format(Double.valueOf(pair._2))))
                    .countByValue();

            sumCoordinates.print();

            // Start the computation
            jssc.start();
            jssc.awaitTermination();
        }
    }
}
