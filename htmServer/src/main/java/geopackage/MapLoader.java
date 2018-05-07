package geopackage;

import edu.jhu.htm.core.Convex;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.manager.GeoPackageManager;
import sky.htm.Converter;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;

public class MapLoader {

    private static BoundingBox getBBofIstanbul() {
        //Istanbul
        double minLatitude = 40.780000;
        double maxLatitude = 41.339800;
        double minLongitude = 28.507700;
        double maxLongitude = 29.441900;

        return new BoundingBox(minLongitude, maxLongitude, minLatitude, maxLatitude);
    }

    //Legacy
    public static Convex generateRegionWithGeoDB(Converter converter, String geoDBFilePath) {

        GeoPackage geoPackage = null;
        try {
            File testFileDB = new File(geoDBFilePath);
            geoPackage = GeoPackageManager.open(testFileDB);
            System.out.println("connected");
            Contents contents = new Contents();
            contents.setBoundingBox(getBBofIstanbul());
            contents.setTableName("TUR_2");
            List<GeoPackageGeometryData> dbGeometryList = readGeometriesFromGeoPackage(geoPackage.getFeatureDao(contents));
            System.out.println("geometries are read");

            GeoPackageGeometryData istanbul = dbGeometryList.get(1);

            return converter.convertMapGeometryIntoConvex(istanbul);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(geoPackage != null) {
                geoPackage.close();
            }
        }
        return null;
    }

    public static List<GeoPackageGeometryData> readGeometriesFromGeoPackage(FeatureDao dao) {

        assertNotNull(dao);

        List<GeoPackageGeometryData> geometryList = new ArrayList<>();
        FeatureResultSet cursor = dao.queryForAll();

        while (cursor.moveToNext()) {
            GeoPackageGeometryData geometryData = cursor.getGeometry();
            if (geometryData != null) {
                System.out.println("GEOM will be processed is type of "
                        + geometryData.getGeometry().getGeometryType());

                geometryList.add(geometryData);
            }
        }

        return geometryList;
    }


}
