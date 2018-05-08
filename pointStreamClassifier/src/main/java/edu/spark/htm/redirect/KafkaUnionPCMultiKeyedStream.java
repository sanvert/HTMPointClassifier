package edu.spark.htm.redirect;

import edu.kafka.producer.MessageSenderWrapper;
import edu.kafka.zookeeper.ZooKeeperClientProxy;
import edu.kafka.producer.MessageSender;
import edu.spark.accumulator.MapAccumulator;
import edu.spark.report.ReportTask;
import edu.util.PropertyMapper;
import edu.util.RegionMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.util.AccumulatorV2;
import scala.Tuple2;
import skiplist.IntervalSkipList;
import sky.sphericalcurrent.ProcessRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

public class KafkaUnionPCMultiKeyedStream {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final Level LOG_LEVEL = Level.WARN;
    private static final boolean DEBUG = false;
    private static final Duration BATCH_DURATION
            = Durations.milliseconds(Long.valueOf(PropertyMapper.defaults().get("spark.kafka.direct.batch.duration")));
    private static final long REPORT_PERIOD = Long.valueOf(PropertyMapper.defaults().get("report.period"));
    private static final String KAFKA_TOPIC_GATHERING_TOPICS_PREFIX = "m";

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(LOG_LEVEL);

        final String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");
        final ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);

        int numOfStreams = Integer.parseInt(PropertyMapper.defaults().get("spark.stream.count"));
        numOfStreams = numOfStreams == 0 ? 4 : numOfStreams;

        final String groupId = PropertyMapper.defaults().get("kafka.group.id");


        // Create context with a specified batch interval
        SparkConf sparkConf = new SparkConf()
                .setAppName("KafkaStreamingTweetCoordinates")
                //.set("spark.storage.memoryFraction", "0.5") // Deprecated since 1.6
                .set("spark.serializer", KryoSerializer.class.getName())
                .registerKryoClasses(new Class[]{IntervalSkipList.class, IntervalSkipList.Node.class});

        if (DEBUG) {
            sparkConf.setMaster("spark://nl1lxl-108916.ttg.global:7077");
            //.set("spark.driver.bindAddress","127.0.0.1")
        }

        JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);
        javaSparkContext.setLogLevel(LOG_LEVEL.toString());

        final AccumulatorV2 resultReport = new MapAccumulator();
        javaSparkContext.sc().register(resultReport, "resultReport");

        final List<String> allTopics = zooKeeperClientProxy.getKafkaTopics();

        ReportTask reportTimer = new ReportTask(resultReport);
        new Timer().schedule(reportTimer, 1000, REPORT_PERIOD);

        final int htmDepth = 20;

        final List<IntervalSkipList> intervalSkipLists = RegionMapper.convertIntoSkipLists("regionsHTM.json");

        try (JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, BATCH_DURATION)) {

            //Kafka consumer params
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, zooKeeperClientProxy.getKafkaBrokerListAsString());
            kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            //Kafka topics to subscribe
            List<String> topics = allTopics.stream()
                    .filter(topic -> topic.startsWith(KAFKA_TOPIC_GATHERING_TOPICS_PREFIX))
                    .collect(Collectors.toList());

            // Performance improvement - stream from multiple channels
            List<JavaDStream<Tuple2<Integer, String>>> kafkaStreams = new ArrayList<>(numOfStreams);
            for (int i = 0; i < numOfStreams; i++) {
                kafkaStreams.add(KafkaUtils.createDirectStream(
                        jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.<Integer, String>Subscribe(topics, kafkaParams))
                        .map(record -> new Tuple2<>(record.key(), record.value()))
                );
            }

            JavaDStream<Tuple2<Integer, String>> coordinatesStream = jssc.union(kafkaStreams.get(0),
                    kafkaStreams.subList(1, kafkaStreams.size()));


            JavaPairDStream<Integer, String> coordinatePair = coordinatesStream
                    .mapToPair(record -> {
                        Integer key = record._1;
                        String[] coordinateArray = record._2.split(",");
                        StringBuilder resultBuilder = new StringBuilder();
                        for (String coordinate : coordinateArray) {
                            String[] pair = coordinate.split(";");
                            if (pair.length == 2) {
                                long hid = ProcessRange.fHtmLatLon(Double.valueOf(pair[0]), Double.valueOf(pair[1]),
                                        htmDepth);

                                String id = StringUtils.EMPTY;
                                for (IntervalSkipList skipList : intervalSkipLists) {
                                    if (skipList.contains(hid)) {
                                        id = skipList.getId();
                                        break;
                                    }
                                }
                                resultBuilder.append(id).append(",");
                            } else {
                                System.err.println("malformed coordinate:" + coordinate);
                                resultBuilder.append("-").append(",");
                            }
                        }

                        return new Tuple2<>(key, resultBuilder.toString());
                    });

            JavaPairDStream<String, Long> sumCoordinates = coordinatePair
                    .flatMap(pair -> {
                        MessageSenderWrapper.getInstance().send(pair._1, pair._2);
                        return Arrays.asList(pair._2.split(",")).iterator();
                    }).countByValue();

            if (DEBUG) {
                //sumCoordinates.print();
            }

            sumCoordinates.foreachRDD(rdd -> {
                resultReport.add(rdd.collectAsMap());
                if (resultReport.isZero()) {
                    System.out.println("NO RECORDS: " + System.currentTimeMillis());
                }
            });

            // Start the computation
            jssc.start();
            jssc.awaitTermination();
        }
    }
}
