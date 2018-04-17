package spherical;

import spherical.refs.Cartesian;
import spherical.refs.Constant;
import spherical.util.Pair;
import spherical.util.Triple;

public class Trixel {
    /// 
    // Epsilon hisory:
    // intially      1.0e-11; 
    // then failed   1.0e-15
    // but found ok  1.0e-14
    // fine tuned:   7.17e-16
    /// <summary>
    /// This tolerance is used for comparing angles
    /// </summary>
    public static final double Epsilon2 = 2.0e-8;
    //public static final Double Epsilon = 64.6 * Constant.DoublePrecision;
    //
    /// <summary>
    /// This tolerance is used for comparing fractional roots
    /// </summary>
    public static final double Epsilon = 1.0e-14;
    /// <summary>
    /// Twice the Double precision tolerance
    /// </summary>
    public static final double DblTolerance = 2.0 * Constant.DoublePrecision;
    // WAS: 1e-15;
    private static long
            S0 = 8L, S1 = 9L, S2 = 10L, S3 = 11L,
            N0 = 12L, N1 = 13L, N2 = 14L, N3 = 15L;

    /// <summary>
    /// Number of bits : an HID
    /// </summary>
    static int eHIDBits = 64;

    /// <summary>
    /// Maximum number of characters : text descriptions
    /// Whenever character arrays are used to keep the text
    /// description of a trixel, they must be exactly eMaxNameSize
    /// characters long
    /// </summary>
    public static int TrixelNameLength = 32;
    private static long IDHIGHBIT = 0x2000000000000000L;
    private static long IDHIGHBIT2 = 0x1000000000000000L;
    /// <summary>
    /// Do not instantiate this class
    /// </summary>
    private Trixel() {
    }
    /// <summary>
    /// Convert a normalized Cartesian to a level 20 HtmID.
    /// if the paramater is a Cartesian.Nan, then 0, an invalid HtmID
    /// is returned (there are no integer NaNs).
    /// </summary>
    /// <param name="v"></param>
    /// <returns>64 bit HtmID</returns>
    public static Long CartesianToHid20(Cartesian v) throws Exception {
        if(Cartesian.IsNaN(v))
            return 0L;

        return XyzToNameOrHid(v.getX(), v.getY(), v.getZ(), 20, null).getX();
    }
    /// <summary>
    /// Convert a Cartesian coordinate to a HID.
    /// 
    /// Given a Cartesian coordinate
    /// and a level number, it returns the 64 bit HID.
    /// <b>WARNING!</b>
    /// x, y, z are assumed to be normalized, so this function
    /// doesn't waste time normalzing.
    /// </summary>
    /// <param name="x">getX coordinate of location</param>
    /// <param name="y">getY coordinate of location</param>
    /// <param name="z">getZ coordinate of location</param>
    /// <param name="depth">The level of the HID</param>
    /// <returns>64-bit HID</returns>
    public static Long CartesianToHid(double x, double y, double z, int depth) throws Exception {
        return XyzToNameOrHid(x, y, z, depth, null).getX();
    }
    /// <summary>
    /// Find the string representation of the HtmID to a given level of a unit vector
    /// 
    /// The vector <strong>must</strong> be normalized.
    /// </summary>
    /// <param name="x">Double</param>
    /// <param name="y">Double</param>
    /// <param name="z">Double</param>
    /// <param name="depth">int</param>
    /// <returns>text representation of HtmID</returns>
    /// 
    public static String XyzToHidName(Double x, Double y, Double z, int depth) throws Exception {
        char[] name = new char[TrixelNameLength];
        Pair<Long, Integer> res = XyzToNameOrHid(x, y, z, depth, name);
        return new String(name, 0, res.getY());
    }
    /// <summary>
    /// Convert a HtmID to its textual representation
    /// </summary>
    /// <param name="hid">64-bit HtmID</param>
    /// <returns>The textual representation</returns>
    public static String ToString(Long hid) {
        char[] name = new char[TrixelNameLength];
        int len;
        len = ToName(name, hid);
        return new String(name, 0, len);
    }
    /// <summary>
    /// Decide whether or not a node : the tree of a given HtmID is an
    /// ancestor of another node
    /// </summary>
    /// <param name="grandpa">64-bit HtmID, candidate for ancestry</param>
    /// <param name="hid">64-bit HtmID who is the potential descendant</param>
    /// <returns>true if grandpa is an ancestor of hid</returns>
    public static boolean IsAncestor(Long grandpa, Long hid) {
        int shifts = LevelOfHid(hid) - LevelOfHid(grandpa);
        if(shifts < 0)
            return false;
        long descendant = hid >> (2 * shifts);
        return (descendant == grandpa);
    }

