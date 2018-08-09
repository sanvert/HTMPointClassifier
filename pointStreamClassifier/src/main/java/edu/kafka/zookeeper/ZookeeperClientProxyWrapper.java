package edu.kafka.zookeeper;

import edu.util.PropertyMapper;

// Bill Pugh style singleton.
public class ZookeeperClientProxyWrapper {
    private ZookeeperClientProxyWrapper() {
        // private constructor
    }

    private static class ZookeeperClientProxyHolder {
        private static ZooKeeperClientProxy ZOOKEEPER_CLIENT_PROXY
                = new ZooKeeperClientProxy(PropertyMapper.readDefaultProps().get("zookeeper.host.list"));
    }

    public static ZooKeeperClientProxy getInstance() {
        return ZookeeperClientProxyHolder.ZOOKEEPER_CLIENT_PROXY;
    }

}
