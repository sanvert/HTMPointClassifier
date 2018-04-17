package edu.kafka;

import edu.util.PropertySetting;
import kafka.cluster.Broker;
import kafka.common.TopicAndPartition;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import scala.collection.JavaConversions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;

public class ZooKeeperClientProxy {
    private static int sessionTimeOutInMs = 15 * 1000; // 15 secs
    private static int connectionTimeOutInMs = 10 * 1000; // 10 secs
    private ZkClient zkClient;

    public ZooKeeperClientProxy(String zookeeperHosts) {
        this.zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs,
                ZKStringSerializer$.MODULE$);
    }

    public List<String> getKafkaTopics() {
        return JavaConversions.seqAsJavaList(ZkUtils.getAllTopics(zkClient));
    }

    public Map<String, Integer> getKafkaTopicAndPartitions() {
        return JavaConversions.setAsJavaSet(ZkUtils.getAllPartitions(zkClient)).stream()
                .collect(groupingBy(TopicAndPartition::topic, HashMap::new,
                        mapping(TopicAndPartition::partition, summingInt(n -> Integer.valueOf(n)))));
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
        return JavaConversions.seqAsJavaList(ZkUtils.getAllBrokersInCluster(zkClient));
    }

    public String getKafkaBrokerListAsString() {
        return getKafkaBrokers().stream().map(Broker::connectionString).collect(joining(","));
    }

    //Testing purpose only
    public static void main(String[] args) {
        String zookeeperHosts = PropertySetting.defaults().get("zookeeper.host.list");

        ZooKeeperClientProxy zooKeeperClientProxy = new ZooKeeperClientProxy(zookeeperHosts);
        zooKeeperClientProxy.getKafkaTopics().stream().forEach(System.out::println);
        System.out.println(zooKeeperClientProxy.getKafkaBrokerListAsString());
    }
}
