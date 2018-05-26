package edu.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;

public class LargePolygonSplitter {
    private static final GeometryFactory gf = new GeometryFactory();

    public static List<Geometry> split(Geometry given, double maxArea) {
        return split(given, maxArea, g -> false);
    }

    public static List<Geometry> split(Geometry given, double maxArea, Predicate<Geometry> predicate) {
        List<Geometry> others = new ArrayList<>();
        PriorityQueue<Geometry> queue = new PriorityQueue<>(comparing(Geometry::getArea).reversed());
        queue.add(given);

        while (queue.peek().getEnvelope().getArea() > maxArea) {
            Geometry current = queue.poll();
            Point centroid = current.getCentroid();
            Geometry bbox = current.getEnvelope();
            if(!(bbox.getCoordinates().length == 5)) {
                throw new IllegalStateException();
            }
            for (int i = 0; i < 4; i++) {
                Geometry intersection = current.intersection(box(centroid, bbox.getCoordinates()[i]));
                if (!intersection.isEmpty()) {
                    if (predicate.test(intersection)) {
                        others.add(intersection);
                    }
                    else {
                        queue.add(intersection);
                    }
                }
            }
        }

        List<Geometry> result = new ArrayList<>(queue);
        result.addAll(others);
        return result;
    }

    private static Geometry box(Point centroid, Coordinate corner) {
        LineString lineString = gf.createLineString(new Coordinate[] { centroid.getCoordinate(), corner });
        return lineString.getEnvelope();
    }
}