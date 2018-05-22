package sky;

import edu.jhu.htm.core.Convex;
import edu.jhu.htm.core.Domain;
import edu.jhu.htm.core.HTMindexImp;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.Vector3d;
import edu.jhu.htm.geometry.Chull;
import sky.htm.Converter;

import java.io.Serializable;

public class HtmRegions implements Serializable {

    //Legacy
    public HTMrange generateBarcelonaRegionHTM(Converter converter, int htmDepth) {
        HTMindexImp index = (HTMindexImp) new HTMindexImp();

        Vector3d[] v = new Vector3d[3];
        v[0] = converter.convertLatLongToVector3D(40.000838, 0.007918);//new Vector3d(40.0d, 65.0d);
        v[1] = converter.convertLatLongToVector3D(40.000838, 0.009141);//new Vector3d(40.0d, 85.0d);
        v[2] = converter.convertLatLongToVector3D(40.0, 0.008151);//new Vector3d(44.0d, 45.0d);

        Vector3d[] v2 = new Vector3d[3];
        v2[0] = v[0];
        v2[1] = v[1];
        v2[2] = converter.convertLatLongToVector3D(40.0, 0.008959);//new Vector3d(44.0d, 85.0d);

        Convex convex = new Convex(v[0], v[1], v[2]);
        //convex.simplify();

        Convex convex2 = new Convex(v2[0], v2[1], v2[2]);
        //convex2.simplify();

        Domain domain = new Domain();
        domain.add(convex);
        domain.add(convex2);

        domain.setOlevel(htmDepth);
        HTMrange htmRange = new HTMrange();
        domain.intersect(index, htmRange, false);

        return htmRange;

    }

    //Legacy
    public HTMrange generateConvexOverIstanbulRegion(Converter converter, int htmDepth) {
        HTMindexImp index = (HTMindexImp) new HTMindexImp();

        Vector3d[] v = new Vector3d[4];
        v[0] = converter.convertLatLongToVector3D(41.344030, 28.738862);//new Vector3d(40.0d, 65.0d);
        v[1] = converter.convertLatLongToVector3D(41.216060, 29.588929);//new Vector3d(40.0d, 85.0d);
        v[2] = converter.convertLatLongToVector3D(40.762029, 29.441987);//new Vector3d(44.0d, 45.0d);
        v[3] = converter.convertLatLongToVector3D(40.968700, 28.506776);//new Vector3d(44.0d, 85.0d);

        Convex convex = new Convex(v[0], v[1], v[2], v[3]);
        convex.simplify();

        Domain domain = new Domain();
        domain.add(convex);

        domain.setOlevel(htmDepth);
        HTMrange htmRange = new HTMrange();

        domain.intersect(index, htmRange, false);

        return htmRange;
    }

    //Legacy
    public HTMrange generateConvexOverIstanbulRegionWithChull(Converter converter, int htmDepth) {
        HTMindexImp index = (HTMindexImp) new HTMindexImp();

        Vector3d[] v = new Vector3d[4];
        v[0] = converter.convertLatLongToVector3D(41.344030, 28.738862);//new Vector3d(40.0d, 65.0d);
        v[1] = converter.convertLatLongToVector3D(41.216060, 29.588929);//new Vector3d(40.0d, 85.0d);
        v[2] = converter.convertLatLongToVector3D(40.762029, 29.441987);//new Vector3d(44.0d, 45.0d);
        v[3] = converter.convertLatLongToVector3D(40.968700, 28.506776);//new Vector3d(44.0d, 85.0d);


        Chull c = new Chull();
        c.add(v[0]);
        c.add(v[1]);
        c.add(v[2]);
        c.add(v[3]);

        HTMrange htmRange = new HTMrange();

        c.getDomain().intersect(index, htmRange, false);

        return htmRange;
    }
}