    private static Pair<Long, Triple<Cartesian, Cartesian, Cartesian>> Startpane(
            double xin, double yin, double zin,
            char[] name) {

        long baseID;

        Cartesian v0 = Cartesian.NaN();
        Cartesian v1 = Cartesian.NaN();
        Cartesian v2 = Cartesian.NaN();
        if((xin > 0) && (yin >= 0)) {
            baseID = (zin >= 0) ? N3 : S0;

        } else if((xin <= 0) && (yin > 0)) {
            baseID = (zin >= 0) ? N2 : S1;

        } else if((xin < 0) && (yin <= 0)) {
            baseID = (zin >= 0) ? N1 : S2;

        } else if((xin >= 0) && (yin < 0)) {
            baseID = (zin >= 0) ? N0 : S3;

        } else {
            baseID = (zin >= 0) ? N3 : S0;
        }
        if(baseID <= 0) {
            return new Pair(-1, new Triple(v0, v1, v2));
        }

        HtmState htm = HtmState.getInstance();

        int bix = (int) (baseID - 8L);

        v0 = new Cartesian(htm.originalPoints[htm.faces[bix].vi0], false);
        v1 = new Cartesian(htm.originalPoints[htm.faces[bix].vi1], false);
        v2 = new Cartesian(htm.originalPoints[htm.faces[bix].vi2],  false);

        if(name != null) {
            name[0] = htm.faces[bix].name[0];
            name[1] = htm.faces[bix].name[1];
            name[2] = '\0';
        }
        return new Pair(baseID, new Triple(v0, v1, v2));
    }

    private static Pair<Long, Integer> XyzToNameOrHid(double x, double y, double z, int depth, char[] name)
            throws Exception {

        long topHID;
        long hid;
        Cartesian v0, v1, v2;
        Cartesian w0, w1, w2;
        Cartesian p = new Cartesian(x, y, z, true);
        w0 = Cartesian.NaN();
        w1 = Cartesian.NaN();
        w2 = Cartesian.NaN();
        Pair<Long, Triple<Cartesian, Cartesian, Cartesian>> paneRes = Startpane(x, y, z, name);

        topHID = paneRes.getX();
        v0 = paneRes.getY().getX();
        v1 = paneRes.getY().getY();
        v2 = paneRes.getY().getZ();

        // We have two copies of almost identical code, for speed's sake
        //
        int len = 2;
        if(name == null) {
            if(topHID < 8) {
                hid = 1;
                return new Pair(hid, len);
            }
            hid = topHID;
            //
            // Start searching for the children
            ///
            while(depth-- > 0) {
                hid <<= 2;
                w2.SetMiddlePoint(v0, v1, true);
                w0.SetMiddlePoint(v1, v2, true);
                w1.SetMiddlePoint(v2, v0, true);
                if(Contains(p, v0, w2, w1)) {
                    // hid |= 0;

                    v1.Set(w2, false);
                    v2.Set(w1, false);
                } else if(Contains(p, v1, w0, w2)) {
                    hid |= 1;
                    v0.Set(v1, false);
                    v1.Set(w0, false);
                    v2.Set(w2, false);
                } else if(Contains(p, v2, w1, w0)) {
                    hid |= 2;
                    v0.Set(v2, false);
                    v1.Set(w1, false);
                    v2.Set(w0, false);
                } else if(Contains(p, w0, w1, w2)) {
                    hid |= 3;
                    v0.Set(w0, false);
                    v1.Set(w1, false);
                    v2.Set(w2, false);
                } else {
                    // CATASTROPHIC ERROR!!! THROW Something!
                    throw new Exception("Panic in Cartesian2hid");
                }
            }
        } else {
            if(topHID < 8) {
                name[0] = 'X';
                name[1] = 'X';
                name[2] = '\0';
            }
            hid = topHID;
            //
            // Start searching for the children
            ///

            while(depth-- > 0) {
                w2.SetMiddlePoint(v0, v1, true);
                w0.SetMiddlePoint(v1, v2, true);
                w1.SetMiddlePoint(v2, v0, true);
                if(Contains(p, v0, w2, w1)) {
                    name[len++] = '0';
                    v1.Set(w2, false);
                    v2.Set(w1, false);
                } else if(Contains(p, v1, w0, w2)) {
                    name[len++] = '1';
                    v0.Set(v1, false);
                    v1.Set(w0, false);
                    v2.Set(w2, false);
                } else if(Contains(p, v2, w1, w0)) {
                    name[len++] = '2';
                    v0.Set(v2, false);
                    v1.Set(w1, false);
                    v2.Set(w0, false);
                } else if(Contains(p, w0, w1, w2)) {
                    name[len++] = '3';
                    v0.Set(w0, false);
                    v1.Set(w1, false);
                    v2.Set(w2, false);
                } else {
                    // CATASTROPHIC ERROR!!! THROW Something!
                    throw new Exception("Panic in Cartesian2hid");
                }
            }
            name[len] = '\0';
        }

        return new Pair(hid, len);
    }
    /// <summary>
    /// Convert the location given by (x, y, z) to the symbolic text
    /// name of the HID
    /// <strong>WARNING</strong>:
    /// x, y, z are assumed to be normalized, so this function
    /// doesn't waste time normalizing.
    /// </summary>
    /// <param name="nam">Character array that holds the text for
    /// the trixel's name</param>
    /// <param name="x">getX coordinate of location</param>
    /// <param name="y">getY coordinate of location</param>
    /// <param name="z">getZ coordinate of location</param>
    /// <param name="depth">The level of the HID</param>
    /// <returns>true if conversion was successful</returns>

