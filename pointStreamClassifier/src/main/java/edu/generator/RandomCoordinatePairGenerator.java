package edu.generator;

import com.sun.tools.javac.util.Pair;
import edu.kafka.producer.RegionBox;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomCoordinatePairGenerator implements StreamGenerator<Pair> {
    private static final Random rand = new Random();
    private RegionBox regionBox;
    private double probability;
    private double latitudeWindow;
    private double longitudeWindow;

    public RandomCoordinatePairGenerator(RegionBox regionBox) {
        this.regionBox = regionBox;
    }

    public RandomCoordinatePairGenerator(double probability, RegionBox regionBox) {
        this.probability = probability;
        this.regionBox = regionBox;
        this.latitudeWindow = regionBox.getMaxLatitude() - regionBox.getMinLatitude();
        this.longitudeWindow = regionBox.getMaxLongitude() - regionBox.getMinLongitude();
    }

    protected Double randLatitude() {
        Double latitude = ThreadLocalRandom.current().nextDouble(regionBox.getMinLatitude(), regionBox.getMaxLatitude());
        if(this.rand.nextDouble() > probability) {
            latitude += latitudeWindow;
        }

        return latitude;
    }

    protected Double randLongitude() {
        Double longitude = ThreadLocalRandom.current().nextDouble(regionBox.getMinLongitude(), regionBox.getMinLongitude());
        if(this.rand.nextDouble() > probability) {
            longitude += longitudeWindow;
        }

        return longitude;
    }

    protected Pair randCoordinates() {

        Double latitude = ThreadLocalRandom.current().nextDouble(regionBox.getMinLatitude(), regionBox.getMaxLatitude());
        Double longitude = ThreadLocalRandom.current().nextDouble(regionBox.getMinLongitude(), regionBox.getMinLongitude());

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
