package spherical;

public class HtmFace {
    public long hid;
    public char[] name;
    public int vi0, vi1, vi2;

    public HtmFace(long hid, char n0, char n1, int i0, int i1, int i2) {
        this.hid = hid;
        this.name = new char[2];
        this.name[0] = n0;
        this.name[1] = n1;
        this.vi0 = i0;
        this.vi1 = i1;
        this.vi2 = i2;
    }
}
