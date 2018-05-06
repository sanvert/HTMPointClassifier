package sky;

import edu.jhu.htm.core.HTMfunc;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.Vector3d;
import geopackage.MapLoader;
import sky.htm.Converter;
import spherical.util.Pair;

public class GenericTests {

    public static void testConversion() {
        Converter converter = new Converter();
        int arg=0;

        double lat = 52.166083;
        double lng = 4.584162;

        long n = System.currentTimeMillis();
        Vector3d v = converter.convertLatLongToVector3D(lat, lng);
        System.out.println("vector conversion took in msec of " + (System.currentTimeMillis() - n));
        n = System.currentTimeMillis();
        Pair<Double, Double> p = converter.convertVector3DToLatLon(v);
        System.out.println("lat-lng:" + p + " in msec of " + (System.currentTimeMillis() - n));
    }

    public static void geoDBIstanbul(String path) {
        Converter converter = new Converter();
        MapLoader.generateRegiontWithGeoDB(converter, path);
    }

    public static void istanbulRegionTest() {
        int htmDepth = 12;
        Converter converter = new Converter();
        HtmRegions regions = new HtmRegions();
        HTMrange htmRange = regions.generateConvexOverIstanbulRegion(converter, htmDepth);

        try {
            Vector3d pointIn = converter.convertLatLongToVector3D(41.194363, 28.896791);
            long lookupIdIn = HTMfunc.lookupId(pointIn.ra(), pointIn.dec(), htmDepth);
            System.out.println("getPoint in the range:" + htmRange.isIn(lookupIdIn));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Vector3d pointOut =converter.convertLatLongToVector3D(41.183248, 28.582332);
            long lookupIdIn = HTMfunc.lookupId(pointOut.ra(), pointOut.dec(), htmDepth);
            System.out.println("getPoint in the range:" + htmRange.isIn(lookupIdIn));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void barcelonaRegionTest() {
        int htmDepth = 20;
        Converter converter = new Converter();
        HtmRegions regions = new HtmRegions();

        HTMrange htmRange = regions.generateBarcelonaRegionHTM(converter, htmDepth);
        try {
            Vector3d pointIn = converter.convertLatLongToVector3D(40.0007, 0.008);
            System.out.println(pointIn.ra() + " " + pointIn.dec());
            long lookupIdIn = HTMfunc.lookupId(pointIn.ra(), pointIn.dec(), htmDepth);
            System.out.println("getPoint in the range:" + htmRange.isIn(lookupIdIn));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //barcelonaRegionTest();
        //istanbulRegionTest();

        String gadmPath = "/home/tarmur/Downloads/gadm34_TUR.gpkg";
        geoDBIstanbul(gadmPath);
    }
}
