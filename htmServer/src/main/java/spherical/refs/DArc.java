package spherical.refs;

public class DArc {
    //
    // Fields
    //
    public Arc arc;

    public int dir;

    public double angleStart;

    public double angleEnd;

    public PatchPart segment;

    public Cartesian pointEnd() {
        if (this.dir == -1) {
            return this.arc.getPoint1();
        }
        return this.arc.getPoint2();
    }

    public Cartesian PointStart() {
        if (this.dir == -1) {
            return this.arc.getPoint2();
        }
        return this.arc.getPoint1();
    }

    public Double getAngleStart() {
        return angleStart;
    }

    public Double getAngleEnd() {
        return angleEnd;
    }

    //
    // Constructors
    //
    public DArc(Arc arc, int dir, PatchPart segment) {
        this.arc = arc;
        this.dir = dir;
        this.segment = segment;
    }

    //
    // Static Methods
    //
    static int ComparisonAngle(DArc a, DArc b) {
        int num = a.getAngleStart().compareTo(b.angleStart);
        if (num == 0) {
            num = a.getAngleEnd().compareTo(b.angleEnd);
        }
        return num;
    }

    @Override
    public String toString() {
        return this.dir + ":" + this.angleStart + " " + this.angleEnd;
    }
}
