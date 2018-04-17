package sky.htm;

import edu.jhu.htm.core.Convex;
import edu.jhu.htm.core.Vector3d;
import edu.jhu.htm.geometry.Chull;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.MultiPolygon;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;
import spherical.util.Pair;

import java.io.Serializable;
import java.util.List;

public class Converter implements Serializable {

    private static final double R = 6371.0;

    public Vector3d convertLatLongToVector3D(double lat, double lng) {
        lng = Math.toRadians(lng);
        lat = Math.toRadians(lat);
        double x = R * Math.cos(lng) * Math.cos(lat);
        double y = R * Math.cos(lng) * Math.sin(lat);
        double z = R * Math.sin(lng);

        return new Vector3d(x, y, z);
    }

    public Pair<Double, Double> convertVector3DToLatLon(Vector3d v) {
        // Lat long reverse conversion
        double lat = Math.toDegrees(Math.atan2(v.y(), v.x()));
        double lng = Math.toDegrees(Math.asin(v.z() / R));
        return new Pair(lat, lng);
    }

    public Convex createConvexByListOfPoints(List<Pair<Double, Double>> pointList) {
        Chull c = new Chull();
        for(Pair<Double, Double> p : pointList) {
            c.add(convertLatLongToVector3D(p.getX(), p.getY()));
        }
        return c.getConvex();
    }

    public Convex convertMapGeometryIntoConvex(GeoPackageGeometryData data) throws Exception {
        Chull c = new Chull();
        if(data.getGeometry() != null &&
                data.getGeometry().getGeometryType() == GeometryType.MULTIPOLYGON) {
            MultiPolygon mp = (MultiPolygon)data.getGeometry();
            for(Polygon polygon : mp.getPolygons()) {
                for(LineString line : polygon.getRings()) {
                    for(Point point : line.getPoints()) {
                        Vector3d vector = convertLatLongToVector3D(point.getY(), point.getX());
                        c.add(vector);
                    }
                }
            }
            c.makeConvex();
        }

        return c.getConvex();

    }
}
