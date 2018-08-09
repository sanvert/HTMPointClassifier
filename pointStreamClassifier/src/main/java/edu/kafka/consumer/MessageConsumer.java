package edu.kafka.consumer;

import edu.kafka.zookeeper.CustomZookeeperClientProxyProvider;
import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class MessageConsumer implements Runnable {

    private static final boolean DEBUG = false;
    private static final int POLL_TIMEOUT_MSEC = 1000;
    private static final AtomicLong CHANNEL_SIZE = new AtomicLong(0);

    private final Properties properties;
    private final Consumer<Integer, String> messageConsumer;
    private final AsynchronousFileChannel asynchronousFileChannel;

    public MessageConsumer(final AsynchronousFileChannel fileChannel, final String zookeeperHosts,
                           final String topic, final String groupId) {
        this.properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                CustomZookeeperClientProxyProvider.getInstance(zookeeperHosts).getKafkaBrokerListAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer using props.
        messageConsumer = new KafkaConsumer<>(properties);
        // Subscribe to the topic.
        messageConsumer.subscribe(Collections.singletonList(topic));
        this.asynchronousFileChannel = fileChannel;
    }

    public void run() {
        final int giveUpCount = 11;
        int noRecordsCount = 0;
        System.out.println("STARTED");

        while (true) {
            try {
                final ConsumerRecords<Integer, String> consumerRecords =
                        messageConsumer.poll(POLL_TIMEOUT_MSEC);
                if (consumerRecords.count() == 0) {
                    noRecordsCount++;
                    if (noRecordsCount > giveUpCount) break;
                    else continue;
                }

                consumerRecords.forEach(record -> {
                    if(DEBUG) {
                        System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                                record.key(), record.value(),
                                record.partition(), record.offset());
                    }
                    StringBuilder sb = new StringBuilder(record.value().length())
                            .append(record.key())
                            .append(":")
                            .append(record.value())
                            .append(StringUtils.LF);

                    byte[] toWrite = sb.toString().getBytes();
                    long currentPosition = CHANNEL_SIZE.getAndAdd(toWrite.length);
                    asynchronousFileChannel.write(ByteBuffer.wrap(toWrite), currentPosition);

                });
                System.out.println("Num of received recs: " + consumerRecords.count());

                if(!consumerRecords.isEmpty()) {
                    System.out.println(System.currentTimeMillis() + "L");
                }
                messageConsumer.commitAsync();
            } catch (Exception e) {
                System.out.println(e);
                noRecordsCount++;
            }
        }

        messageConsumer.close();
        System.out.println("DONE");
        try {
            asynchronousFileChannel.force(false);
        } catch (IOException e) {
            System.out.println("CHANNEL ERROR:" + e);
        }
    }
}
