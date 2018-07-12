package edu.kafka.producer;

import edu.generator.StreamGenerator;
import edu.kafka.zookeeper.ZooKeeperClientProxy;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MessageProducer {

    private final kafka.producer.ProducerConfig producerConfig;
    private final Properties properties;
    private final StreamGenerator streamGenerator;
    private final int streamLength;
    private final int batchSize;
    private String topicToSend;

    public MessageProducer(String topic, String zookeeperHosts, StreamGenerator streamGenerator,
                           int streamLength, int batchSize) {
        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        topicToSend = topic;
        properties = new Properties();

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
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, zooKeeperClientProxy.getKafkaBrokerListAsString());
            properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            properties.put(ProducerConfig.RETRIES_CONFIG, 0);
        }

        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); //gzip, snappy, lz4
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(batchSize * 1024));
        properties.put("metadata.broker.list", zooKeeperClientProxy.getKafkaBrokerListAsString());

        //Parameters for previous versions
        properties.put("request.required.acks", "0");
        properties.put("compression.codec", "2"); //1: gzip, 2:snappy
        properties.put("producer.type", "async");
        properties.put("batch.num.messages", String.valueOf(batchSize));
//        properties.put("queue.buffering.max.ms", "5000");
//        properties.put("queue.buffering.max.messages", "10000");

        this.producerConfig = new kafka.producer.ProducerConfig(properties);
        this.streamGenerator = streamGenerator;
        this.streamLength = streamLength;
        this.batchSize = batchSize;
    }

    public void startSending() {
        if(batchSize < 11) {
            Producer<Integer, String> producer = new Producer<>(producerConfig);
            sendLegacyBatch(producer, streamLength, batchSize);
            producer.close();
        } else {
            try(KafkaProducer kafkaProducer = new KafkaProducer(properties)) {
                sendBatch(kafkaProducer, streamLength);
            }
        }
    }

    public void startSendingWithKey() {
        try(KafkaProducer kafkaProducer = new KafkaProducer(properties)) {
            sendBatchWithKey(kafkaProducer, streamLength);
        }
    }

    private void sendLegacyBatch(Producer producer, int counter, int batchSize) {
        while (counter > 0) {
            List<KeyedMessage> batchList = new ArrayList<>();
            int currentBatchCount = batchSize;
            while(currentBatchCount > 0 && counter - batchList.size() > 0) {
                batchList.add( new KeyedMessage(topicToSend, streamGenerator.generateString()));
                currentBatchCount--;
            }
            producer.send(batchList);
            counter-=batchSize;
        }
    }

    private void sendBatch(KafkaProducer producer, int counter) {
        while (counter > 0) {
            ProducerRecord<Integer, String> message = new ProducerRecord<>(topicToSend,
                    streamGenerator.generateString());
            producer.send(message);
            counter--;
        }
    }

    private void sendBatchWithKey(KafkaProducer producer, int counter) {
        while (counter > 0) {
            ProducerRecord<Integer, String> message = new ProducerRecord<>(topicToSend, counter,
                    streamGenerator.generateString());
            producer.send(message);
            counter--;
        }
    }
}
