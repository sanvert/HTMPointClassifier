package edu.spark.htm;

import edu.kafka.ZooKeeperClientProxy;
import edu.spark.accumulator.MapAccumulator;
import edu.util.PropertyMapper;
import edu.util.RegionMapper;
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
import java.util.TimerTask;

public class KafkaUnionPointClassificationStream {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final Level LOG_LEVEL = Level.WARN;
    private static final String INV = "-1";
    private static final boolean DEBUG = true;
    private static final Duration BATCH_DURATION = Durations.milliseconds(1000);
    private static final boolean MULTI_COORDINATES_IN_REQUEST = false;

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(LOG_LEVEL);
        String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");

        int numOfStreams = Integer.parseInt(PropertyMapper.defaults().get("spark.stream.count"));
        numOfStreams = numOfStreams == 0 ? 4 : numOfStreams;

        final String groupId = PropertyMapper.defaults().get("kafka.group.id");

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);

        // Create context with a 0.2 seconds batch interval
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

        KafkaUnionPointClassificationStream.ReportTask reportTimer
                = new KafkaUnionPointClassificationStream.ReportTask(resultReport);
        new Timer().schedule(reportTimer, 1000, 5000);

        final int htmDepth = 20;

        final List<IntervalSkipList> intervalSkipLists = RegionMapper.convertIntoSkipLists("regionsHTM.json");

        try (JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, BATCH_DURATION)) {

            //Kafka consumer params
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", zooKeeperClientProxy.getKafkaBrokerListAsString());
            kafkaParams.put("group.id", groupId);
            kafkaParams.put("key.deserializer", StringDeserializer.class.getName());
            kafkaParams.put("value.deserializer", StringDeserializer.class.getName());

            //Kafka topics to subscribe
            List<String> topics = zooKeeperClientProxy.getKafkaTopics();

            // Performance improvement - stream from multiple channels
            List<JavaDStream<String>> kafkaStreams = new ArrayList<>(numOfStreams);
            for (int i = 0; i < numOfStreams; i++) {
                kafkaStreams.add(KafkaUtils.createDirectStream(
                        jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams))
                        .map(record -> record.value())
                );
            }

            JavaDStream<String> coordinatesStream = jssc.union(kafkaStreams.get(0),
                    kafkaStreams.subList(1, kafkaStreams.size()));

            if (MULTI_COORDINATES_IN_REQUEST) {
                coordinatesStream = coordinatesStream.flatMap(coordinates ->
                        Arrays.asList(coordinates.split(",")).iterator());
            }

            JavaPairDStream<String, String> coordinatePair = coordinatesStream
                    .mapToPair(coordinate -> {
                        String[] coordinateArray = coordinate.split(";");
                        String latitude = INV;
                        String longitude = INV;
                        if (coordinateArray.length == 2) {
                            latitude = coordinateArray[0];
                            longitude = coordinateArray[1];
                        } else {
                            System.err.println("stream ERROR");
                        }
                        return new Tuple2<>(latitude, longitude);
                    });

            JavaPairDStream<String, Long> sumCoordinates = coordinatePair
                    .map(pair -> {
                        long hid = ProcessRange.fHtmLatLon(Double.valueOf(pair._1), Double.valueOf(pair._2), htmDepth);
                        boolean isInside = false;
                        String id = null;
                        for (IntervalSkipList skipList : intervalSkipLists) {
                            isInside = skipList.contains(hid);
                            if (isInside) {
                                id = skipList.getId();
                                break;
                            }
                        }

                        System.out.println("worker log");
                        return isInside ? id + "-" + groupId : "-";
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

    private static final class ReportTask extends TimerTask {

        private final MapAccumulator mapAccumulator;

        public ReportTask(AccumulatorV2 accumulator) {
            mapAccumulator = (MapAccumulator) accumulator;
        }

        @Override
        public void run() {
            try {
                if (mapAccumulator.value().size() > 0) {
                    System.out.println("Accumulator Batch Start Time:"
                            + mapAccumulator.getLastBatchStartTime()
                            + ", Last Update Time: "
                            + mapAccumulator.getLastUpdateTime());
                    mapAccumulator.value().forEach((k, v) -> System.out.println(k + " " + v));
                }
            } catch (Exception e) {
            }
        }
    }
}
