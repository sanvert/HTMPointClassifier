package spherical;

import spherical.refs.Cartesian;
import spherical.refs.Constant;
import spherical.refs.Convex;
import spherical.refs.Halfspace;

/// <summary>
/// Implements a simple region
/// </summary>
public class Rectangle  {
    // no instances needed, Microsoft.Performance CA1053
    private Rectangle() {
    }

    public static Convex Make(double ra1, double dec1, double ra2, double dec2) {
        //
        // Create four halfspaces. Two great circles for RA and
        // two small circles for DEC

        double dlo, dhi; // offset from center, parameter for constraint
        double declo, dechi;
        double costh, sinth; // sine and cosine of theta (RA) for rotation of vector
        double x, y, z;

        //
        // Halfspaces belonging to declo and dechi are circles parallel
        // to the xy plane, their normal is (0, 0, +/-1)
        //
        // declo halfpsacet is pointing up (0, 0, 1)
        // dechi is pointing down (0, 0, -1)
        if (dec1 > dec2)
        {
            declo = dec2;
            dechi = dec1;
        }
        else
        {
            declo = dec1;
            dechi = dec2;
        }
        dlo = Math.sin(declo * Constant.Degree2Radian);
        dhi = -Math.sin(dechi * Constant.Degree2Radian); // Yes, MINUS!
        Convex con = new Convex(4);
        con.Add(new Halfspace(new Cartesian(0.0, 0.0, 1.0, false), dlo)); // Halfspace #1
        con.Add(new Halfspace(new Cartesian(0.0, 0.0, -1.0, false),dhi)); // Halfspace #1

        costh = Math.cos(ra1 * Constant.Degree2Radian);
        sinth = Math.sin(ra1 * Constant.Degree2Radian);
        x = -sinth;
        y = costh;
        z = 0.0;
        con.Add(new Halfspace(new Cartesian(x, y, z, false), 0.0));// Halfspace #3

        costh = Math.cos(ra2 * Constant.Degree2Radian);
        sinth = Math.sin(ra2 * Constant.Degree2Radian);
        x = sinth;
        y = -costh;
        z = 0.0;

        con.Add(new Halfspace(new Cartesian(x, y, z, false), 0.0));// Halfspace #4
        return con;
    }
}