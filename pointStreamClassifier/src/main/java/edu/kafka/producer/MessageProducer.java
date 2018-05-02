package edu.kafka.producer;

import com.sun.tools.javac.util.Pair;
import edu.generator.RandomCoordinateGenerator;
import edu.generator.StreamGenerator;
import edu.kafka.ZooKeeperClientProxy;
import edu.util.PropertyMapper;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MessageProducer {

    private final ProducerConfig producerConfig;
    private final Properties props;
    private final StreamGenerator streamGenerator;
    private final int streamLength;
    private final int batchSize;
    private String topic;

    public MessageProducer(String zookeeperHosts, StreamGenerator streamGenerator, int streamLength, int batchSize) {
        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        topic = zooKeeperClientProxy.getKafkaTopics().get(0);
        props = new Properties();

        /*
         * To serialize custom payload
         * Serialization option 1 - com.spring.kafka.PayloadSerializer.
         * Serialization option 2 - ByteArraySerializer - Java Object -> String (Preferrably JSON represenation instead of toString)->byteArray
         */
        if(batchSize < 50) {
            //props.put("partitioner.class", "example.producer.SimplePartitioner");
        } else {
            props.put("acks", "0");
            props.put("bootstrap.servers", zooKeeperClientProxy.getKafkaBrokerListAsString());
            props.put("buffer.memory", 33554432);
            //"org.apache.kafka.common.serialization.StringSerializer"
            props.put("key.serializer", StringSerializer.class.getName());
            props.put("value.serializer", StringSerializer.class.getName());
            props.put("linger.ms", 1);
            props.put("retries", 0);
        }

        props.put("compression.codec", "2"); //1: GZIP, 2:Snappy
        props.put("request.required.acks", "0");
        props.put("metadata.broker.list", zooKeeperClientProxy.getKafkaBrokerListAsString());
        props.put("serializer.class", StringEncoder.class.getName());
        props.put("batch.num.messages", String.valueOf(batchSize));
        props.put("producer.type", "async");
        props.put("queue.buffering.max.ms", "5000");
        props.put("queue.buffering.max.messages", "10000");

        this.producerConfig = new ProducerConfig(props);
        this.streamGenerator = streamGenerator;
        this.streamLength = streamLength;
        this.batchSize = batchSize;
    }

    public void startSending() {

        if(batchSize < 50) {
            Producer<String, String> producer = new Producer<>(producerConfig);
            sendBatch(producer, streamLength, batchSize);
            producer.close();
        } else {
            try(KafkaProducer kafkaProducer = new KafkaProducer(props)) {
                send(kafkaProducer, streamLength);
            }
        }

    }

    private void sendBatch(Producer producer, int counter, int batchSize) {
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

    private void send(KafkaProducer producer, int counter) {
        while (counter > 0) {
            ProducerRecord<String, String> message = new ProducerRecord<>(topic, streamGenerator.generateString());
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

        String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");
        StreamGenerator<Pair> generator
                = new RandomCoordinateGenerator(1.0, minLatitude, maxLatitude, minLongitude, maxLongitude);
        MessageProducer producer = new MessageProducer(zookeeperHosts, generator, 1000, 100);
        producer.startSending();
    }
}
