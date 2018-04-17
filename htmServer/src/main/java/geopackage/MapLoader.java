package geopackage;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.geom.GeoPackageGeometryData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;

public class MapLoader {
    
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
