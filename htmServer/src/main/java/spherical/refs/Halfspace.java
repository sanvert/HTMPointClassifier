package spherical.refs;

import spherical.util.Pair;
import spherical.util.Triple;

import java.util.List;

public class Halfspace {
    public static final Halfspace UnitSphere = new Halfspace(new Cartesian (0.0, 0.0,1.0,false), 
            -1.0,0.0);

    public static final String Revision ="$Revision: 1.57 $";

    //
    // Fields
    //
    private Cartesian vector;

    private double cos0;

    private double sin0;

    public Double getCos0() {
        return cos0;
    }

    public Double getSin0() {
        return sin0;
    }

    //
    // Properties
    //
    public Double Area()

    {
        return 6.2831853071795862 * (1.0 - this.cos0) * Constant.SquareRadian2SquareDegree;
    }


    public ESign getESign()

    {
        Double DoublePrecision = Constant.DoublePrecision;
        if (this.cos0 > DoublePrecision) {
            return ESign.Positive;
        }
        if (this.cos0 < -DoublePrecision) {
            return ESign.Negative;
        }
        return ESign.Zero;
    }

    public Double RadiusInArcmin()

    {

        return this.RadiusInRadian() * Constant.Radian2Arcmin;

    }

    public Double RadiusInDegree()

    {

        return this.RadiusInRadian() * Constant.Radian2Degree;
    }


    public Double RadiusInRadian() {
        double rad;
        if (cos0 >= 1)
            rad = 0;
        else if (cos0 <= -1)
            rad = Math.PI;
        else
            rad = Math.acos(cos0);
        return rad;
    }


    public Cartesian Vector()

    {
        return this.vector;
    }

    //
    // Constructors
    //
    public Halfspace(Halfspace h) {
        this.vector = new Cartesian(h.Vector(), false);
        this.cos0 = h.getCos0();
        this.sin0 = h.getSin0();
    }

    public Halfspace(Cartesian center, double cos0, double sin0) {
        this.vector = center;
        this.cos0 = cos0;
        this.sin0 = sin0;
    }

    public Halfspace(Double x, Double y, Double z, boolean normalize, Double cos0, Double sin0) {
        this(new Cartesian(x, y, z, normalize), cos0, sin0);
    }

    public Halfspace(Double x, Double y, Double z, boolean normalize, Double cos0) {
        this(new Cartesian(x, y, z, normalize), cos0, Math.sqrt(1.0 - cos0 * cos0));
    }

    public Halfspace(Cartesian center, Double cos0) {
        this(center, cos0, Math.sqrt(1.0 - cos0 * cos0));
    }

    public Halfspace(Double ra, Double dec, Double arcmin) {
        this.vector = new Cartesian(ra, dec);
        double num = arcmin * Constant.Arcmin2Radian;
        this.cos0 = Math.cos(num);
        this.sin0 = Math.sin(num);
    }

    public Halfspace(Cartesian p0, Cartesian p1, Cartesian p2) {
        Cartesian v1 = new Cartesian (p1.getX()-p0.getX(), p1.getY()-p0.getY(), p1.getZ()-p0.getZ(), false);
        Cartesian v2 = new Cartesian (p2.getX()-p0.getX(), p2.getY()-p0.getY(), p2.getZ()-p0.getZ(), false);
        this.vector = v1.Cross(v2, true);
        this.cos0 = this.vector.Dot(p0);
        this.sin0 = Math.sqrt(1 - cos0 * cos0);

        if (this.cos0 < 0)
        {
            this.Invert();
        }
    }

    //
    // Static Methods
    //
    static int ComparisonRadiusXYZ(Halfspace h, Halfspace g) {
        int num = g.getCos0().compareTo(h.cos0);
        if (num != 0) {
            return num;
        }
        num = h.Vector().getX().compareTo(g.Vector().getX());
        if (num != 0) {
            return num;
        }
        num = h.Vector().getY().compareTo(g.Vector().getY());
        if (num != 0) {
            return num;
        }
        return h.Vector().getZ().compareTo(g.Vector().getZ());
    }

    public static Halfspace Invert(Halfspace h) {
        return h.Inverse();
    }

