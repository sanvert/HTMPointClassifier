package spherical;

import spherical.refs.Arc;

public class SmartArc {
    public Arc arc;
    Long Hid1;
    Long Hid2;

    SmartArc(Arc a) throws Exception {
        this.arc = a;
        this.Hid1 = Trixel.CartesianToHid20(a.getPoint1());
        this.Hid2 = Trixel.CartesianToHid20(a.getPoint2());// nan input is ok
    }

    public Arc getArc() {
        return arc;
    }
}
