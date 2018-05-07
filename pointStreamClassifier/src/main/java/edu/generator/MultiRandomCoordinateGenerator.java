package edu.generator;

import edu.kafka.producer.RegionBox;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiRandomCoordinateGenerator extends RandomCoordinatePairGenerator {
    private final int multiCount;

    public MultiRandomCoordinateGenerator(double probability, RegionBox regionBox, int multiCount) {
        super(probability, regionBox);
        this.multiCount = multiCount;
    }

    @Override
    public String generateString() {
        return IntStream.range(0, multiCount)
                .mapToObj(i -> randLatitude().toString() + ";" + randLongitude().toString())
                .collect(Collectors.joining(","));
    }
}
