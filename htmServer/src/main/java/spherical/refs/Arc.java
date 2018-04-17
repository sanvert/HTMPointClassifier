package spherical.refs;


public class Arc {

    public static String Revision = "$Revision: 1.39 $";

    private Halfspace circle;

    private Cartesian point1;

    private Cartesian point2;

    private Cartesian middle;

    private Cartesian up;

    private Cartesian wp;

    private double angle;

    private double area;


    public Double Angle()

    {
        return this.angle;
    }

    public Halfspace Circle() {

        return this.circle;
    }

    public boolean IsFull() {
        return this.getPoint1() == this.getPoint2() && !Double.isNaN(this.Angle());
    }

    public Double Length() {
        return this.angle * this.circle.getSin0();
    }

    public Cartesian Middle()

    {
        return this.middle;
    }

    public Cartesian getPoint1()
    {
        return this.point1;
    }

    public Cartesian getPoint2()
    {
        return this.point2;
    }

    public void setPoint1(Cartesian point1) {
        this.point1 = point1;
    }

    public void setPoint2(Cartesian point2) {
        this.point2 = point2;
    }

    public Arc(Arc a) {
        this.circle = a.circle;
        this.point1 = a.point1;
        this.point2 = a.point2;
        this.middle = a.middle;
        this.area = a.area;
        this.angle = a.angle;
        this.up = a.up;
        this.wp = a.wp;
    }

    public Arc(Halfspace circle, Cartesian p) {
        this.circle = circle;
        this.point1 = p;
        this.point2 = p; // Cartesian.NaN;
        this.angle = 2 * Math.PI;

        this.up = p.Sub(circle.Vector().Scaled(circle.getCos0()));
        this.up.Scale(1 / circle.getSin0());
        this.wp = circle.Vector().Cross(up, true);

        Cartesian u = up.Scaled(-circle.getSin0());
        Cartesian n = circle.Vector().Scaled(circle.getCos0());
        this.middle = u.Add(n);

        double sign = circle.getCos0() < 0 ? -1 : 1;
        this.area = 2 * Math.PI * (1 - circle.getCos0() * sign) * sign
                * Constant.SquareRadian2SquareDegree;
    }

    public Arc(Cartesian p1, Cartesian p2) {
        Cartesian center = p1.Cross(p2, false);
        Cartesian cartesian = new Cartesian(center.getX() * center.getX(), center.getY() * center.getY(),
                center.getZ() * center.getZ(), false);
        if (Math.max(cartesian.getX(), Math.max(cartesian.getY(), cartesian.getZ())) <= Constant.DoublePrecision2x) {
            throw new IllegalArgumentException("Arc..ctor(p1,p2): p1 and p2 are co-linear");
        }
        center.Scale(1.0 / Math.sqrt(cartesian.getX() + cartesian.getY() + cartesian.getZ()));
        this.circle = new Halfspace(center, 0.0, 1.0);
        this.point1 = p1;
        this.point2 = p2;
        this.middle = p1.GetMiddlePoint(p2, true);
        this.area = 0.0;
        this.up = p1;
        this.wp = this.circle.Vector().Cross(this.up, true);
        this.angle = p1.AngleInRadian(p2);
    }

    public Arc() {
    }

    public Arc(Halfspace circle, Cartesian p1, Cartesian p2) {
        this.circle = circle;
        this.point1 = p1;
        this.point2 = p2;
        this.up = p1.Sub(circle.Vector().Scaled(circle.getCos0()));
        this.up.Scale(1.0 / circle.getSin0());
        this.wp = circle.Vector().Cross(this.up, true);
        if (p1.Same(p2)) {
            throw new IllegalArgumentException("Arc..ctor(c,p1,p2): p1 and p2 are the same");
        }

        this.area = this.GetAreaSemilune();
        this.angle = this.GetAngle(p2);
        this.middle = this.GetPoint(this.angle / 2.0);
    }

    //
    // Methods
    //
    public Double CalcTriangleArea(Cartesian p) {
        // if full circle, return area
        if (point2 == point1) {
            return this.area;
        }

        // if point is same as one of the endpoints, return area
        if (p.Same(point1) || p.Same(point2)) {
            return this.area;
        }

        // 3 contributions: girard area, semilune and orance slice
        //
        //
        // Girard area: 0 if 2 points are the same

        double area = Cartesian.SphericalTriangleArea(p, this.point1, this.middle)
                + Cartesian.SphericalTriangleArea(p, this.middle, this.point2);

        Arc a = new Arc();
        a.point1 = this.point1;
        a.point2 = this.middle;
        a.circle = this.circle;
        area += 2 * a.GetAreaSemilune();

        return area;
    }

    public boolean containsOnEdge(Cartesian p) {
        Topo t = this.circle.GetTopo(p, Constant.SinTolerance);
        if (t == Topo.Same) {
            if (this.Angle() >= this.GetAngle(p) || this.point1.Same(p) || this.point2.Same(p)) {
                return true;
            }
        }
        return false;
    }

    public Double GetAngle(Cartesian x) {
        double phi = Math.atan2(wp.Dot(x), up.Dot(x));
        if (phi < 0) {
            phi += 2 * Math.PI;
        }
        return phi;
    }

    private Double GetAreaSemilune() {
        double a, b, dist, betahalf, det, area, ratio, sign = 1;
        // if great circle, zero area

        if (circle.getESign() == ESign.Zero) {
            return 0.0;
        }

        Cartesian center = circle.Vector();
        det = Cartesian.TripleProduct(center, point1, point2);

        if (circle.getCos0() < 0) {
            center.Mirror();
            sign = -1;
        }

        // great circ dist of p1 and p2
        dist = 0.5 * point1.Distance(point2);
        if (dist > 1) dist = 1;
        betahalf = Math.asin(dist);
        ratio = Math.tan(betahalf) * circle.getCos0() / circle.getSin0() * sign;
        if (ratio > 1) ratio = 1;
        a = Math.asin(ratio);
        ratio = dist / circle.getSin0();
        if (ratio > 1) ratio = 1;
        b = 2 * Math.asin(ratio);

        // precise area
        area = 2 * a - b * circle.getCos0() * sign;
        if (det < 0) {
            area = 2 * Math.PI * (1 - circle.getCos0() * sign) - area;
        }
        area *= Constant.SquareRadian2SquareDegree;
        if (circle.getCos0() < 0) area *= -1;

        return area;
    }

    public Cartesian GetPoint(Double phi) {
        Cartesian u = this.up;
        u.Scale(Math.cos(phi)*this.circle.getSin0());
        Cartesian w = this.wp;
        w.Scale(Math.sin(phi)*this.circle.getSin0());
        Cartesian n = this.circle.Vector();
        n.Scale(this.circle.getCos0());
        Cartesian x = u.Add(w);
        x = x.Add(n);
        return x;
    }

    public Convex GetWideConvex(Double arcmin) {
        Convex convex = new Convex(4);
        arcmin /= 2.0;
        Halfspace h = this.circle;
        h.Grow(arcmin);
        convex.Add(h);
        Halfspace h2 = this.circle;
        h2.Invert();
        h2.Grow(arcmin);
        convex.Add(h2);
        Cartesian center = this.circle.Vector().Cross(this.point1, true);
        convex.Add(new Halfspace(center, 0.0, 1.0));
        center = this.point2.Cross(this.circle.Vector(), true);
        convex.Add(new Halfspace(center, 0.0, 1.0));
        return convex;
    }

    @Override
    public String toString() {
        return  this.circle + " " + this.point1 + " " + this.point2;
    }

}

