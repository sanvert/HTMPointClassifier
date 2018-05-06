package geopackage;

import edu.jhu.htm.core.Convex;
import mil.nga.geopackage.GeoPackage;
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

    public static void generateRegiontWithGeoDB(Converter converter, String geoDBFilePath) {

        GeoPackage geoPackage = null;
        try {
            File testFileDB = new File(geoDBFilePath);
            geoPackage = GeoPackageManager.open(testFileDB);
            System.out.println("connected");
            List<List<GeoPackageGeometryData>> dbGeometryList = readGeometriesFromGeoPackage(geoPackage);
            System.out.println("geometries are read");

            GeoPackageGeometryData istanbul = dbGeometryList.get(1).get(40);

            Convex convex = converter.convertMapGeometryIntoConvex(istanbul);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(geoPackage != null) {
                geoPackage.close();
            }
        }
    }

    public static List<List<GeoPackageGeometryData>> readGeometriesFromGeoPackage(GeoPackage geoPackage) throws SQLException {
        List<List<GeoPackageGeometryData>> dbGeometryList = new ArrayList<>();
        GeometryColumnsDao geometryColumnsDao = geoPackage.getGeometryColumnsDao();
        if (geometryColumnsDao.isTableExists()) {
            List<GeometryColumns> results = geometryColumnsDao.queryForAll();
            for (GeometryColumns geometryColumns : results) {
                FeatureDao dao = geoPackage.getFeatureDao(geometryColumns);

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
                dbGeometryList.add(geometryList);
            }
        } else {
            System.out.println("GEOM Columns does not exists!");
        }

        return dbGeometryList;
    }


}
