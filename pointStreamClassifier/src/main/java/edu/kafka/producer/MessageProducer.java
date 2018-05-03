package edu.kafka.producer;

import com.sun.tools.javac.util.Pair;
import edu.generator.RandomCoordinateGenerator;
import edu.generator.StreamGenerator;
import edu.kafka.zookeeper.ZooKeeperClientProxy;
import edu.util.PropertyMapper;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MessageProducer {

    private final kafka.producer.ProducerConfig producerConfig;
    private final Properties props;
    private final StreamGenerator streamGenerator;
    private final int streamLength;
    private final int batchSize;
    private String topic;

    public MessageProducer(String zookeeperHosts, StreamGenerator streamGenerator,
                           int streamLength, int batchSize) {
        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        topic = zooKeeperClientProxy.getKafkaTopics().get(0);
        props = new Properties();

        /*
         * To serialize custom payload
         * Serialization option 1 - com.spring.kafka.PayloadSerializer.
         * Serialization option 2 - ByteArraySerializer - Java Object -> String (Preferrably JSON represenation instead of toString)->byteArray
         */
        if(batchSize < 20) {
            //props.put("partitioner.class", "example.producer.SimplePartitioner");
            //props.put("producer.type", "async"); // Deprecated, always async
            //props.put("serializer.class", StringEncoder.class.getName());
            //props.put("queue.buffering.max.ms", "5000");
            //props.put("queue.buffering.max.messages", "10000");
        } else {
            props.put(ProducerConfig.ACKS_CONFIG, "0");
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, zooKeeperClientProxy.getKafkaBrokerListAsString());
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            props.put(ProducerConfig.RETRIES_CONFIG, 0);
        }

        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); //1: gzip, 2: snappy
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(batchSize * 1000));

        this.producerConfig = new kafka.producer.ProducerConfig(props);
        this.streamGenerator = streamGenerator;
        this.streamLength = streamLength;
        this.batchSize = batchSize;
    }

    public void startSending() {
        if(batchSize < 50) {
            Producer<String, String> producer = new Producer<>(producerConfig);
            sendLegacyBatch(producer, streamLength, batchSize);
            producer.close();
        } else {
            try(KafkaProducer kafkaProducer = new KafkaProducer(props)) {
                sendBatch(kafkaProducer, streamLength);
            }
        }
    }

    public void startSendingWithKey() {
        try(KafkaProducer kafkaProducer = new KafkaProducer(props)) {
            sendBatchWithKey(kafkaProducer, streamLength);
        }
    }

    private void sendLegacyBatch(Producer producer, int counter, int batchSize) {
        while (counter > 0) {
            List<KeyedMessage> batchList = new ArrayList<>();
            int currentBatchCount = batchSize;
            while(currentBatchCount > 0 && counter - batchList.size() > 0) {
                batchList.add( new KeyedMessage(topic, streamGenerator.generateString()));
                currentBatchCount--;
            }
            producer.send(batchList);
            counter-=batchSize;
        }
    }

    private void sendBatch(KafkaProducer producer, int counter) {
        while (counter > 0) {
            ProducerRecord<String, String> message = new ProducerRecord<>(topic, streamGenerator.generateString());
            producer.send(message);
            counter--;
        }
    }

    private void sendBatchWithKey(KafkaProducer producer, int counter) {
        while (counter > 0) {
            ProducerRecord<String, String> message = new ProducerRecord<>(topic, String.valueOf(counter),
                    streamGenerator.generateString());
            producer.send(message);
            counter--;
        }
    }

    public static void main(String[] args) {
        //Region to generate random coordinates
        double minLatitude = 40.780000;
        double maxLatitude = 41.339800;
        double minLongitude = 28.507700;
        double maxLongitude = 29.441900;

        int streamLength = 1000;
        int batchSize = 50;

        String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");
        StreamGenerator<Pair> generator
                = new RandomCoordinateGenerator(1.0, minLatitude, maxLatitude, minLongitude, maxLongitude);
        MessageProducer producer = new MessageProducer(zookeeperHosts, generator, streamLength, batchSize);
        producer.startSending();
    }
}
