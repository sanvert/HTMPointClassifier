package edu.util;


import com.vividsolutions.jts.algorithm.locate.IndexedPointInAreaLocator;
import com.vividsolutions.jts.algorithm.locate.PointOnGeometryLocator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.LineStringExtracter;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.distance.FacetSequenceTreeBuilder;
import org.geotools.geojson.GeoJSON;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.geom.GeometryJSON;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JTSIndexCreator {



    public static void create() throws IOException, URISyntaxException {
        final STRtree tree = new STRtree();
        //List<Geometry> gList = LargePolygonSplitter.split(g.getGeometryN(0).getGeometryN(0), 0.000005);
        //gList.forEach(geom -> tree.insert(geom.getEnvelopeInternal(), geom));
        //tree = FacetSequenceTreeBuilder.build(g);

        final Map<Integer, PointOnGeometryLocator> polygonIndexMap = new HashMap<>();

        final Path paths = Paths.get(ClassLoader.getSystemResource("geojson").toURI());
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(paths, "*.json")) {
            stream.forEach(path -> {
                String regionName = path.getFileName().toString().split("_")[0];
                GeometryJSON geometryJSON = new GeometryJSON(12);
                try {
                    GeometryCollection g = (GeometryCollection)geometryJSON
                            .read(new FileInputStream(path.toFile()));
                    MultiPolygon mp = (MultiPolygon) g.getGeometryN(0);
                    //Polygon p = (Polygon) g.getGeometryN(0).getGeometryN(0);
                    PointOnGeometryLocator areaLocator = new IndexedPointInAreaLocator(mp);
                    polygonIndexMap.put(RegionMapper.CITY_ID_MAP.get(regionName), areaLocator);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        int res = polygonIndexMap.get(1).locate(new Coordinate(29.41804, 40.89778));




        System.out.println();
    }

    public static void main(String[] args) {
        try {
            create();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
