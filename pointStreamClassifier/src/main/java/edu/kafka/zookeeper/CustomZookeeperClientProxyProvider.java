package edu.kafka.zookeeper;

public class CustomZookeeperClientProxyProvider {
    private static final Object LOCK = new Object();
    private volatile static ZooKeeperClientProxy ZOOKEEPER_CLIENT_PROXY;

    private CustomZookeeperClientProxyProvider() {
        //ignore constructor
    }

    public static ZooKeeperClientProxy getInstance(final String zookeeperHosts) {
        if(ZOOKEEPER_CLIENT_PROXY == null) {
            synchronized (LOCK) {
                if(ZOOKEEPER_CLIENT_PROXY == null) {
                    ZOOKEEPER_CLIENT_PROXY
                            = new ZooKeeperClientProxy(zookeeperHosts);
                }
            }
        }
        return ZOOKEEPER_CLIENT_PROXY;
    }
}
