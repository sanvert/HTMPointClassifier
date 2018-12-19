package edu.kafka.producer;

import com.google.common.collect.Iterables;
import edu.kafka.reader.TweetJsonSourceReader;
import edu.kafka.zookeeper.ZooKeeperClientProxy;
import edu.util.PropertyMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class MessageProducerJsonSource {
    private static final int KAFKA_BATCH_SIZE = 1024; //KB

    private final kafka.producer.ProducerConfig producerConfig;
    private final Properties properties;
    private final int streamLength;
    private final String sourceDirectory;
    private final Map<String, String> tweetSource;
    private String topicToSend;

    public MessageProducerJsonSource(String sourceDirectory, String topic, String zookeeperHosts,
                                     int streamLength, int batchSize, Set<String> searchAreas) {
        searchAreas.add("istanbul");
        searchAreas.add("izmir");
        searchAreas.add("ankara");
        searchAreas.add("kocaeli");
        searchAreas.add("eskisehir");

        this.sourceDirectory = sourceDirectory;
        this.tweetSource = readSource(searchAreas);

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        this.topicToSend = topic;
        this.properties = new Properties();

        /*
         * To serialize custom payload
         * Serialization option 1 - com.spring.kafka.PayloadSerializer.
         * Serialization option 2 - ByteArraySerializer - Java Object -> String (Preferrably JSON represenation instead of toString)->byteArray
         */
        if(batchSize < 20) {
            //properties.put("partitioner.class", "example.producer.SimplePartitioner");
            //properties.put("producer.type", "async"); // Deprecated, always async
            //properties.put("serializer.class", StringEncoder.class.getName());
            //properties.put("queue.buffering.max.ms", "5000");
            //properties.put("queue.buffering.max.messages", "10000");
        } else {
            properties.put(ProducerConfig.ACKS_CONFIG, "0");
            System.out.println(zooKeeperClientProxy.getKafkaBrokerListAsString());
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, zooKeeperClientProxy.getKafkaBrokerListAsString());
            properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            properties.put(ProducerConfig.RETRIES_CONFIG, 0);
        }

        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                PropertyMapper.readDefaultProps().get("kafka.compression.codec.name")); //gzip, snappy, lz4
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG,
                String.valueOf(KAFKA_BATCH_SIZE *
                        Integer.parseInt(PropertyMapper.readDefaultProps().get("kafka.batch.size"))));
        properties.put("metadata.broker.list", zooKeeperClientProxy.getKafkaBrokerListAsString());

        //Parameters for previous versions
        properties.put("request.required.acks", "0");
        properties.put("compression.codec",
                PropertyMapper.readDefaultProps().get("kafka.compression.codec.id")); //1: gzip, 2:snappy
        properties.put("producer.type", "async");
        properties.put("batch.num.messages", String.valueOf(batchSize));
//        properties.put("queue.buffering.max.ms", "5000");
//        properties.put("queue.buffering.max.messages", "10000");

        this.producerConfig = new kafka.producer.ProducerConfig(properties);
        this.streamLength = streamLength;
    }

    private void sendBatchWithKey(KafkaProducer producer, int counter) {
        Iterator<Map.Entry<String, String>> cyclicIterator = Iterables.cycle(this.tweetSource.entrySet()).iterator();
        while (counter > 0) {
            Map.Entry<String, String> entry = cyclicIterator.next();
            ProducerRecord<String, String> message = new ProducerRecord<>(topicToSend, entry.getKey(),
                    entry.getValue());
            producer.send(message);
            counter--;
        }
    }

    private Map<String, String> readSource(Set<String> searchAreas) {
        String extension = ".json";

        try {
            TweetJsonSourceReader reader = new TweetJsonSourceReader(sourceDirectory, searchAreas, extension);
            return reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    public void startSendingWithKey() {
        try(KafkaProducer kafkaProducer = new KafkaProducer(properties)) {
            sendBatchWithKey(kafkaProducer, streamLength);
        }
    }

    public static void main(String[] args) {
        String tempInputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data/temp";
        String processedInputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data/processed";
        String ongoingInputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data";
        Set<String> searchKeywords = new HashSet<>();
        searchKeywords.add("istanbul");
        searchKeywords.add("izmir");
        searchKeywords.add("ankara");
        searchKeywords.add("kocaeli");
        searchKeywords.add("eskisehir");
        String extension = ".json";

        try {
            TweetJsonSourceReader reader = new TweetJsonSourceReader(tempInputFolder, searchKeywords, extension);
            reader.read().entrySet().stream().limit(100).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
