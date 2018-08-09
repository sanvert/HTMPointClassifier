package edu.kafka.producer;

import edu.kafka.zookeeper.CustomZookeeperClientProxyProvider;
import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;
import edu.util.PropertyMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSenderFactory {
    //Topic to MessageSender mapper.
    private static final Map<String, MessageSender> MESSAGE_SENDER_MAP = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    private static final int KAFKA_BATCH_SIZE = 2048; //KB

    public static final String KAFKA_CONSUMER_TOPICS_PREFIX = "c-";

    public static MessageSender getSender(final String topic, final String zookeeperHosts) {
        final String consumerTopic = KAFKA_CONSUMER_TOPICS_PREFIX + topic;
        if (!MESSAGE_SENDER_MAP.containsKey(consumerTopic)) {
            synchronized (LOCK) {
                if (!MESSAGE_SENDER_MAP.containsKey(consumerTopic)) {
                    MessageSender messageSender
                        = new MessageSender(CustomZookeeperClientProxyProvider.getInstance(zookeeperHosts)
                            .getKafkaBrokerListAsString(), consumerTopic,
                        KAFKA_BATCH_SIZE
                                * Integer.parseInt(PropertyMapper.readDefaultProps().get("kafka.batch.size")));
                    MESSAGE_SENDER_MAP.putIfAbsent(consumerTopic, messageSender);
                }
            }
        }
        return MESSAGE_SENDER_MAP.get(consumerTopic);
    }
}
