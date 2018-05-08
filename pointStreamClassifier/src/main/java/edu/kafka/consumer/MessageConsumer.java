package edu.kafka.consumer;

import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;
import edu.util.PropertyMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;
import java.util.Properties;

public class MessageConsumer implements Runnable {

    private static final boolean DEBUG = false;
    private static final int POLL_TIMEOUT_MSEC = 1000;

    public static final String KAFKA_TOPIC_GATHERING_TOPICS_PREFIX = "m";

    private final Properties properties;
    private final Consumer<Integer, String> messageConsumer;

    public MessageConsumer(final String topic, final String groupId) {
        this.properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                ZookeeperClientProxyWrapper.getInstance().getKafkaBrokerListAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer using props.
        messageConsumer = new KafkaConsumer<>(properties);
        // Subscribe to the topic.
        messageConsumer.subscribe(Collections.singletonList(topic));
    }

    public void run() {
        final int giveUp = 11;
        int noRecordsCount = 0;
        System.out.println("STARTED");

        while (true) {
            try {
                final ConsumerRecords<Integer, String> consumerRecords =
                        messageConsumer.poll(POLL_TIMEOUT_MSEC);
                if (consumerRecords.count() == 0) {
                    noRecordsCount++;
                    if (noRecordsCount > giveUp) break;
                    else continue;
                }
                if (DEBUG) {
                    consumerRecords.forEach(record -> {
                        System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                                record.key(), record.value(),
                                record.partition(), record.offset());
                    });
                } else {
                    System.out.println("Num of received recs: " + consumerRecords.count());
                }
                if(!consumerRecords.isEmpty()) {
                    System.out.println(System.currentTimeMillis());
                }
                messageConsumer.commitAsync();
            } catch (Exception e) {
                System.out.println(e);
                noRecordsCount++;
            }
        }

        messageConsumer.close();
        System.out.println("DONE");
    }

    public static void main(String[] args) {
        Runnable messageConsumer = new MessageConsumer(KAFKA_TOPIC_GATHERING_TOPICS_PREFIX,
                PropertyMapper.defaults().get("kafka.group.id"));
        new Thread(messageConsumer).start();
    }
}
