package spherical;

import spherical.refs.Cartesian;
import spherical.util.Pair;
import spherical.util.Triple;

public class HtmState {

    private String _versionstring = "Spherical.HTM 3.1.2 (Release - Build 3-29-2007)";
    private int _hdelta = 2;

    private int _deltalevel = 0;    // used : interactive experimentation, developer
    private long _uniqueID = 0L; // each halfspace may have a unique number
    private int _times = 0;

    private int _minlevel;
    private int _maxlevel;

    private double[] _featuresizes;
    static final HtmState instance = new HtmState();

    /// FOR MAKING STATIC HTMLOOKUP
    public int[] Xfaces;
    public HtmFace[] faces;
    Cartesian[] originalPoints;

    HtmState()
    {
        faces = new HtmFace[8];
        faces[0] = new HtmFace(8, 'S', '0', 1, 5, 2);
        faces[1] = new HtmFace(9, 'S', '1', 2,5,3);
        faces[2] = new HtmFace(10,'S', '2',3,5,4);
        faces[3] = new HtmFace(11, 'S', '3', 4,5,1);
        faces[4] = new HtmFace(12, 'N', '0', 1,0,4);
        faces[5] = new HtmFace(13, 'N', '1', 4,0,3);
        faces[6] = new HtmFace(14, 'N', '2', 3,0,2);
        faces[7] = new HtmFace(15, 'N', '3', 2,0,1);

        originalPoints = new Cartesian[6];
        originalPoints[0] = new Cartesian( 0.0,  0.0,  1.0, false);
        originalPoints[1] = new Cartesian( 1.0,  0.0,  0.0, false);
        originalPoints[2] = new Cartesian( 0.0,  1.0,  0.0, false);
        originalPoints[3] = new Cartesian(-1.0,  0.0,  0.0, false);
        originalPoints[4] = new Cartesian( 0.0, -1.0,  0.0, false);
        originalPoints[5] = new Cartesian( 0.0,  0.0, -1.0, false);

        _featuresizes = new double[] {
                1.5707963267949,        //0
                0.785398163397448,      //1
                0.392699081698724,      //2
                0.196349540849362,
                0.098174770424681,
                0.0490873852123405,     //5
                0.0245436926061703,
                0.0122718463030851,
                0.00613592315154256,
                0.00306796157577128,
                0.00153398078788564,   //10
                0.000766990393942821,
                0.00038349519697141,
                0.000191747598485705,
                9.58737992428526E-05,
                4.79368996214263E-05,  //15
                2.39684498107131E-05,
                1.19842249053566E-05,
                5.99211245267829E-06,
                2.99605622633914E-06,
                1.49802811316957E-06,  //20
                7.49014056584786E-07,  //21
                3.74507028292393E-07,  //22
                0.0,
                -1.0 // to stop search beyond 0
        };

    }

    public Cartesian Originalpoint(int i) {
        return originalPoints[i];
    }

    public Pair<Long, Triple<Integer, Integer, Integer>> Face(int i) {
        Triple<Integer, Integer, Integer> out = new Triple<>(this.faces[i].vi0, this.faces[i].vi1, this.faces[i].vi2);
        return new Pair<>(this.faces[i].hid, out);
    }


    public static HtmState getInstance() {
        return instance;
    }

    public int times() {
        return _times;
    }

    public int minlevel()

    {
        return _minlevel;
    }

    public int maxlevel()

    {
        return _maxlevel;
    }


    public int hdelta()

    {
        return _hdelta;
    }

    public int deltalevel()

    {
        return _deltalevel;
    }

    public long newID() {
        _times++;
        _uniqueID++;
        return _uniqueID;
    }

    public String getVersion() {
        return _versionstring;
    }

    public int getLevel(Double radius) {
        int i = 0;
        while (this._featuresizes[i] > radius) {
            i++; // there is a -1 at the end, will force a break
        }
        return i;
    }
}
