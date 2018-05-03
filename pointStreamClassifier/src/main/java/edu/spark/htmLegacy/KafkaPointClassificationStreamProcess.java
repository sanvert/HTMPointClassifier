package edu.spark.htmLegacy;

import edu.jhu.htm.core.HTMfunc;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.Vector3d;
import edu.jhu.skiplist.SkipList;
import edu.kafka.ZooKeeperClientProxy;
import edu.spark.accumulator.MapAccumulator;
import edu.spark.report.ReportTask;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.util.AccumulatorV2;
import scala.Tuple2;
import sky.HtmRegions;
import sky.htm.Converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static edu.util.ArgumentUtils.readArgumentsSilently;
import static edu.util.ArgumentUtils.readFromArgumentListSilently;

public class KafkaPointClassificationStreamProcess {

    private static final boolean ENABLE_STATE_PERSISTENCE = false;
    private static final int STREAMING_DURATION = 300;
    private static final String INV = "-1";
    private static final Logger LOGGER = LogManager.getLogger("coordinate.stream");

    private static Function2<List<Long>, Optional<Long>, Optional<Long>> UPDATE_CUMULATIVE_RESULTS_STATE
            = (numList, current) -> Optional.of(numList.stream().reduce(current.orElse(0L), Long::sum));

    public static void main(String[] args) throws InterruptedException {
        LOGGER.setLevel(Level.WARN);
        String[] appConfig = readArgumentsSilently(args);
        String zookeeperHosts = readFromArgumentListSilently(appConfig, 0, "localhost:2181");

        String groupId = readFromArgumentListSilently(appConfig, 1, "00");

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);

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

        final AccumulatorV2 resultReport = new MapAccumulator();
        javaSparkContext.sc().register(resultReport, "resultReport");

        ReportTask reportTimer = new ReportTask(resultReport);
        new Timer().schedule(reportTimer, 1000, 5000);

        try(JavaStreamingContext jssc = new JavaStreamingContext(javaSparkContext, Durations.milliseconds(STREAMING_DURATION))) {
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


            JavaDStream<String> coordinates = KafkaUtils.createDirectStream(
                    jssc,
                    LocationStrategies.PreferConsistent(),
                    ConsumerStrategies.<String,String>Subscribe(topics, kafkaParams))
                    .map(record -> record.value());


            JavaPairDStream<String, String> coordinatePair = coordinates
                    .mapToPair(coordinate -> {
                        String[] coordinateArray = coordinate.split(";");
                        String latitude = INV;
                        String longitude = INV;
                        if(coordinateArray.length == 2) {
                            latitude = coordinateArray[0];
                            longitude = coordinateArray[1];
                        } else {
                            System.err.println("ERROR reading stream");
                        }
                        return new Tuple2<>(latitude, longitude);
                    });

            JavaPairDStream<String, Long> sumCoordinates = coordinatePair
                    .map(pair -> {
                        Vector3d pointIn = converter.convertLatLongToVector3D(Double.valueOf(pair._1), Double.valueOf(pair._2));
                        long lookupIdIn = HTMfunc.lookupId(pointIn.ra(), pointIn.dec(), broadcastDepth.value());
                        boolean isInside = broadcastRange.value().isIn(lookupIdIn);
                        return isInside ? "1-g" + groupId : "0-g" + groupId;
                    })
                    .countByValue()
                    .reduceByKey((acc, val) -> acc + val);

            sumCoordinates.foreachRDD(rdd -> {
                resultReport.add(rdd.collectAsMap());
                if(resultReport.isZero()) {
                    System.out.println("NO RECORDS: " + System.currentTimeMillis());
                }
            });

            //DEBUG
            //sumCoordinates.print();

            if(ENABLE_STATE_PERSISTENCE) {
                sumCoordinates.updateStateByKey(UPDATE_CUMULATIVE_RESULTS_STATE);
            }

            // Start the computation
            jssc.start();
            jssc.awaitTermination();
        }
    }
}
