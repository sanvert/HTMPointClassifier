package edu.kafka.consumer;

import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;
import java.util.Properties;

public class MessageConsumer {

    private final Properties properties;
    private final Consumer<Long, String> messageConsumer;

    public MessageConsumer(final String topic, final String groupId) {
        this.properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                ZookeeperClientProxyWrapper.getInstance().getKafkaBrokerListAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer using props.
        messageConsumer = new KafkaConsumer<>(properties);
        // Subscribe to the topic.
        messageConsumer.subscribe(Collections.singletonList(topic));

    }

    public void runConsumer() {
        final int giveUp = 100;
        int noRecordsCount = 0;

        while (true) {
            try {
                final ConsumerRecords<Long, String> consumerRecords =
                        messageConsumer.poll(500);
                if (consumerRecords.count() == 0) {
                    noRecordsCount++;
                    if (noRecordsCount > giveUp) break;
                    else continue;
                }
                consumerRecords.forEach(record -> {
                    System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                            record.key(), record.value(),
                            record.partition(), record.offset());
                });
                messageConsumer.commitAsync();
            } catch (Exception e) {
                System.out.println(e);
                noRecordsCount++;
            }
        }

        messageConsumer.close();
        System.out.println("DONE");
    }
}
