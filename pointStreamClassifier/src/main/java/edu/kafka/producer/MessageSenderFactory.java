package edu.kafka.producer;

import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSenderFactory {
    //Topic to MessageSender mapper.
    private static final Map<String, MessageSender> MESSAGE_SENDER_MAP = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    private static final int KAFKA_CLIENT_BATCH_SIZE = 1024; //KB

    public static final String KAFKA_CONSUMER_TOPICS_PREFIX = "c-";

    public static MessageSender getSender(String topic) {
        topic = KAFKA_CONSUMER_TOPICS_PREFIX + topic;
        if (!MESSAGE_SENDER_MAP.containsKey(topic)) {
            synchronized (LOCK) {
                if (!MESSAGE_SENDER_MAP.containsKey(topic)) {
                    MessageSender messageSender
                            = new MessageSender(ZookeeperClientProxyWrapper.getInstance().getKafkaBrokerListAsString(),
                            topic,
                            KAFKA_CLIENT_BATCH_SIZE);
                    MESSAGE_SENDER_MAP.putIfAbsent(topic, messageSender);
                }
            }
        }
        return MESSAGE_SENDER_MAP.get(topic);
    }
}