    /// <summary>
    /// Convert the named trixel to a triangle described
    /// by three vertices.
    /// The vertices are given by three arrays of three Doubles.
    /// The coordinates of the triangles are given in
    /// the order (x, y, z) and so that the location
    /// is on the surface of a unit sphere
    /// </summary>
    /// <param name="name">The text description of the trixel</param>
    /// <param name="c0">The getX coordinate</param>
    /// <param name="c1">The getY coordinate</param>
    /// <param name="c2">The getZ coordinate</param>
    /// <returns>true, if the conversion succeeded, false otherwise</returns>
    public static Pair<Boolean, Triple<Cartesian, Cartesian, Cartesian>> NameToTriangle(char[] name) {
        boolean rstat = false;

        Cartesian v0, v1, v2;
        Cartesian w0, w1, w2;

        //
        // Get the top level hemi-demi-semi space
        //
        int k;

        k = (int)name[1] - '0';
        Cartesian c0 = Cartesian.NaN();
        Cartesian c1 = Cartesian.NaN();
        Cartesian c2 = Cartesian.NaN();
        if(k < 0 || k > 3) {// Do not have a valid name
            return new Pair(rstat, new Triple(c0, c1, c2));
        }
        if(name[0] != 'N' && name[0] != 'S') {
            return new Pair(rstat, new Triple(c0, c1, c2));
        }

        if(name[0] == 'N')
            k += 4;
        // now k is 8-11 for s0-s3, or 12-15 for n0-n3
        //
        HtmState htm = HtmState.getInstance();
        v0 = new Cartesian(htm.originalPoints[htm.faces[k].vi0], false);
        v1 = new Cartesian(htm.originalPoints[htm.faces[k].vi1], false);
        v2 = new Cartesian(htm.originalPoints[htm.faces[k].vi2], false);


        w0 = Cartesian.NaN();
        w1 = Cartesian.NaN();
        w2 = Cartesian.NaN();
        k = 2;
        while(name[k] != '\0') {
            w2.SetMiddlePoint(v0, v1, true);
            w0.SetMiddlePoint(v1, v2, true);
            w1.SetMiddlePoint(v2, v0, true);
            switch(name[k]) {
                case '0':
                    v1.Set(w2, false);
                    v2.Set(w1, false);
                    break;
                case '1':
                    v0.Set(v1, false);
                    v1.Set(w0, false);
                    v2.Set(w2, false);
                    break;
                case '2':
                    v0.Set(v2, false);
                    v1.Set(w1, false);
                    v2.Set(w0, false);
                    break;
                case '3':
                    v0.Set(w0, false);
                    v1.Set(w1, false);
                    v2.Set(w2, false);
                    break;
            }
            k++;
        }
        c0 = new Cartesian(v0, false);
        c1 = new Cartesian(v1, false);
        c2 = new Cartesian(v2, false);
        rstat = true;
        return new Pair(rstat, new Triple(c0, c1, c2));
    }


