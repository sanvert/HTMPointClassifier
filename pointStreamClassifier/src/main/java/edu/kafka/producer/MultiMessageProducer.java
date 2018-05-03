package edu.kafka.producer;

import com.sun.tools.javac.util.Pair;
import edu.generator.MultiRandomCoordinateGenerator;
import edu.generator.StreamGenerator;
import edu.util.PropertyMapper;

public class MultiMessageProducer extends MessageProducer {

    public MultiMessageProducer(final String zookeeperHosts, final StreamGenerator streamGenerator,
                                final int streamLength, final int multiCount, final int batchSize) {
        super(zookeeperHosts, streamGenerator, streamLength / multiCount, batchSize);
    }

    public static void main(String[] args) {

        //Region to generate random coordinates
        double minLatitude = 40.780000;
        double maxLatitude = 41.339800;
        double minLongitude = 28.507700;
        double maxLongitude = 29.441900;

        int streamLength = 1000000;
        int multiCount = 20;
        int batchSize = 40;

        String zookeeperHosts = PropertyMapper.defaults().get("zookeeper.host.list");
        StreamGenerator<Pair> generator
                = new MultiRandomCoordinateGenerator(0.6, minLatitude, maxLatitude, minLongitude, maxLongitude,
                multiCount);

        MessageProducer mp = new MultiMessageProducer(zookeeperHosts, generator, streamLength,
                multiCount, batchSize);
        mp.startSending();
    }
}
