package edu.kafka.producer;

import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;

public class MessageSenderWrapper {
    private MessageSenderWrapper() {
        // private constructor
    }

    // static inner class - inner classes are not loaded until they are
    // referenced.
    private static class MessageSenderHolder {
        private static final String KAFKA_TOPIC_EMITTING_TOPICS_PREFIX = "e";
        private static final int KAFKA_CLIENT_BATCH_SIZE = 50;

        private static MessageSender messageSender
                = new MessageSender(ZookeeperClientProxyWrapper.getInstance().getKafkaBrokerListAsString(),
                ZookeeperClientProxyWrapper.getInstance().getKafkaTopics().stream()
                        .filter(t -> t.startsWith(KAFKA_TOPIC_EMITTING_TOPICS_PREFIX))
                        .findFirst().orElse(KAFKA_TOPIC_EMITTING_TOPICS_PREFIX),
                KAFKA_CLIENT_BATCH_SIZE
        );
    }

    // global access point
    public static MessageSender getInstance() {
        return MessageSenderHolder.messageSender;
    }

}
