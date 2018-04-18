package edu.kafka;

import edu.util.PropertySetting;
import kafka.cluster.Broker;
import kafka.zk.KafkaZkClient;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Time;
import scala.collection.JavaConversions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;

public class ZooKeeperClientProxy {
    private static int sessionTimeOutInMs = 15 * 1000; // 15 secs
    private static int connectionTimeOutInMs = 10 * 1000; // 10 secs
    private KafkaZkClient zkClient;

    public ZooKeeperClientProxy(String zookeeperHosts) {
        this.zkClient = KafkaZkClient.apply(zookeeperHosts, false, sessionTimeOutInMs,
                connectionTimeOutInMs, 100, Time.SYSTEM,
                "kafka.server", "SessionExpireListener");
    }

    public List<String> getKafkaTopics() {
        return JavaConversions.seqAsJavaList(zkClient.getAllTopicsInCluster()).stream()
                .filter(t -> !t.contains("_")).collect(Collectors.toList());
    }

    public Map<String, Integer> getKafkaTopicAndPartitions() {
        return JavaConversions.setAsJavaSet(zkClient.getAllPartitions()).stream()
                .collect(groupingBy(TopicPartition::topic, HashMap::new,
                        mapping(TopicPartition::partition, summingInt(n -> Integer.valueOf(n)))));
    }

    public List<Broker> getKafkaBrokers() {
        /*
         * Another way to return brokers
         * List<String> ids = zkClient.getChildren("/brokers/ids");
         *  ids.stream().forEach(id -> {
         *      String brokerData = zkClient.readData("/brokers/ids/" + id, true);
         *      System.out.println(brokerData);
         *  });
         */
        return JavaConversions.seqAsJavaList(zkClient.getAllBrokersInCluster());
    }

    public String getKafkaBrokerListAsString() {
        return getKafkaBrokers().stream()
                .flatMap(broker -> JavaConversions.seqAsJavaList(broker.endPoints()).stream())
                .map(endPoint -> endPoint.host() + ":" + endPoint.port())
                .collect(joining(","));
    }

    //Testing purpose only
    public static void main(String[] args) {
        String zookeeperHosts = PropertySetting.defaults().get("zookeeper.host.list");
        System.out.println(zookeeperHosts);
        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        zooKeeperClientProxy.getKafkaTopics().stream().forEach(System.out::println);
        System.out.println(zooKeeperClientProxy.getKafkaBrokerListAsString());
    }
}
