package spherical;

import spherical.refs.Constant;

public class Wedge {
    public static final double Epsilon = 3.0 * Constant.DoublePrecision;
    double lo;
    double hi;
    /// <summary>
    /// Universal wedge, contains everything
    /// </summary>
    Wedge() {
        this.lo = 0.0;
        this.hi = 2.0 * Math.PI;
    }
    /// <summary>
    /// outgoing arc (low) and incoming arc (high)
    /// </summary>
    /// <param name="low"></param>
    /// <param name="high"></param>
    Wedge(double outangle, double inangle) {
        this.lo = outangle;
        if(inangle < outangle) {
            inangle += 2.0 * Math.PI;
        }
        this.hi = inangle;
    }
    /// <summary>
    /// This wedge can either completely or partially overlap the other wedge, or
    /// it's relationship is undefined
    ///
    /// </summary>
    /// <param name="other">other widget to test</param>
    /// <returns>One of Partial, Inner or Undefined</returns>
    Markup Compare(Wedge other) {
        double mylo, myhi, otherlo, otherhi;
        mylo = this.lo;
        myhi = this.hi;
        otherlo = other.lo;
        otherhi = other.hi;

        // find the lowest low, subtract from all
        double bias = mylo < otherlo ? mylo : otherlo;

        mylo -= bias;
        myhi -= bias;
        otherlo -= bias;
        otherhi -= bias;

        if(myhi > 2.0 * Math.PI) {
            myhi -= 2.0 * Math.PI;
            mylo -= 2.0 * Math.PI;
        }
        if(mylo > otherlo + Epsilon && mylo < otherhi - Epsilon)
            return Markup.Partial;
        if(myhi > otherlo + Epsilon && myhi < otherhi - Epsilon)
            return Markup.Partial;

        if(mylo <= otherlo + Epsilon && myhi >= otherhi - Epsilon)
            return Markup.Inner;
        return Markup.Undefined;
    }
}