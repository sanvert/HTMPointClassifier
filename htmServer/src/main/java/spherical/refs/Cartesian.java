package spherical.refs;

import spherical.util.Pair;
import spherical.util.Triple;

import java.util.List;

public class Cartesian {

    //
    // Fields
    //
    private double x;

    private double y;

    private double z;

    //
    // Properties
    //
    public Double Dec() {
        if (Math.abs(this.z) < 1.0) {
            return Math.asin(this.z) * Constant.Radian2Degree;
        }
        return 90.0 * Math.signum(this.z);
    }

    public Double RA() {
        Pair<Double, Double> result = Cartesian.Xyz2Radec(this.x, this.y, this.z);
        return result.getX();
    }

    public Double Norm() {
        return Math.sqrt(this.Norm2());
    }

    public Double Norm2() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }


    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Double getX() {

        return this.x;
    }


    public Double getY() {

        return this.y;
    }


    public Double getZ() {

        return this.z;
    }


    public static Cartesian NaN() {
        return new Cartesian(Double.NaN, Double.NaN, Double.NaN, false);
    }

    //
    // Constructors
    //
    public Cartesian() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    public Cartesian(double x, double y, double z, boolean normalize) {
        this.x = x;
        this.y = y;
        this.z = z;
        if (normalize) {
            this.Normalize();
        }
    }

    public Cartesian(Cartesian p, boolean normalize) {
        this(p.x, p.y, p.z, normalize);
    }

    public Cartesian(double ra, double dec) {
        Triple<Double, Double, Double> result = Cartesian.Radec2Xyz(ra, dec);
        this.x = result.getX();
        this.y = result.getY();
        this.z = result.getZ();
    }

    public static Pair<Double, Double> Cartesian2Radec(Cartesian p) {
        return Cartesian.Xyz2Radec(p.x, p.y, p.z);
    }

    public static Pair<Double, Double> Cartesian2RadecRadian(Cartesian p) {
        return Cartesian.Xyz2RadecRadian(p.x, p.y, p.z);
    }

    public static Cartesian CenterOfMass(List<Cartesian> plist, boolean normalize) {
        Cartesian result = new Cartesian(0.0, 0.0, 0.0, false);
        for (Cartesian current : plist) {
            result.x += current.x;
            result.y += current.y;
            result.z += current.z;
        }
        if (normalize) {
            result.Normalize();
        }
        return result;
    }

    public static double GirardArea(double a1, double a2, double a3) {
        double s = 0.5 * (a1 + a2 + a3);
        double area = -Math.PI
                + 2 * Math.asin(Math.sqrt(Math.sin(s - a2) * Math.sin(s - a3) / (Math.sin(a2) * Math.sin(a3))))
                + 2 * Math.asin(Math.sqrt(Math.sin(s - a1) * Math.sin(s - a3) / (Math.sin(a1) * Math.sin(a3))))
                + 2 * Math.asin(Math.sqrt(Math.sin(s - a2) * Math.sin(s - a1) / (Math.sin(a2) * Math.sin(a1))))
                ;
        return area;
    }

    public static boolean IsNaN(Cartesian p) {
        return Double.isNaN(p.x) && Double.isNaN(p.y) && Double.isNaN(p.z);
    }

    /// <summary>
    /// Computes the near-optimal bounding sphere of a list of points.
    /// </summary>
    /// <remarks>Based on code by Jack Ritter from Graphics Gems, Academic Press, 1990</remarks>
    /// <param name="plist"></param>
    /// <param name="center"></param>
    /// <param name="radius"></param>
    public static Pair<Cartesian, Double> FindBoundingSphere(List<Cartesian> plist) {
            /*
                An Efficient Bounding Sphere
                by Jack Ritter
                from "Graphics Gems", Academic Press, 1990
            */
        /* Routine to calculate near-optimal bounding sphere over    */
        /* a set of points in 3D */
        /* This contains the routine find_bounding_sphere(), */
        /* the struct definition, and the globals used for parameters. */
        /* The abs() of all coordinates must be < BIGNUMBER */
        /* Code written by Jack Ritter and Lyle Rains. */
        Cartesian center = new Cartesian();
        int i;
        double radius;
        double dx,dy,dz;
        double rad_sq,xspan,yspan,zspan,maxspan;
        double old_to_p,old_to_p_sq,old_to_new;
        Cartesian xmin,xmax,ymin,ymax,zmin,zmax,dia1,dia2;

        /* FIRST PASS: find 6 minima/maxima points */
        xmin = ymin = zmin = new Cartesian(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false);
        xmax = ymax = zmax = new Cartesian(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, false);

        for (i=0;i<plist.size();i++) {
            Cartesian caller_p = plist.get(i);
            if (caller_p.x<xmin.x)  xmin = caller_p;
            if (caller_p.x>xmax.x)  xmax = caller_p;
            if (caller_p.y<ymin.y)  ymin = caller_p;
            if (caller_p.y>ymax.y)  ymax = caller_p;
            if (caller_p.z<zmin.z)  zmin = caller_p;
            if (caller_p.z>zmax.z)  zmax = caller_p;
        }

        /* Set xspan = distance between the 2 points xmin & xmax (squared) */
        dx = xmax.x - xmin.x;
        dy = xmax.y - xmin.y;
        dz = xmax.z - xmin.z;
        xspan = dx*dx + dy*dy + dz*dz;

        /* Same for y & z spans */
        dx = ymax.x - ymin.x;
        dy = ymax.y - ymin.y;
        dz = ymax.z - ymin.z;
        yspan = dx*dx + dy*dy + dz*dz;

        dx = zmax.x - zmin.x;
        dy = zmax.y - zmin.y;
        dz = zmax.z - zmin.z;
        zspan = dx*dx + dy*dy + dz*dz;

        /* Set points dia1 & dia2 to the maximally separated pair */
        dia1 = xmin; dia2 = xmax; /* assume xspan biggest */
        maxspan = xspan;
        if (yspan>maxspan)
        {
            maxspan = yspan;
            dia1 = ymin; dia2 = ymax;
        }
        if (zspan>maxspan)
        {
            dia1 = zmin; dia2 = zmax;
        }

        /* dia1,dia2 is a diameter of initial sphere */
        /* calc initial center */
        center.x = (dia1.x+dia2.x)/2.0;
        center.y = (dia1.y+dia2.y)/2.0;
        center.z = (dia1.z+dia2.z)/2.0;
        /* calculate initial radius**2 and radius */
        dx = dia2.x-center.x; /* x component of radius vector */
        dy = dia2.y-center.y; /* y component of radius vector */
        dz = dia2.z-center.z; /* z component of radius vector */
        rad_sq = dx*dx + dy*dy + dz*dz;
        radius = Math.sqrt(rad_sq);

        /* SECOND PASS: increment current sphere */

        for (i = 0; i < plist.size(); i++)
        {
            Cartesian caller_p = plist.get(i);
            dx = caller_p.x - center.x;
            dy = caller_p.y - center.y;
            dz = caller_p.z - center.z;
            old_to_p_sq = dx * dx + dy * dy + dz * dz;
            if (old_to_p_sq > rad_sq) 	/* do r**2 test first */
            { 	/* this point is outside of current sphere */
                old_to_p = Math.sqrt(old_to_p_sq);
                /* calc radius of new sphere */
                radius = (radius + old_to_p) / 2.0;
                rad_sq = radius * radius; 	/* for next r**2 compare */
                old_to_new = old_to_p - radius;
                /* calc center of new sphere */
                center.x = (radius * center.x + old_to_new * caller_p.x) / old_to_p;
                center.y = (radius * center.y + old_to_new * caller_p.y) / old_to_p;
                center.z = (radius * center.z + old_to_new * caller_p.z) / old_to_p;
                /* Suppress if desired */
                //printf("\n New sphere: cen,rad = %f %f %f   %f",
                //    cen.x,cen.y,cen.z, rad);
            }
        }

        return new Pair<>(center, radius);
    }

    public static Halfspace MinimalEnclosingCircleOptimalSlow(List<Cartesian> plist) {
        Halfspace mec = new Halfspace(new Cartesian(1.0, 0.0, 0.0, false), -1.0, 0.0);
        double angle, minangle;
        boolean found = false;
        minangle = Math.PI;

        double costol, sintol;
        costol = Constant.CosTolerance;
        sintol = Constant.SinTolerance;

        // if a single point -> single point
        if (plist.size() == 1)
            return new Halfspace(plist.get(0), 1.0, 0.0);

        // check diagonals
        Cartesian Pi, Pj, c;
        for (int i = 0; i < plist.size() - 1; i++)
        {
            Pi = plist.get(i);
            for (int j = i + 1; j < plist.size(); j++)
            {
                Pj = plist.get(j);
                // middle point (normalized, of course)
                if (Pi.Same(Pj.Mirrored()))
                {
                    c = Cartesian.CenterOfMass(plist, true);
                } else {
                    c = plist.get(i).GetMiddlePoint(plist.get(j), true);
                }
                double rad = c.AngleInRadian(Pj);
                Halfspace h = new Halfspace(c, Math.cos(rad), Math.sin(rad));
                angle = h.RadiusInRadian();
                if (angle < minangle && h.Contains(plist, costol, sintol)) {
                    found = true;
                    mec = h;
                    minangle = angle;
                }
            }
        }
        // check triangles
        for (int i = 0; i < plist.size() - 1; i++) {
            for (int j = i + 1; j < plist.size(); j++) {
                for (int k = 0; k < plist.size(); k++) {
                    if (k == i || k == j) continue;

                    Halfspace h;
                    try
                    {
                        h = new Halfspace(plist.get(i), plist.get(j), plist.get(k));
                    }
                    catch (IllegalArgumentException e)
                    {
                        continue;
                    }
                    angle = h.RadiusInRadian();
                    if (angle < minangle && h.Contains(plist, costol, sintol))
                    {
                        found = true;
                        mec = h;
                        minangle = angle;
                    }
                    else
                    {
                        h.Invert();
                        angle = h.RadiusInRadian();
                        if (angle < minangle && h.Contains(plist, costol, sintol))
                        {
                            found = true;
                            mec = h;
                            minangle = angle;
                        }
                    }
                }
            }
        }
        if (!found)
            throw new IllegalArgumentException("MinimalEnclosingCircle(): not found! invalid input?");

        return mec;
    }

    public static Halfspace MinimalEnclosingCircle(List<Cartesian> plist) {
        Pair<Cartesian, Double> boundingSRes = FindBoundingSphere(plist);
        Cartesian center = boundingSRes.getX();
        double radius = boundingSRes.getY();

        Cartesian n = center;
        double norm2 = n.Norm2();
        double norm = Math.sqrt(norm2);
        n.Scale(1 / norm);

        double r2 = radius * radius;
        double d = (1 + norm2 - r2) / norm / 2;

        /*
        if (center.Same(new Cartesian())) Console.Error.WriteLine("Center is 0");
        if (norm2 > 1) Console.Error.WriteLine("p2 > 1 !?!");
        if (r2 > 1) Console.Error.WriteLine("r2 > 1 !?!");
        if (d < 0) Console.Error.WriteLine("Hole?");
        */
        if (radius >= 1 || d <= 0)
            return Cartesian.MinimalEnclosingCircleOptimalSlow(plist);

        return new Halfspace(n, d);
    }

    public static Cartesian Parse(String repr, boolean normalize) {
        String[] array = repr.split(" ", 3);
        double num = Double.parseDouble(array[0]);
        double num2 = Double.parseDouble(array[1]);
        double num3 = Double.parseDouble(array[2]);
        return new Cartesian(num, num2, num3, normalize);
    }

    public static Triple<Double, Double, Double> Radec2Xyz(double ra, double dec) {
        double x, y, z;
        double epsilon = Constant.DoublePrecision2x;

        double diff;
        double cd = Math.cos(dec * Constant.Degree2Radian);
        diff = 90.0 - dec;
        // First, compute Z, consider cases, where declination is almost
        // +/- 90 degrees
        if (diff < epsilon && diff > -epsilon)
        {
            x = 0.0;
            y = 0.0;
            z = 1.0;
            return new Triple<>(x, y, z);
        }
        diff = -90.0 - dec;
        if (diff < epsilon && diff > -epsilon)
        {
            x = 0.0;
            y = 0.0;
            z = -1.0;
            return new Triple<>(x, y, z);
        }
        z = Math.sin(dec * Constant.Degree2Radian);
        //
        // If we get here, then
        // at least z is not singular
        //
        double quadrant;
        double qint;
        int iint;
        quadrant = ra / 90.0; // how close is it to an integer?
        // if quadrant is (almost) an integer, force x, y to particular
        // values of quad:
        // quad,   (x,y)
        // 0       (1,0)
        // 1,      (0,1)
        // 2,      (-1,0)
        // 3,      (0,-1)
        // q>3, make q = q mod 4, and reduce to above
        // q mod 4 should be 0.
        qint = (double)((int)quadrant);
        if (Math.abs(qint - quadrant) < epsilon) {
            iint = (int)qint;
            iint %= 4;
            if (iint < 0) iint += 4;
            switch (iint) {
                case 0:
                    x = cd;
                    y = 0.0;
                    break;
                case 1:
                    x = 0.0;
                    y = cd;
                    break;
                case 2:
                    x = -cd;
                    y = 0.0;
                    break;
                case 3:
                default:
                    x = 0.0;
                    y = -cd;
                    break;
            }
        } else {
            x = Math.cos(ra * Constant.Degree2Radian) * cd;
            y = Math.sin(ra * Constant.Degree2Radian) * cd;
        }

        return new Triple<>(x, y, z);
    }

    public static Double SphericalTriangleArea(Cartesian p1, Cartesian p2, Cartesian p3) {
        // difference vectors
        Cartesian v1 = new Cartesian(p3.x - p2.x, p3.y - p2.y, p3.z - p2.z, false);
        Cartesian v2 = new Cartesian(p1.x - p3.x, p1.y - p3.y, p1.z - p3.z, false);
        Cartesian v3 = new Cartesian(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z, false);
        // related angles
        double a1 = 2 * Math.asin(0.5 * Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z));
        double a2 = 2 * Math.asin(0.5 * Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z));
        double a3 = 2 * Math.asin(0.5 * Math.sqrt(v3.x * v3.x + v3.y * v3.y + v3.z * v3.z));

        double area;
        double det = Cartesian.TripleProduct(v1, v2, p3);

        if (Math.abs(det) < Math.E) {
            area = 0;
        } else {
            area = GirardArea(a1, a2, a3);
            area *= Constant.SquareRadian2SquareDegree;
            if (det < 0) area *= -1;
        }
        return area;
    }

    public static double TripleProduct(Cartesian p1, Cartesian p2, Cartesian p3) {
        double x, y, z; // cross product of 1 and 2
        x = p1.y * p2.z - p1.z * p2.y;
        y = p1.z * p2.x - p1.x * p2.z;
        z = p1.x * p2.y - p1.y * p2.x;
        return x * p3.x + y * p3.y + z * p3.z;
    }

    public static Pair<Double, Double> Xyz2Radec(double x, double y, double z) {
        double epsilon = Constant.DoublePrecision2x;

        double ra, rdec;
        if (z >= 1) rdec = Math.PI / 2;
        else if (z <= -1) rdec = -Math.PI / 2;
        else rdec = Math.asin(z);
        double dec = rdec * Constant.Radian2Degree;

        double cd = Math.cos(rdec);
        if (cd > epsilon || cd < -epsilon)  // is the vector pointing to the poles?
        {
            if (y > epsilon || y < -epsilon)  // is the vector in the x-z plane?
            {
                double arg = x / cd;
                double acos;
                if (arg <= -1)
                {
                    acos = 180;
                }
                else if (arg >= 1)
                {
                    acos = 0;
                }
                else
                {
                    acos = Math.acos(arg) * Constant.Radian2Degree;
                }
                if (y < 0.0)
                {
                    ra = 360 - acos;
                }
                else
                {
                    ra = acos;
                }
            }
            else
            {
                ra = (x < 0.0 ? 180.0 : 0.0);
            }
        }
        else
        {
            ra = 0.0;
        }

        return new Pair<>(ra, dec);
    }

    public static Pair Xyz2RadecRadian(Double x, Double y, Double z) {
        Pair raDecPair = new Pair();
        double DoublePrecision2x = Constant.DoublePrecision2x;
        double num;
        if (z >= 1.0) {
            num = 1.5707963267948966;
        } else if (z <= -1.0) {
            num = -1.5707963267948966;
        } else {
            num = Math.asin(z);
        }
        raDecPair.setY(num);
        double num2 = Math.cos(num);
        if (num2 <= DoublePrecision2x && num2 >= -DoublePrecision2x) {
            raDecPair.setX(0.0);
            return raDecPair;
        }
        if (y <= DoublePrecision2x && y >= -DoublePrecision2x) {
            raDecPair.setX((x < 0.0) ? 3.1415926535897931 : 0.0);
            return raDecPair;
        }
        double num3 = x / num2;
        double num4;
        if (num3 <= -1.0) {
            num4 = 3.1415926535897931;
        } else if (num3 >= 1.0) {
            num4 = 0.0;
        } else {
            num4 = Math.acos(num3);
        }
        if (y < 0.0) {
            raDecPair.setX(6.2831853071795862 - num4);
            return raDecPair;
        }
        raDecPair.setX(num4);
        return raDecPair;
    }

    public Cartesian Add(Cartesian that) {
        return new Cartesian(this.x + that.x, this.y + that.y, this.z + that.z, false);
    }

    public Double AngleInArcmin(Cartesian p) {
        return this.AngleInRadian(p) * Constant.Radian2Arcmin;
    }

    public Double AngleInDegree(Cartesian p) {
        return this.AngleInRadian(p) * Constant.Radian2Degree;
    }

    public Double AngleInRadian(Cartesian p) {
        double a, d;
        d = 0.5 * Distance(p);
        if (d < 1) {
            a = 2 * Math.asin(d);
        } else {
            a = Math.PI;
        }
        return a;
    }

    public Cartesian Cross(Cartesian p, boolean normalize) {
        return new Cartesian(
                y * p.z - z * p.y,
                z * p.x - x * p.z,
                x * p.y - y * p.x,
                normalize);
    }

    public double Distance(Cartesian p) {
        double dx, dy, dz;
        dx = x - p.x;
        dy = y - p.y;
        dz = z - p.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double Dot(Cartesian p) {
        return this.x * p.x + this.y * p.y + this.z * p.z;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof Cartesian) {
            Cartesian right = (Cartesian) o;
            return this.x == right.getX() && this.y == right.getY() && this.z == right.getZ();
        }

        return false;
    }

    public Cartesian GetMiddlePoint(Cartesian that, boolean normalize) {
        return new Cartesian(this.x + that.x, this.y + that.y, this.z + that.z, normalize);
    }

    public void Mirror() {
        this.x *= -1.0;
        this.y *= -1.0;
        this.z *= -1.0;
    }

    public Cartesian Mirrored() {
        return new Cartesian(-1 * x, -1 * y, -1 * z, false);
    }

    public double Normalize() {
        double lenSqr = x * x + y * y + z * z;
        double err = lenSqr - 1;
        double len = 1.0;

        if (err > Constant.DoublePrecision4x || err < -Constant.DoublePrecision4x)
        {
            len = Math.sqrt(lenSqr);
            x /= len;
            y /= len;
            z /= len;
            return len;
        } else {
            return 1.0;
        }
    }

    public boolean Same(Cartesian that) {
        if (this.equals(that)) return true;

        // quick reject based on cosine
        if (this.Dot(that) < Constant.CosSafe) return false;

        if (this.AngleInRadian(that) < Constant.Tolerance) return true;

        else return false;
    }

    public void Scale(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public Cartesian Scaled(double s) {
        Cartesian result = new Cartesian(this, false);
        result.Scale(s);
        return result;
    }

    public void Set(Cartesian v, boolean normalize) {
        this.Set(v.x, v.y, v.z, normalize);
    }

    public void Set(Double ra, Double dec) {
        Triple<Double, Double, Double> result = Cartesian.Radec2Xyz(ra, dec);
        this.x = result.getX();
        this.y = result.getY();
        this.z = result.getZ();
    }

    public void Set(Double x, Double y, Double z, boolean normalize) {
        this.x = x;
        this.y = y;
        this.z = z;
        if (normalize) {
            this.Normalize();
        }
    }

    public void SetMiddlePoint(Cartesian p, Cartesian q, boolean normalize) {
        this.x = p.x + q.x;
        this.y = p.y + q.y;
        this.z = p.z + q.z;
        if (normalize) {
            this.Normalize();
        }
    }

    public Cartesian Sub(Cartesian that) {
        return new Cartesian(this.x - that.x, this.y - that.y, this.z - that.z, false);
    }

    public Pair<Cartesian, Cartesian> Tangent() {
        double sinRa, cosRa, sinDec, cosDec;

        Pair<Double, Double> result = Cartesian.Cartesian2RadecRadian(this);
        double ra = result.getX();
        double dec = result.getY();
        sinRa = Math.sin(ra);
        cosRa = Math.cos(ra);
        sinDec = Math.sin(dec);
        cosDec = Math.cos(dec);

        Cartesian west = new Cartesian(sinRa, -cosRa, 0.0, false);
        Cartesian north = new Cartesian(-sinDec * cosRa, -sinDec * sinRa, cosDec, false);

        return new Pair<>(west, north);
    }

    public double Get (int i)
    {
        switch (i) {
            case 0:
                return this.x;
            case 1:
                return this.y;
            case 2:
                return this.z;
            default:
                throw new IndexOutOfBoundsException ("Cartesian");
        }
    }

    @Override
    public String toString() {
        return String.format("{0:%f} {1:%f} {2:%f}", this.x, this.y, this.z);
    }
}