    /// <summary>
    /// 
    /// </summary>
    /// <param name="v1"></param>
    /// <param name="v2"></param>
    /// <param name="v3"></param>
    /// <param name="xin"></param>
    /// <param name="yin"></param>
    /// <param name="zin"></param>
    /// <param name="name"></param>
    /// <returns></returns>
    private static Pair<Long, Triple<Cartesian, Cartesian, Cartesian>> startpane(Double xin, Double yin, Double zin,
            char[] name) {

        Cartesian v0 = Cartesian.NaN();
        Cartesian v1 = Cartesian.NaN();
        Cartesian v2 = Cartesian.NaN();
        long baseID = 0; // MUST CHANNGE below
        if((xin > 0) && (yin >= 0)) {
            baseID = (zin >= 0) ? N3 : S0;

        } else if((xin <= 0) && (yin > 0)) {
            baseID = (zin >= 0) ? N2 : S1;

        } else if((xin < 0) && (yin <= 0)) {
            baseID = (zin >= 0) ? N1 : S2;

        } else if((xin >= 0) && (yin < 0)) {
            baseID = (zin >= 0) ? N0 : S3;

        } else {
            baseID = (zin >= 0) ? N3 : S0;
        }
        if(baseID <= 0) {
            return new Pair(-1, new Triple(v0, v1, v2));
        }

        HtmState htm = HtmState.getInstance();

        int bix = (int) (baseID - 8L);

        v0 = new Cartesian(htm.originalPoints[htm.faces[bix].vi0], false);
        v1 = new Cartesian(htm.originalPoints[htm.faces[bix].vi1], false);
        v2 = new Cartesian(htm.originalPoints[htm.faces[bix].vi2],  false);

        if(name != null) {
            name[0] = htm.faces[bix].name[0];
            name[1] = htm.faces[bix].name[1];
            name[2] = '\0';
        }
        return new Pair(baseID, new Triple(v0, v1, v2));
    }
    /// <summary>
    /// Test if p is inside triangle given by v1, v2, v3
    /// </summary>
    /// <param name="p">point to test</param>
    /// <param name="v1">first vertex of triangle</param>
    /// <param name="v2">second vertex of triangle</param>
    /// <param name="v3">third vertex of triangle</param>
    /// <returns></returns>
    static boolean Contains(Cartesian p, Cartesian v1, Cartesian v2, Cartesian v3) {
        // if (v1 getX v2) . p < epsilon then false
        // same for v2 getX v3 and v3 getX v1.
        // else return true..
        if(Cartesian.TripleProduct(v1, v2, p) < -Trixel.DblTolerance)
            return false;
        if(Cartesian.TripleProduct(v2, v3, p) < -Trixel.DblTolerance)
            return false;
        if(Cartesian.TripleProduct(v3, v1, p) < -Trixel.DblTolerance)
            return false;
        return true;
    }
    /// <summary>
    /// 
    /// </summary>
    /// <param name="hid"></param>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <param name="c"></param>
    public static Triple<Cartesian, Cartesian, Cartesian> ToTriangle(Long hid) {
        // check for legal hid value

        char[] name = new char[TrixelNameLength];
        ToName(name, hid);
        return NameToTriangle(name).getY();
        // Exceptions
    }
    /// <summary>
    /// Decide if the given 64-bit integer is a valid HtmID
    /// </summary>
    /// <param name="hid">64-bit input number</param>
    /// <returns>true if given number is a valid HtmID, false otherwise</returns>
    public static boolean IsValid(Long hid) {
        int i;
        if (hid < 8) {
            return false;
        }
        // determine index of first set bit
        for(i = 0; i < eHIDBits; i += 2) {
            if(0 != ((hid << i) & IDHIGHBIT))
                break;
            if(0 != ((hid << i) & IDHIGHBIT2))  // invalid id
                return false;
        }
        return true;
    }
    //static Trixel.Name ToName(long hid) {
    //    Trixel.Name result = new Trixel.Name();
    //    Trixel.ToName(result.text, hid);
    //    return result;
    //}
    /// <summary>
    /// Convert a 64-bit HID to a text desciption of the trixel.
    /// </summary>
    /// <param name="name">An array of HtmTrixel.eMaxNameSize (a static : this class)
    /// character The array is null terminated.</param>
    /// <param name="hid">The HID</param>
    /// <returns>The size, or the length of the text</returns>
    public static int ToName(char[] name, long hid) {
        int size = 0, i;
        int c;                                   // a spare character;
        long shifted_bit;
        long shifted_hid;
        // determine index of first set bit, top 2 always assumed 0
        // this is to eliminate the problem of mixing ulongs with longs
        //
        if(hid < 0)
            return -2; // higy bit set

        if(hid < 8)
            return -1; // -1 means Bad hid

        for(i = 2; i < eHIDBits; i += 2) {
            shifted_hid = hid << (i - 2);
            shifted_bit = (shifted_hid) & IDHIGHBIT;
            if(shifted_bit != 0)
                break;
            if((shifted_hid & IDHIGHBIT2) != 0)
                return -1;
        }


        size = (eHIDBits - i) >> 1;
        //
        // fill characters starting with the last one
        //
        for(i = 0; i < size - 1; i++) {
            c = '0' + (int)((hid >> i * 2) & (int)3);
            name[size - i - 1] = (char)c;
        }
        //
        // put : first character
        //
        shifted_bit = (hid >> (size * 2 - 2)) & 1;
        if(shifted_bit != 0) {
            name[0] = 'N';
        } else {
            name[0] = 'S';
        }
        name[size] = '\0'; // end string
        return size;
    }

