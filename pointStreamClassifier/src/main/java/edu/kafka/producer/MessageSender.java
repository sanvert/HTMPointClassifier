package edu.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class MessageSender {

    private final AtomicReference<Producer> kafkaProducerRef = new AtomicReference<>();
    private final Properties properties;
    private final String topic;

    public MessageSender(final String kafkaBrokerList, final String topic, final int batchSize) {
        this.topic = topic;

        this.properties = new Properties();
        properties.put(ProducerConfig.ACKS_CONFIG, "0");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        properties.put(ProducerConfig.RETRIES_CONFIG, 0);
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); //1: gzip, 2: snappy
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(batchSize * 1024));

        //Parameters for previous versions
        properties.put("metadata.broker.list", kafkaBrokerList);
        properties.put("compression.codec", "2"); //1: GZIP, 2:Snappy
        properties.put("request.required.acks", "0");
        //properties.put("serializer.class", StringEncoder.class.getName());
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

    public void send(Integer key, String value) {
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
