package spherical;

import spherical.refs.Cartesian;
import spherical.refs.Convex;
import spherical.refs.Halfspace;
import spherical.util.Triple;

public class Circle
{
    /// <summary>
    /// Make a Convex from the parameters of the given circle
    /// </summary>
    /// <param name="ra">Right Ascensionof center</param>
    /// <param name="dec">Declinationof center</param>
    /// <param name="radius">radius : minutes of arc</param>
    /// <returns>a new Convex object</returns>
    public static Convex Make(Double ra, Double dec, Double radius) {
        Triple<Double, Double, Double> result = Cartesian.Radec2Xyz(ra, dec);
        return Make(result.getX(), result.getY(), result.getZ(), radius);
    }
    /// <summary>
    /// Make a Convex from the parameters of the given circle
    /// </summary>
    /// <param name="x">getX coord of center on unit sphere</param>
    /// <param name="y">getY coord of center on unit sphere</param>
    /// <param name="z">getZ coord of center on unit sphere</param>
    /// <param name="radius">radius : arc minutes</param>
    /// <returns>a new Convex object</returns>
    public static Convex Make(Double x, Double y, Double z, Double radius) {
        Convex con = new Convex();
        Double arg = Math.PI * radius / 10800.0;
        Double d;
        d = Math.cos(arg);
        con.Add(new Halfspace(new Cartesian (x, y, z, false), d));
        return con;
    }
}