    public static Halfspace Parse(String repr, boolean normalize) {
        String separator = " ";
        String text = repr.trim().replace(',', ' ')
                .replace("\r\n", " ")
                .replace('\n', ' ').replace('\t', ' ');
        String text2 = "";
        while (text2 != text) {
            text2 = text;
            text = text2.replace("  ", " ");
        }
        String[] array = text2.split(separator, 4);
        double x = Double.parseDouble(array[0]);
        double y = Double.parseDouble(array[1]);
        double z = Double.parseDouble(array[2]);
        double num;
        double num2;
        try {
            num = Double.parseDouble(array[3]);
            num2 = Math.sqrt(1.0 - num * num);
        } catch (Exception e) {
            String[] array2 = array[3].split(separator, 2);
            num = Double.parseDouble(array2[0]);
            num2 = Double.parseDouble(array2[1]);
        }
        return new Halfspace(new Cartesian(x, y, z, normalize), num, num2);
    }

    //
    // Methods
    //
    boolean Contains(List<Cartesian> list, Double costol, Double sintol) {
        for(Cartesian current : list) {
            if (!this.Contains(current, costol, sintol)) {
                return false;
            }
        }
        return true;
    }

    public boolean Contains(Cartesian p, double costol, double sintol) {
        if (this.IsAll()) {
            return true;
        }
        if (this.cos0 + 1 < Constant.DoublePrecision) {
            return true;
        }
        boolean res;

        double costhres = cos0 * costol - sin0 * sintol;
        double d, err;
        if (this.sin0 > Constant.SinSafe) {
            d = p.Dot(vector);
            err = d - costhres;
            res = err > 0;
        } else {
            d = p.Distance(vector);
            double thres = 2 * Math.sqrt((1 - costhres) / 2);
            err = d - thres;
            res = err < 0;
        }
        double ang = this.vector.AngleInRadian(p);
        if (ang < this.RadiusInRadian() + Constant.Tolerance) {
            res = true;
        } else {
            res = false;
        }
        Cartesian x = this.vector.Cross(p, false);
        double cos = this.vector.Dot(p);
        double sin = x.Norm();
        double sinDiff = sin * this.cos0 - cos * this.sin0;
        if (sinDiff < sintol) res = true;

        return res;
    }

    public boolean equals(Halfspace right) {
        return this.vector.equals(right.vector)
                && this.getCos0().equals(right.cos0)
                && this.getSin0().equals(right.sin0);
    }

