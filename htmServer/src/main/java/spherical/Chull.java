package spherical;

/// <summary>
///
/// Computes the convex hull of a collection of points
/// on the surface of the sphere. Uses QuickHull3D
/// </summary>

import spherical.quickhull.Face;
import spherical.quickhull.QuickHull3D;
import spherical.refs.Cartesian;
import spherical.refs.Convex;
import spherical.refs.Halfspace;
import spherical.util.Pair;

import java.util.List;

public class Chull {
    /// <summary>
    /// Various error conditions for unsuccesful convex hull creation
    /// </summary>
    public enum Cherror {
        /// <summary>
        /// operation was succesful
        /// </summary>
        Ok,
        /// <summary>
        /// points were coplanar, convex hull undefined
        /// </summary>
        Coplanar,
        /// <summary>
        /// points can not be confined to any hemisphere, convex hull undefined
        /// </summary>
        BiggerThanHemisphere,
        /// <summary>
        /// The required number of points were not present
        /// </summary>
        NotEnoughPoints,
        /// <summary>
        /// Other error, not defined
        /// </summary>
        Unkown
    }
    /// <summary>
    /// Create a convex spherical polygon from the spherical convex hull of the given points
    /// </summary>
    /// <param name="xs">list of x coords</param>
    /// <param name="ys">list of y coords</param>
    /// <param name="zs">list of z coords</param>
    /// <param name="err">error code</param>
    /// <returns>a new Convex object</returns>
    public static Pair<Convex, Cherror> Make(List<Double> xs, List<Double> ys, List<Double> zs) throws Exception {
        return PointsToConvex(xs, ys, zs);
    }
    /// <summary>
    /// Do not allow default constructor
    /// </summary>
    private Chull(){
    }

    public static Pair<Convex, Cherror> ToConvex(List<Cartesian> points) throws Exception {
        Cartesian origin = new Cartesian(0.0, 0.0, 0.0, false);
        QuickHull3D qh = new QuickHull3D();
        Cartesian planeNormal;
        Convex con = null;
        Cherror err;
        //
        // Ingest points into input
        //
        if(points.size() < 3) {
            err = Cherror.NotEnoughPoints;
            return new Pair(con, err); // Should still be null
        }
        err = Cherror.Ok; // benefit of doubt;
        //
        // Add the origin too
        //
        points.add(new Cartesian(0.0, 0.0, 0.0, false));
        try {
            qh.build(points);
            // See if the origin is in it
            //
            //
            for (Face face : qh.GetFaceList()) {
                //
                //
                double D = face.distanceToPlane(origin);
                if (D >= -Trixel.DblTolerance) {

                    planeNormal = face.getNormal();
                    if (con == null) {
                        con = new Convex();
                    }
                    con.Add(new Halfspace(-planeNormal.getX(),
                            -planeNormal.getY(),
                            -planeNormal.getZ(), true, 0.0));
                }
            }
            if (con == null) {
                err = Cherror.BiggerThanHemisphere;
            }
        } catch (QuickHull3D.CoplanarException e) {
            err = Cherror.Coplanar;
        }

        return new Pair(con, err);
    }
    /// <summary>
    /// Internal function for which Make(...) serves as a public wrapper
    /// </summary>
    /// <param name="xs">list of x coorsd</param>
    /// <param name="ys">list of y coords</param>
    /// <param name="zs">list of z coords</param>
    /// <param name="err">error code</param>
    /// <returns></returns>
    static Pair<Convex, Cherror> PointsToConvex(List<Double> xs, List<Double> ys, List<Double> zs) throws Exception {
        Cartesian origin = new Cartesian(0.0, 0.0, 0.0, false);
        QuickHull3D qh = new QuickHull3D();
        Cartesian planeNormal;
        Convex con = null;
        Cherror err;
        //
        // Ingest points into input
        //
        if(xs.size() < 3) {
            err = Cherror.NotEnoughPoints;
            return new Pair(con, err); // Should still be null
        }
        err = Cherror.Ok; // benefit of doubt;
        //
        // Add the origin too
        //
        xs.add(0.0);
        ys.add(0.0);
        zs.add(0.0);
        try {
            qh.build(xs, ys, zs);
            // See if the origin is in it
            //
            //
            for (Face face : qh.GetFaceList()) {
                //
                //
                double D = face.distanceToPlane(origin);
                if (D >= -Trixel.DblTolerance) {

                    planeNormal = face.getNormal();
                    if (con == null) {
                        con = new Convex();
                    }
                    con.Add(new Halfspace(-planeNormal.getX(),
                            -planeNormal.getY(),
                            -planeNormal.getZ(), true, 0.0));
                }
            }
            if (con == null) {
                err = Cherror.BiggerThanHemisphere;
            }
        } catch (QuickHull3D.CoplanarException e) {
            err = Cherror.Coplanar;
        }
        return new Pair(con, err);
    }
}