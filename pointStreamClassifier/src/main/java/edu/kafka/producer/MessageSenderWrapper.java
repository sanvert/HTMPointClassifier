package edu.kafka.producer;

import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;

// Bill Pugh style singleton.
public class MessageSenderWrapper {
    private MessageSenderWrapper() {
        // private constructor
    }

    // static inner class - inner classes are not loaded until they are
    // referenced.
    private static class MessageSenderHolder {
        private static final String KAFKA_CONSUMER_TOPICS_PREFIX = "c-";
        private static final int KAFKA_CLIENT_BATCH_SIZE = 50; //KB

        private static MessageSender messageSender
                = new MessageSender(ZookeeperClientProxyWrapper.getInstance().getKafkaBrokerListAsString(),
                            ZookeeperClientProxyWrapper.getInstance().getKafkaTopics().stream()
                                .filter(t -> t.startsWith(KAFKA_CONSUMER_TOPICS_PREFIX))
                                .findFirst().orElse(KAFKA_CONSUMER_TOPICS_PREFIX),
                            KAFKA_CLIENT_BATCH_SIZE
                            );
    }

    public static MessageSender getInstance() {
        return MessageSenderHolder.messageSender;
    }

}