    @Override
    public boolean equals(Object right) {
        if (right == null) {
            return false;
        }
        if (this == right) {
            return true;
        }
        if (this.getClass() != right.getClass()) {
            return false;
        }
        Halfspace right2 = (Halfspace) right;
        return this.equals(right2);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    //Problematic
    public Cartesian GetPointWest() {
        double sin0 = Math.sin(this.RadiusInRadian());
        Pair<Double, Double> xyzRadecRes =
                Cartesian.Xyz2Radec(this.vector.getX(), this.vector.getY(), this.vector.getZ());
        double sinRA = Math.sin(xyzRadecRes.getX() * Constant.Degree2Radian);
        double cosRA = Math.cos(xyzRadecRes.getY() * Constant.Degree2Radian);
        double x = vector.getX() * cos0 + sinRA * sin0;
        double y = vector.getY() * cos0 - cosRA * sin0;
        double z = vector.getZ() * cos0;
        Cartesian w = new Cartesian(x, y, z, false);
        return w;
    }

    public Triple<Topo, Integer, Pair<Cartesian, Cartesian>> GetTopo(Halfspace h) {
        return this.GetTopoRes(h);
    }

//    public Topo GetTopo(Halfspace h, out Cartesian pos, out Cartesian neg) {
//        Triple<Topo, Integer, Pair<Cartesian, Cartesian> result = this.GetTopoRes(h);
//        return result.getX();
//    }

    public Topo GetTopo(Cartesian p, Double sintol) {
        double num = this.vector.Dot(p);
        if (num > this.cos0 + Constant.SafeLimit) {
            return Topo.Inner;
        }
        if (num < this.cos0 - Constant.SafeLimit) {
            return Topo.Outer;
        }
        double a = this.vector.AngleInRadian(p);
        double num2 = Math.sin(a);
        double num3 = num2 * this.cos0 - num * this.sin0;
        if (num3 > -sintol && num3 < sintol) {
            return Topo.Same;
        }
        if (num > this.cos0) {
            return Topo.Inner;
        }
        return Topo.Outer;
    }

    public Topo GetTopo(Cartesian p) {
        return this.GetTopo(p, Constant.SinTolerance);
    }

    private Triple<Topo, Integer, Pair<Cartesian, Cartesian>> GetTopoRes(Halfspace h) {

        Cartesian pos = Cartesian.NaN();
        Cartesian neg = Cartesian.NaN();
        int flag = -99;
        if (this.equals(h)) {
            return new Triple(Topo.Same, flag, new Pair<>(pos, neg));
        }

        Halfspace hinv = h.Inverse();
        if (this == hinv)  return new Triple(Topo.Inverse, flag, new Pair<>(pos, neg));

        double angle = this.Vector().AngleInRadian(h.Vector());
        double r1 = this.RadiusInRadian();
        double r2 = h.RadiusInRadian();

        // quick rejects with safe threshold in radian
        double quick_margin = Constant.SafeLimit;

        double d = angle - r1 - r2;
        double c1 = r1 - angle - r2;
        double c2 = r2 - angle - r1;

        if (d > quick_margin) {
            return new Triple(Topo.Disjoint, flag, new Pair<>(pos, neg));
        }

        // if c1 > "0": h1 contains h2
        if (c1 > quick_margin) {
            return new Triple(Topo.Outer, flag, new Pair<>(pos, neg));
        }

        if (c2 > quick_margin) {
            return new Triple(Topo.Inner, flag, new Pair<>(pos, neg));
        }

        Pair<Integer, Pair<Cartesian, Cartesian>> rootsRes = this.Roots(h);
        flag = rootsRes.getX();
        pos = rootsRes.getY().getX();
        neg = rootsRes.getY().getY();

        // co-linear vectors
        if (flag == 0) {
            boolean same_direction = this.vector.Dot(h.vector) > 0;
            double coserr;

            coserr = this.cos0 - h.cos0;
            if (same_direction
                    && coserr < Constant.DoublePrecision4x
                    && coserr > -Constant.DoublePrecision4x)
            {
                return new Triple(Topo.Same, flag, new Pair<>(pos, neg));
            }

            coserr = this.cos0 + h.cos0;
            if (!same_direction
                    && coserr < Constant.DoublePrecision4x
                    && coserr > -Constant.DoublePrecision4x) {
                return new Triple(Topo.Inverse, flag, new Pair<>(pos, neg));
            }
        }

        if (flag == 2) return new Triple(Topo.Intersect, flag, new Pair<>(pos, neg));
        if (d > 0) return new Triple(Topo.Disjoint, flag, new Pair<>(pos, neg));
        if (c1 > 0) return new Triple(Topo.Outer, flag, new Pair<>(pos, neg));
        if (c2 > 0) return new Triple(Topo.Inner, flag, new Pair<>(pos, neg));

        return new Triple(Topo.Overlap, flag, new Pair<>(pos, neg));
    }

    public Pair<Boolean, Pair<Cartesian, Cartesian>> GetXLine (Halfspace that)
    {
        Cartesian xdir = this.vector.Cross (that.vector, false);
        Cartesian xpt;

        Cartesian dir2 = new Cartesian(xdir.getX() * xdir.getX(), xdir.getY() * xdir.getY(), xdir.getZ() * xdir.getZ(), false);
        double invdet, zero = Constant.DoublePrecision2x;
        if (dir2.getZ() > dir2.getY() && dir2.getZ() > dir2.getX() && dir2.getZ() > zero)
        {
            /* find xpt on X-Y plane */
            invdet = 1.0 / xdir.getZ();
            xpt = new Cartesian(
                    -this.vector.getY() * that.cos0 + that.vector.getY() * this.cos0,
                    -that.vector.getX() * this.cos0 + this.vector.getX() * that.cos0,
                    0.0, false);
        } else if (dir2.getY() > dir2.getX() && dir2.getY() > zero) {
            /* find on X-Z */
            invdet = -1 / xdir.getY();
            xpt = new Cartesian(
                    -this.vector.getZ() * that.cos0 + that.vector.getZ() * this.cos0, 0.0,
                    -that.vector.getX() * this.cos0 + this.vector.getX() * that.cos0, false);
        } else if (dir2.getX() > zero) {
            invdet = 1 / xdir.getX();
            xpt = new Cartesian(0.0,
                    -this.vector.getZ() * that.cos0 + that.vector.getZ() * this.cos0,
                    -that.vector.getY() * this.cos0 + this.vector.getY() * that.cos0, false);
        } else {
            xpt = Cartesian.NaN();
            return new Pair(false, new Pair(xdir, xpt));
        }

        xpt.Scale(invdet);
        invdet = 1.0 / Math.sqrt(dir2.getX() + dir2.getY() + dir2.getZ());
        xdir.Scale(invdet);

        return new Pair(true, new Pair(xdir, xpt));
    }

    public Halfspace Grow(Double arcmin) {
        double num = Math.acos(this.cos0) + arcmin * Constant.Arcmin2Radian;
        if (num > Math.PI) {
            throw new IllegalArgumentException("Halfspace.Grow(): radius bigger than Pi");
        }
        if (num < 0.0) {
            throw new IllegalArgumentException("Halfspace.Grow(): radius less than 0");
        }
        this.cos0 = Math.cos(num);
        this.sin0 = Math.sin(num);
        return this;
    }

    public Halfspace Inverse() {
        return new Halfspace(this.vector.Mirrored(), -this.cos0, this.sin0);
    }

    public void Invert() {
        this.vector.Mirror();
        this.cos0 *= -1.0;
    }

    boolean IsAll(Double sinhalf) {
        return this.cos0 < -0.9999 && this.sin0 < sinhalf;
    }

    public boolean IsAll() {
        return this.IsAll(Constant.SinHalf);
    }

    boolean IsEmpty(Double sinhalf) {
        return this.cos0 > 0.9999 && this.sin0 < sinhalf;
    }

    public boolean IsEmpty() {
        return this.IsEmpty(Constant.SinHalf);
    }

    public Pair<Integer, Pair<Cartesian, Cartesian>> Roots (Halfspace that) {
        Cartesian pos;
        Cartesian neg;

        double g = this.vector.Dot(that.vector);
        if (g - 1 > -Constant.DoublePrecision4x || g + 1 < Constant.DoublePrecision4x) {
            pos = neg = Cartesian.NaN();
            return new Pair<>(0, new Pair(pos, neg));
        }

        Pair<Boolean, Pair<Cartesian, Cartesian>> xLineRes = this.GetXLine(that);
        Cartesian v = xLineRes.getY().getX();
        Cartesian r0 = xLineRes.getY().getY();
        if (!xLineRes.getX()) {
            pos = neg = Cartesian.NaN();
            return new Pair<>(0, new Pair(pos, neg));
        }
        double a = v.Dot(v); // 1;
        double b = 2.0 * r0.Dot(v);
        double c = 2.0 +
                (r0.getX() - 1) * (r0.getX() + 1) +
                (r0.getY() - 1) * (r0.getY() + 1) +
                (r0.getZ() - 1) * (r0.getZ() + 1);

        double d = b * b - 4.0 * a * c;
        if (d < 0) {
            pos = neg = Cartesian.NaN();
            return new Pair<>(-1, new Pair(pos, neg));
        }
        double sign = (b < 0.0 ? -1.0 : 1.0);
        d = Math.sqrt(d);
        double q = -0.5 * (b + (sign * d));

        if (a == 0) {
            pos = neg = Cartesian.NaN();
            return new Pair<>(-2, new Pair(pos, neg));
        }
        if (q == 0) {
            pos = neg = Cartesian.NaN();
            return new Pair<>(-3, new Pair(pos, neg));
        }

        pos = r0.Add(v.Scaled(q / a)); // r0.Add(v.Scaled(q));
        neg = r0.Add(v.Scaled(c / q));

        double det = v.Dot(pos);
        if (det < 0.0) {
            Cartesian t = pos;
            pos = neg;
            neg = t;
        }

        return new Pair<>(2, new Pair(pos, neg));
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean outsin) {
        String text = this.vector + "-" + this.cos0;
        if (outsin) {
            text += String.format(" {0:%f}", this.sin0);
        }
        return text;
    }
}