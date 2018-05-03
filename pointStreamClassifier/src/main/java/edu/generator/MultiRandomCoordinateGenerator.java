package edu.generator;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiRandomCoordinateGenerator extends RandomCoordinatePairGenerator {
    private final int multiCount;

    public MultiRandomCoordinateGenerator(double probability, double minLatitude, double maxLatitude,
                                          double minLongitude, double maxLongitude, int multiCount) {
        super(probability, minLatitude, maxLatitude, minLongitude, maxLongitude);
        this.multiCount = multiCount;
    }

    @Override
    public String generateString() {
        return IntStream.range(0, multiCount)
                .mapToObj(i -> randLatitude().toString() + ";" + randLongitude().toString())
                .collect(Collectors.joining(","));
    }
}
