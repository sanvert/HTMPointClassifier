package spherical.refs;

public class DArcChange {
    //
    // Fields
    //
    public Double angle;

    public DArc darc;

    public int change;

    //
    // Properties
    //
    public boolean isStart() throws Exception {
        int x = this.change * this.darc.dir;
        switch (x) {
            case -1:
                return false;
            case 1:
                return true;
        }
        throw new Exception("change not +/- 1?");
    }

    public Cartesian point() throws Exception {
        switch (this.change) {
            case -1:
                return this.darc.arc.getPoint2();
            case 1:
                return this.darc.arc.getPoint1();
        }
        throw new Exception("change not +/- 1?");
    }

    //
    // Constructors
    //
    public DArcChange(DArc darc, int change, Double angle) {
        this.darc = darc;
        this.change = change;
        this.angle = angle;
    }

    //
    // Static Methods
    //
    static int comparisonAngle(DArcChange a, DArcChange b) {
        return a.angle.compareTo(b.angle);
    }

    //
    // Methods
    //
    @Override
    public String toString() {
        return this.change + " - angle:" + this.angle;
    }
}
