package edu.kafka.zookeeper;

import edu.util.PropertyMapper;

public class ZookeeperClientProxyWrapper {
    private ZookeeperClientProxyWrapper() {
        // private constructor
    }

    private static class ZookeeperClientProxyHolder {
        private static ZooKeeperClientProxy zooKeeperClientProxy
                = new ZooKeeperClientProxy(PropertyMapper.defaults().get("zookeeper.host.list"));
    }

    // global access point
    public static ZooKeeperClientProxy getInstance() {
        return ZookeeperClientProxyHolder.zooKeeperClientProxy;
    }

}
