package edu.spark.htmLegacy;

import edu.jhu.htm.core.HTMfunc;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.Vector3d;
import edu.jhu.skiplist.SkipList;
import edu.kafka.ZooKeeperClientProxy;
import kafka.serializer.StringDecoder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import scala.Tuple2;
import sky.HtmRegions;
import sky.htm.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.util.GenericUtils.readArgumentsSilently;
import static edu.util.GenericUtils.readFromArgumentListSilently;
import static edu.util.GenericUtils.readIntegerArgumentSilently;

public class KafkaUnionPointClassificationStream {
    private static final Logger LOGGER = LogManager.getLogger("coordinate.stream");
    private static final String INV = "-1";

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(Level.WARN);
        String[] appConfig = readArgumentsSilently(args);
        String zookeeperHosts = readFromArgumentListSilently(appConfig, 0, "localhost:2181");

        int numOfStreams = readIntegerArgumentSilently(appConfig, 1);
        numOfStreams = numOfStreams == 0 ? 4 : numOfStreams;

        String groupId = readFromArgumentListSilently(appConfig, 2, "00");

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        List<String> topics = zooKeeperClientProxy.getKafkaTopics();

        // Create context with a 0.2 seconds batch interval
        SparkConf sparkConf = new SparkConf()
                .setAppName("KafkaStreamingTweetCoordinates")
                .set("spark.storage.memoryFraction", "0.5")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .registerKryoClasses(new Class[]{HTMrange.class, SkipList.class});

        JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);

        int htmDepth = 20;
        Converter converter = new Converter();
        HtmRegions regions = new HtmRegions();
        HTMrange htm = regions.generateConvexOverIstanbulRegion(converter, htmDepth);

        Broadcast<HTMrange> broadcastRange = javaSparkContext.broadcast(htm);
        Broadcast<Integer> broadcastDepth = javaSparkContext.broadcast(htmDepth);

        try(JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, Durations.milliseconds(200))) {
            jssc.sparkContext().setLogLevel("WARN");

            //Kafka consumer params
            Map<String, String> kafkaParams = new HashMap<>();
            kafkaParams.put("metadata.broker.list", zooKeeperClientProxy.getKafkaBrokerListAsString());
            kafkaParams.put("group.id", groupId);
            //Kafka topics to subscribe
            Map<String, Integer> topicsSet = zooKeeperClientProxy.getKafkaTopicAndPartitions();


            // Performance improvement - stream from multiple channels
            List<JavaPairDStream<String, String>> kafkaStreams = new ArrayList<>(numOfStreams);
            for (int i = 0; i < numOfStreams; i++) {
                kafkaStreams.add(KafkaUtils.createStream(
                        jssc,
                        String.class,
                        String.class,
                        StringDecoder.class,
                        StringDecoder.class,
                        kafkaParams,
                        topicsSet,
                        StorageLevel.MEMORY_ONLY()
                ));
            }
            JavaPairDStream<String, String> unifiedMessageStream = jssc.union(kafkaStreams.get(0),
                    kafkaStreams.subList(1, kafkaStreams.size()));

            //DEBUG
            //unifiedMessageStream.print();

            JavaDStream<String> coordinates = unifiedMessageStream.map(Tuple2::_2);
            JavaPairDStream<String, String> coordinatePair = coordinates
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
                        Vector3d pointIn = converter.convertLatLongToVector3D(Double.valueOf(pair._1), Double.valueOf(pair._2));
                        long lookupIdIn = HTMfunc.lookupId(pointIn.ra(), pointIn.dec(), broadcastDepth.value());
                        boolean isInside = broadcastRange.value().isIn(lookupIdIn);
                        return isInside ? "1-g" + groupId : "0-g" + groupId;
                    }).countByValue();

            sumCoordinates.print();

            // Start the computation
            jssc.start();
            jssc.awaitTermination();
        }
    }
}