    /// <summary>
    /// Convert the trixel from text to 64 bit HID.
    /// </summary>
    /// <param name="sname">The string with the text representation of 
    /// the trixel.</param>
    /// <returns>The 64 bit HID</returns>
    public static Long NameToHid(String sname) {
        if (sname != null) {
            int length = sname.length();
            char[] name = sname.toCharArray();
            return NameToHid(name, length);
        } else {
            return 0L;
        }
    }
    /// <summary>
    /// Convert the trixel from text to 64 bit HID
    /// The character array must be of size eMaxNameSize, and
    /// it is not necessary to have null termination. The
    /// number of siginificant characters : the trixel's name
    /// is given as a parameter.
    /// </summary>
    /// <param name="name">The character array with the text 
    /// representation of the trixel.</param>
    /// <param name="effectivelength">The number of significant characters
    /// : the array.</param>
    /// <returns>The 64 bit HID</returns>
    private static Long NameToHid(char[] name, int effectivelength) {
        long result_hid = 0;
        long shifted;
        int i;
        int siz = 0;
        long bits;
        int shift;
        siz = name.length;
        if(name.length < 2)
            return 0L;	// 0 is an illegal HID

        if(name[0] != 'N' && name[0] != 'S')  // invalid name
            return result_hid;
        if(siz > TrixelNameLength)
            return 0L;

        siz = effectivelength;
        for(i = siz - 1; i > 0; i--) {// set bits starting from the end
            if(name[i] > '3' || name[i] < '0') {// invalid name
                return 0L;
            }
            bits = ((int)(name[i] - '0'));
            shift = 2 * (siz - i - 1);
            shifted = bits << shift;
            result_hid += shifted;
        }
        bits = 2;                     // set first pair of bits, first bit always set
        if(name[0] == 'N')
            bits++;      // for north set second bit too
        shift = (2 * siz - 2);
        shifted = bits << shift;
        result_hid += shifted;
        return result_hid;
    }

    /// <summary>
    /// Truncate the HID to fewer bits
    /// The HID of a trixel implicitly contains the level of the trixel.
    /// When you need a lower level trixel that contains the trixel
    /// of the given HID, this function gives it to you.
    /// If the level of
    /// the given htmid is less than or equal to the desired level,
    /// then there is no change.
    /// </summary>
    /// <param name="htmid"></param>
    /// <param name="level"></param>
    /// <returns>HID of lower level trixel</returns>
    public static Long Truncate(Long htmid, int level) {
        Long result = htmid;
        int currentlevel = LevelOfHid(htmid);
        if(level < currentlevel) {
            result = (htmid >> 2 * (currentlevel - level));
        }
        return result;
    }
    /// <summary>
    /// Extend given HID to a desired level.
    /// The opposite of truncate. However,
    /// because there are many descendents, the result is a range of
    /// consecutive HIDs. The low and hi values are returned :
    /// the out variables.
    /// </summary>
    /// <param name="htmid">HID to extend</param>
    /// <param name="level">New level to which to extend</param>
    /// <returns>A pair of 64-bit HtmIDs</returns>
    public static Pair<Long, Long> Extend(Long htmid, int level) {
        Long lo, hi;
        int currentlevel;
        int shiftbits;
        Long dif;
        currentlevel = LevelOfHid(htmid);
        if(level > currentlevel) {
            // amount to extend:
            shiftbits = 2 * (level - currentlevel);
            lo = htmid << shiftbits;
            dif = 1L << shiftbits; // Make sure 64 bit stuff works
            dif--;
            hi = lo + dif;
        } else {
            //truncate
            shiftbits = 2 * (currentlevel - level); // could be 0
            lo = htmid >> shiftbits;
            hi = lo;
        }
        return new Pair(lo, hi);
    }

    /// <summary>
    /// Returns the level number of an HID
    /// <param name="htmid">The HID</param>
    /// <returns>The level number or -1</returns>
    public static int LevelOfHid(Long htmid) {
        int size, i;
        if (htmid < 0) {
            return -1;
        }
        for(i = 2; i < eHIDBits; i += 2) {
            if(0 != ((htmid << (i - 2)) & IDHIGHBIT))
                break;
        }
        size = (eHIDBits - i) / 2;
        return size - 2;
    }
}
