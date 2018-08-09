package edu.spark.htm.redirect;

import com.vividsolutions.jts.geom.Coordinate;
import edu.kafka.producer.MessageSenderFactory;
import edu.kafka.zookeeper.ZooKeeperClientProxy;
import edu.spark.accumulator.MapAccumulator;
import edu.spark.report.ReportTask;
import edu.util.ArgumentUtils;
import edu.util.PropertyMapper;
import edu.util.RTreeIndex;
import edu.util.RegionHTMIndex;
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
import scala.Tuple3;
import skiplist.region.Node;
import skiplist.region.RegionAwareIntervalSkipList;
import sky.sphericalcurrent.ProcessRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.stream.Collectors;

public class KafkaUnionPCMultiKeyedStream {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final Level LOG_LEVEL = Level.WARN;
    private static final boolean DEBUG = false;
    private static final Duration BATCH_DURATION
            = Durations.milliseconds(Long.valueOf(PropertyMapper.readDefaultProps().get("spark.kafka.direct.batch.duration")));
    private static final long REPORT_PERIOD = Long.valueOf(PropertyMapper.readDefaultProps().get("report.period"));
    private static final String KAFKA_PRODUCER_TOPICS_PREFIX = "p-";
    private static final String REGIONS_JSON ="regionsHTM.json";

    private static String MASTER_ADDRESS;

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(LOG_LEVEL);

        final String zookeeperHosts = ArgumentUtils.readCLIArgumentSilently(args, 0,
                PropertyMapper.readDefaultProps().get("zookeeper.host.list"));
        MASTER_ADDRESS = ArgumentUtils.readCLIArgumentSilently(args, 1,
                PropertyMapper.readDefaultProps().get("spark.default.master.address"));
        final ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);

        int numOfStreams = Integer.parseInt(PropertyMapper.readDefaultProps().get("spark.stream.count"));
        numOfStreams = numOfStreams == 0 ? 4 : numOfStreams;

        final String groupId = PropertyMapper.readDefaultProps().get("kafka.group.id");

        // Create context with a specified batch interval
        SparkConf sparkConf = new SparkConf()
                .setAppName("KafkaStreamingTweetCoordinates")
                //.set("spark.storage.memoryFraction", "0.5") // Deprecated since 1.6
                .set("spark.serializer", KryoSerializer.class.getName())
                .registerKryoClasses(new Class[]{RegionHTMIndex.class,
                                                    RTreeIndex.class,
                                                    RegionAwareIntervalSkipList.class,
                                                    Node.class});

        if (DEBUG) {
            sparkConf.setMaster(MASTER_ADDRESS);
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

        System.out.println("INDEX GENERATION STARTED, time" + System.currentTimeMillis());
        RegionHTMIndex regionHTMIndex = new RegionHTMIndex(REGIONS_JSON);
        RTreeIndex rTreeIndex = new RTreeIndex(regionHTMIndex);
        System.out.println("INDEX GENERATION COMPLETED, time" + System.currentTimeMillis());

        try (JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, BATCH_DURATION)) {

            //Kafka consumer params
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, zooKeeperClientProxy.getKafkaBrokerListAsString());
            kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            //Kafka topics to subscribe
            List<String> topics = allTopics.stream()
                    .filter(topic -> topic.startsWith(KAFKA_PRODUCER_TOPICS_PREFIX))
                    .collect(Collectors.toList());

            // Performance improvement - stream from multiple channels
            List<JavaDStream<Tuple3<String, Integer, String>>> kafkaStreams = new ArrayList<>(numOfStreams);
            for (int i = 0; i < numOfStreams; i++) {

                kafkaStreams.add(KafkaUtils.createDirectStream(
                        jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.<Integer, String>Subscribe(topics, kafkaParams))
                        .map(record -> new Tuple3<>(record.topic(), record.key(), record.value()))
                );

            }

            JavaDStream<Tuple3<String, Integer, String>> coordinatesStream = jssc.union(kafkaStreams.get(0),
                    kafkaStreams.subList(1, kafkaStreams.size()));


            JavaPairDStream<Integer, String> coordinatePair = coordinatesStream
                    .mapToPair(record -> {
                        String topic = record._1();
                        Integer key = record._2();
                        String[] coordinateArray = record._3().split(",");
                        StringBuilder resultBuilder = new StringBuilder();
                        for (String coordinate : coordinateArray) {
                            String[] pair = coordinate.split(";");
                            if (pair.length == 2) {

                                double latitude = Double.valueOf(pair[0]);
                                double longitude = Double.valueOf(pair[1]);

                                long hid = ProcessRange.fHtmLatLon(latitude, longitude, htmDepth);
                                Set<Integer> regionIdSet = regionHTMIndex.getResidingRegionIdSet(hid);

                                String id = StringUtils.EMPTY;
                                if(regionIdSet.size() == 1) {
                                    id = String.valueOf(regionIdSet.iterator().next());
                                } else if (regionIdSet.size() > 1) {
                                    Coordinate c = new Coordinate(longitude, latitude);
                                    Optional<Integer> regionId = rTreeIndex.getResidingRegionId(regionIdSet, c);
                                    if(regionId.isPresent()) {
                                        id = String.valueOf(regionId.get());
                                    }
                                }

                                resultBuilder.append(id).append(",");
                            } else {
                                System.err.println("malformed coordinate:" + coordinate);
                                resultBuilder.append("-").append(",");
                            }
                        }

                        MessageSenderFactory.getSender(topic.split("-")[1], zookeeperHosts)
                                .send(key, resultBuilder.toString());

                        return new Tuple2<>(key, resultBuilder.toString());
                    });

            JavaPairDStream<String, Long> sumCoordinates = coordinatePair
                    .flatMap(pair -> {
                        //MessageSenderWrapper.getInstance().send(pair._1, pair._2);
                        return Arrays.asList(pair._2.split(",")).iterator();
                    }).countByValue();

            if (DEBUG) {
                //sumCoordinates.print();
            }

            sumCoordinates.foreachRDD(rdd -> {
                resultReport.add(rdd.collectAsMap());
                if (resultReport.isZero()) {
                    System.out.println("NO PROCESSED RECORDS, Time in ms: " + System.currentTimeMillis());
                }
            });

            // Start the computation
            jssc.start();
            jssc.awaitTermination();
        }
    }
}
