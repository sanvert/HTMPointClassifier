package edu.generator;

import edu.kafka.producer.RegionBox;

public class RandomCoordinateGenerator extends RandomCoordinatePairGenerator {
    public RandomCoordinateGenerator(double probability, RegionBox regionBox) {
        super(probability, regionBox);
    }

    @Override
    public String generateString() {
        return randCoordinatesConcatenated();
    }
}
