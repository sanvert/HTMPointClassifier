package edu.generator;

public class RandomCoordinateGenerator extends RandomCoordinatePairGenerator {
    public RandomCoordinateGenerator(double probability, double minLatitude, double maxLatitude, double minLongitude,
                                     double maxLongitude) {
        super(probability, minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    @Override
    public String generateString() {
        return randLatitude().toString() + ";" + randLongitude().toString();
    }
}
