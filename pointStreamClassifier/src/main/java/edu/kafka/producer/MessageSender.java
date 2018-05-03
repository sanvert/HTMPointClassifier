package edu.kafka.producer;

import edu.kafka.ZooKeeperClientProxy;
import kafka.serializer.StringEncoder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class MessageSender {

    private static final AtomicReference<Producer> kafkaProducerRef = new AtomicReference<>();
    private final Properties properties;
    private final String topic;

    public MessageSender(final ZooKeeperClientProxy zooKeeperClientProxy, final String topic, final int batchSize) {
        this.topic = topic;

        this.properties = new Properties();
        properties.put("acks", "0");
        properties.put("bootstrap.servers", zooKeeperClientProxy.getKafkaBrokerListAsString());
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());
        properties.put("linger.ms", 1);
        properties.put("retries", 0);

        properties.put("compression.codec", "2"); //1: GZIP, 2:Snappy
        properties.put("request.required.acks", "0");
        properties.put("metadata.broker.list", zooKeeperClientProxy.getKafkaBrokerListAsString());
        properties.put("serializer.class", StringEncoder.class.getName());
        properties.put("batch.num.messages", String.valueOf(batchSize));
        properties.put("producer.type", "async");
        properties.put("queue.buffering.max.ms", "5000");
        properties.put("queue.buffering.max.messages", "10000");

        if (kafkaProducerRef.get() == null) {
            kafkaProducerRef.getAndSet(new KafkaProducer(properties));
        }
    }

    public void send(String value) {
        send(null, value);
    }

    public void send(String key, String value) {
        try {
            kafkaProducerRef.get().send(new ProducerRecord(topic, key, value));
        } catch (Exception e) {
            System.out.println(e);
            try {
                kafkaProducerRef.getAndSet(new KafkaProducer(properties)).close();
            } catch (Exception k) {
            }
            kafkaProducerRef.get().send(new ProducerRecord(topic, key, value));
        }
    }
}
