package edu.generator;

public class RandomCoordinateGenerator extends RandomCoordinatePairGenerator {
    public RandomCoordinateGenerator(double probability, double minLatitude, double maxLatitude, double minLongitude,
                                     double maxLongitude) {
        super(probability, minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    private String randCoordinatesString() {

        return randLatitude().toString() + ";" + randLongitude().toString();
    }

    @Override
    public String generateString() {
        return randCoordinatesString();
    }
}
