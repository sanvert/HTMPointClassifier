package edu.spark.htm;

import edu.jhu.htm.core.HTMrange;
import edu.jhu.skiplist.SkipList;
import edu.kafka.ZooKeeperClientProxy;
import edu.util.PropertyMapper;
import edu.util.RegionMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;
import skiplist.IntervalSkipList;
import sky.sphericalcurrent.ProcessRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaUnionPointClassificationStream {
    private static final Logger LOGGER = LogManager.getLogger("coordinate.stream");
    private static final String INV = "-1";
    private static final boolean DEBUG = true;
    private static final Duration DURATION = Durations.milliseconds(1000);

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(Level.WARN);
        String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");

        int numOfStreams = Integer.parseInt(PropertyMapper.defaults().get("spark.stream.count"));
        numOfStreams = numOfStreams == 0 ? 4 : numOfStreams;

        final String groupId = PropertyMapper.defaults().get("kafka.group.id");

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);

        // Create context with a 0.2 seconds batch interval
        SparkConf sparkConf = new SparkConf()
                .setAppName("KafkaStreamingTweetCoordinates")
                .set("spark.storage.memoryFraction", "0.5")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .registerKryoClasses(new Class[]{HTMrange.class, SkipList.class});

        if (DEBUG) {
            sparkConf.setMaster("spark://nl1lxl-108916.ttg.global:7077");
            //.set("spark.driver.bindAddress","127.0.0.1")
        }

        JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);

        final int htmDepth = 20;

        List<IntervalSkipList> intervalSkipLists = RegionMapper.convertIntoSkipLists("regionsHTM.json");

        //Broadcast<HTMrange> broadcastRange = javaSparkContext.broadcast(htm);

        try(JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, DURATION)) {
            jssc.sparkContext().setLogLevel("WARN");

            //Kafka consumer params
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", zooKeeperClientProxy.getKafkaBrokerListAsString());
            kafkaParams.put("group.id", groupId);
            //"org.apache.kafka.common.serialization.StringDeserializer"
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
                                        ConsumerStrategies.<String,String>Subscribe(topics, kafkaParams))
                                .map(record -> record.value())
                );
            }

            JavaDStream<String> coordinatesStream = jssc.union(kafkaStreams.get(0),
                    kafkaStreams.subList(1, kafkaStreams.size()));

            if (DEBUG) {
                //coordinatesStream.print();
                //coordinatesStream.count().print();
            }

            JavaPairDStream<String, String> coordinatePair = coordinatesStream
                    .mapToPair(coordinate -> {
                        String[] coordinateArray = coordinate.split(";");
                        String latitude = INV;
                        String longitude = INV;
                        if(coordinateArray.length == 2) {
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
                        for (IntervalSkipList skipList: intervalSkipLists) {
                            isInside = skipList.contains(hid);
                            if(isInside) {
                                break;
                            }
                        }
                        return isInside ? "1-g" + groupId : "0-g" + groupId;
                    }).countByValue();

            sumCoordinates.print();

            // Start the computation
            jssc.start();
            jssc.awaitTermination();
        }
    }
}
