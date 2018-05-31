package edu.util;


import com.vividsolutions.jts.algorithm.locate.IndexedPointInAreaLocator;
import com.vividsolutions.jts.algorithm.locate.PointOnGeometryLocator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Location;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.geojson.geom.GeometryJSON;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RTreeIndex implements Serializable {

    private final Map<Integer, PointOnGeometryLocator> polygonIndexMap = new HashMap<>();

    private final RegionHTMIndex regionHTMIndex;

    public RTreeIndex(final RegionHTMIndex regionHTMIndex) {
        this.regionHTMIndex = regionHTMIndex;

        try {
            create();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void buildGeometryTree() {
//        final STRtree tree = new STRtree();
//        List<Geometry> gList = LargePolygonSplitter.split(g.getGeometryN(0).getGeometryN(0), 0.000005);
//        gList.forEach(geom -> tree.insert(geom.getEnvelopeInternal(), geom));
//        tree = FacetSequenceTreeBuilder.build(g);
    }

    private void create() throws IOException, URISyntaxException {

        final Path paths = Paths.get(ClassLoader.getSystemResource("geojson").toURI());

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(paths, "*.json")) {
            stream.forEach(path -> {
                String regionName = path.getFileName().toString().split("_")[0].toLowerCase();
                GeometryJSON geometryJSON = new GeometryJSON(12);
                try {
                    GeometryCollection g = (GeometryCollection) geometryJSON
                            .read(new FileInputStream(path.toFile()));
                    MultiPolygon mp = (MultiPolygon) g.getGeometryN(0);
                    //Polygon p = (Polygon) g.getGeometryN(0).getGeometryN(0);
                    PointOnGeometryLocator areaLocator = new IndexedPointInAreaLocator(mp);
                    polygonIndexMap.put(regionHTMIndex.getCityMap().get(regionName), areaLocator);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public Optional<Integer> getResidingRegionId(final Set<Integer> regionIdSet, final Coordinate coordinate) {

        for(Integer id : regionIdSet) {
            if (polygonIndexMap.containsKey(id)) {
                int location = polygonIndexMap.get(id).locate(coordinate);
                if (location != Location.EXTERIOR) {
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }

    //Test
    public static void main(String[] args) {
        String regionFile = "regionsHTM.json";
        final RegionHTMIndex regionHTMIndex = new RegionHTMIndex(regionFile);
        final RTreeIndex rTreeIndex = new RTreeIndex(regionHTMIndex);

        final Set<Integer> trialSet = new HashSet<>();
        trialSet.add(1);

        Optional<Integer> result =
                rTreeIndex.getResidingRegionId(trialSet, new Coordinate(29.41804, 40.89778));

        if(result.isPresent()) {
            System.out.println(result.get());
        }
    }
}
