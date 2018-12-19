package edu.kafka.producer;

import edu.kafka.zookeeper.ZookeeperClientProxyWrapper;
import edu.util.PropertyMapper;
import org.apache.kafka.common.serialization.IntegerSerializer;

// Bill Pugh style singleton. Replaced with another instance.
public class MessageSenderWrapper {
    private MessageSenderWrapper() {
        // private constructor
    }

    // static inner class - inner classes are not loaded until they are
    // referenced.
    // used by threads working in Spark to response
    private static class MessageSenderHolder {
        private static final String KAFKA_CONSUMER_TOPICS_PREFIX = "c-";
        //50 total
        private static final int KAFKA_BATCH_SIZE = 1024; //KB

        private static MessageSender messageSender
                = new MessageSender(ZookeeperClientProxyWrapper.getInstance().getKafkaBrokerListAsString(),
                        ZookeeperClientProxyWrapper.getInstance().getKafkaTopics().stream()
                            .filter(t -> t.startsWith(KAFKA_CONSUMER_TOPICS_PREFIX))
                            .findFirst().orElse(KAFKA_CONSUMER_TOPICS_PREFIX),
                KAFKA_BATCH_SIZE * Integer.parseInt(PropertyMapper.readDefaultProps().get("kafka.batch.size")),
                        IntegerSerializer.class.getName()
        );
    }

    public static MessageSender getInstance() {
        return MessageSenderHolder.messageSender;
    }

}
