package edu.util;


import com.vividsolutions.jts.algorithm.locate.IndexedPointInAreaLocator;
import com.vividsolutions.jts.algorithm.locate.PointOnGeometryLocator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Location;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.geojson.geom.GeometryJSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RTreeIndex implements Serializable {

    private final Map<Integer, PointOnGeometryLocator> polygonIndexMap = new HashMap<>();

    private final RegionHTMIndex regionHTMIndex;

    public RTreeIndex(final RegionHTMIndex regionHTMIndex) {
        this.regionHTMIndex = regionHTMIndex;

        try {
            create();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildGeometryTree() {
//        final STRtree tree = new STRtree();
//        List<Geometry> gList = LargePolygonSplitter.split(g.getGeometryN(0).getGeometryN(0), 0.000005);
//        gList.forEach(geom -> tree.insert(geom.getEnvelopeInternal(), geom));
//        tree = FacetSequenceTreeBuilder.build(g);
    }

    private void create() throws IOException {

        getResources("geojson/").stream().forEach(resource -> {
            try(final InputStream is = resource.openStream()) {
                String[] pathSplit = resource.toString().split("/");
                String regionName = pathSplit[pathSplit.length - 1].split("_")[0].toLowerCase();
                GeometryJSON geometryJSON = new GeometryJSON(12);
                try {
                    GeometryCollection g = (GeometryCollection) geometryJSON
                            .read(is);
                    MultiPolygon mp = (MultiPolygon) g.getGeometryN(0);
                    //Polygon p = (Polygon) g.getGeometryN(0).getGeometryN(0);
                    PointOnGeometryLocator areaLocator = new IndexedPointInAreaLocator(mp);
                    polygonIndexMap.put(regionHTMIndex.getCityMap().get(regionName), areaLocator);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//        final Map<String, String> env = new HashMap<>();
//        env.put( "create", "true" );
//        final FileSystem fs = FileSystems
//                .newFileSystem(URI.create("jar:" + getClass().getResource("/geojson").toString()), env);


        //final Path paths = Paths.get(RTreeIndex.class.getResource("/geojson").toURI());

//        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(paths, "*.json")) {
//            stream.forEach(path -> {
//                String regionName = path.getFileName().toString().split("_")[0].toLowerCase();
//                GeometryJSON geometryJSON = new GeometryJSON(12);
//                try {
//                    GeometryCollection g = (GeometryCollection) geometryJSON
//                            .read(new FileInputStream(path.toFile()));
//                    MultiPolygon mp = (MultiPolygon) g.getGeometryN(0);
//                    //Polygon p = (Polygon) g.getGeometryN(0).getGeometryN(0);
//                    PointOnGeometryLocator areaLocator = new IndexedPointInAreaLocator(mp);
//                    polygonIndexMap.put(regionHTMIndex.getCityMap().get(regionName), areaLocator);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//        }
    }

    public static List<URL> getResources(final String path) throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (
                final InputStream is = loader.getResourceAsStream(path);
                final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr)) {
            return br.lines()
                    .map(l -> loader.getResource(path  + l))
                    .collect(Collectors.toList());
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
    public static void main(String[] args) throws IOException {
        String regionFile = "regionsHTM.json";
        final RegionHTMIndex regionHTMIndex = new RegionHTMIndex(regionFile);
        final RTreeIndex rTreeIndex = new RTreeIndex(regionHTMIndex);

        final Set<Integer> trialSet = new HashSet<>();
        trialSet.add(1);

        Optional<Integer> result =
                rTreeIndex.getResidingRegionId(trialSet, new Coordinate(29.061475, 41.049022));

        if(result.isPresent()) {
            System.out.println(result.get());
        }
    }
}
