package edu.generator;

import com.sun.tools.javac.util.Pair;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomCoordinatePairGenerator implements StreamGenerator<Pair> {
    private static final Random rand = new Random();
    private double minLatitude;
    private double maxLatitude;
    private double minLongitude;
    private double maxLongitude;
    private double probability;
    private double latitudeWindow;
    private double longitudeWindow;

    public RandomCoordinatePairGenerator(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
        new RandomCoordinatePairGenerator(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    public RandomCoordinatePairGenerator(double probability, double minLatitude, double maxLatitude, double minLongitude,
                                         double maxLongitude) {
        this.probability = probability;
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
        this.latitudeWindow = maxLatitude - minLatitude;
        this.longitudeWindow = maxLongitude - minLatitude;
    }

    protected Double randLatitude() {
        Double latitude = ThreadLocalRandom.current().nextDouble(minLatitude, maxLatitude);
        if(this.rand.nextDouble() > probability) {
            latitude += latitudeWindow;
        }

        return latitude;
    }

    protected Double randLongitude() {
        Double longitude = ThreadLocalRandom.current().nextDouble(minLongitude, maxLongitude);
        if(this.rand.nextDouble() > probability) {
            longitude += longitudeWindow;
        }

        return longitude;
    }

    protected Pair randCoordinates() {

        Double latitude = ThreadLocalRandom.current().nextDouble(minLatitude, maxLatitude);
        Double longitude = ThreadLocalRandom.current().nextDouble(minLongitude, maxLongitude);

        if(this.rand.nextDouble() > probability) {
            latitude += latitudeWindow;
            longitude += longitudeWindow;
        }

        return new Pair(longitude, latitude);
    }

    @Override
    public Pair generate() {
        return randCoordinates();
    }

    @Override
    public String generateString() {
        return generate().toString();
    }
}
