package spherical;

import spherical.refs.Cartesian;
import spherical.refs.Convex;
import spherical.refs.Halfspace;
import spherical.util.Pair;

import java.util.List;

public class Polygon {
    public enum Error {
        Ok,
        errZeroLength,
        errBowtieOrConcave,
        errToomanyPoints,
        errUnkown
    }

    static final double Epsilon = 1e-11;

    static Pair<Convex, Error> Make(List<Double> x, List<Double> y, List<Double> z, int len) {
        boolean DIRECTION = false;
        boolean FIRST_DIRECTION = false;
        // The constraint we have for each side is a 0-constraint (great circle)
        // passing through the 2 corners. Since we are in counterclockwise order,
        // the vector product of the two successive corners just gives the correct
        // constraint.
        //
        // Polygons should be counterclockwise
        // Polygons are assumed to be convex, otherwise windingerror is
        // computed wrong.
        int ix;
        Cartesian v = new Cartesian(0.0, 0.0, 1.0, false);
        Convex con = new Convex();
        Error error;
        int i;

        /* PASS 1: check for winding error */

        for (i = 0; i < len; i++)
        {
            // Keep track of winding direction. Should be positive
            // that is, CCW.
            ix = (i == len - 1 ? 0 : i + 1);
            if (i > 0)
            {
                // test third corner against the constraint just formed
                // v is computed in the previous iteration
                // Look at a corner dot v

                if (v.Dot(new Cartesian(x.get(ix), y.get(ix), z.get(ix), false)) < Epsilon)
                {
                    DIRECTION = true;
                    if (i == 1)
                    {
                        FIRST_DIRECTION = true;
                    }
                    // break; 		// Move to pass 2
                }
                else
                {
                    DIRECTION = false;
                    if (i == 1)
                    {
                        FIRST_DIRECTION = false;
                    }
                }
                if (i > 1)
                {
                    if (DIRECTION != FIRST_DIRECTION)
                    {
                        // C++: must clea up new Cartesian and convex
                        // or better yet, do no allocate on top until you know
                        // you need it

                        error = Polygon.Error.errBowtieOrConcave; // BOWTie error
                        return null;
                    }
                }
            }
            // v = corners[i] ^ corners[ i == len-1 ? 0 : i + 1];
            v = (new Cartesian(x.get(i), y.get(i), z.get(i), false))
                    .Cross(new Cartesian(x.get(ix), y.get(ix), z.get(ix), false), false);
            //v.assign(x[i], y[i], z[i]);
            //v.crossMe(x[ix], y[ix], z[ix]);

            if (v.Norm() <= Epsilon)
            {
                error = Polygon.Error.errZeroLength;
                return null;
            }
            // WARNING! if v = zerovector, then edge error!!!
        }
            /* PASS 2: build convex in either original or reverse
               order */

            /* forward:
               Go from 0 to len-1 by +1, cross i and i+1 (or 0)
               reverse:
               Go from len-1 to 0 by -1, cross i and i-1 (or len-1)
            */
        if (DIRECTION)
        {
            for (i = len - 1; i >= 0; i--)
            {
                // v = corners[i] ^ corners[ i == 0 ? len-1 : i-1];
                // v.normalize();
                ix = (i == 0 ? len - 1 : i - 1);
                //v.assign(x[i], y[i], z[i]);
                //v.crossMe(x[ix], y[ix], z[ix]);
                // WARNING! if v = zerovector, then edge error!!!
                v = (new Cartesian(x.get(i), y.get(i), z.get(i), false))
                        .Cross(new Cartesian(x.get(ix), y.get(ix), z.get(ix), false), true);
                //v.Normalize();
                Halfspace c = new Halfspace(v, 0.0);
                // SpatialConstraint c(v,0);
                con.Add(c); ;
            }
        }
        else
        {
            for (i = 0; i < len; i++)
            {

                //v = corners[i] ^ corners[ i == len-1 ? 0 : i+1];
                // v.normalize();
                ix = (i == len - 1 ? 0 : i + 1);
                //v.assign(x[i], y[i], z[i]);
                //v.crossMe(x[ix], y[ix], z[ix]);
                //// WARNING! if v = zerovector, then edge error!!!

                //v.Normalize();
                v = (new Cartesian(x.get(i), y.get(i), z.get(i), false))
                        .Cross(new Cartesian(x.get(ix), y.get(ix), z.get(ix), false), true);
                Halfspace c = new Halfspace(v, 0.0);
                con.Add(c);
            }
        }
        error = Polygon.Error.Ok;
        return new Pair<>(con, error);
    }
}