package spherical;

import spherical.refs.Cartesian;
import spherical.refs.Convex;
import spherical.refs.Halfspace;
import spherical.refs.Region;
import spherical.util.Pair;
import spherical.util.Triple;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private enum Format {
        J2000,
        Cartesian,
        Latlon,
        Null,
        Unkown
    }

    private enum Geometry {
        Region,
        Convex,
        Halfspace, /* n/a */
        Rect,
        Circle,
        Poly,
        Chull,
        Null
    }

    /// <summary>
    /// Descibes the Spherical Shape Parser error
    /// </summary>
    public enum Error {
        /// <summary>
        /// All is well, no error
        /// </summary>
        Ok,
        /// <summary>
        /// The parsed token was not a number.
        /// </summary>
        errIllegalNumber,
        /// <summary>
        /// Premature end of line was reached
        /// </summary>
        errEol,
        /// <summary>
        /// There were other than 2 points specified for a RECT command.
        /// </summary>
        errNot2forRect,
        /// <summary>
        /// There weren't at least 3 points for specifying a polygon or convex hull.
        /// </summary>
        errNotenough4Poly,
        /// <summary>
        /// The given polygon is either concave, or has self-intersecting edges
        /// </summary>
        errPolyBowtie,
        /// <summary>
        /// The polygon specified contains edges of zero length (eg, from consecutive
        /// identical points).
        /// </summary>
        errPolyZeroLength,
        /// <summary>
        /// The list of points for convex hull generation span a space larger than a hemisphere.
        /// </summary>
        errChullTooBig,
        /// <summary>
        /// The list of points for convex hull generation are all on the same great circle.
        /// </summary>
        errChullCoplanar,
        /// <summary>
        /// An empty string was given as a specification.
        /// </summary>
        errNullSpecification,
        /// <summary>
        /// Some other error occured. Please call someone if this happens
        /// </summary>
        errUnknown
    }

    private final int INITIAL_CAPACITY = 30;
    private int _current_capacity = INITIAL_CAPACITY;
    private List<Double> xs, ys, zs, ras, decs;
    private String _spec;
    private String _delims;
    private String[] _tokens;
    private int _ct;
    private Error _error;
    private Format _format;
    private Geometry _geometry; // The kind of object we are building
    private Region _targetregion;        // Everything is a region, it will be built to here....
    private boolean beginAccumulate;

    private Parser() {
        this(null);
    }
    /// <summary>
    /// Create an instance of a shape parser with a specification
    ///
    /// Parsing occurs when parse() is invoked.
    /// </summary>
    /// <param name="in_spec">a valid shape specification</param>
    public Parser(String inSpec) {
        init();
        setSpec(inSpec);
    }

    private void init() {
        _delims = " |\\t|\\n|\\r";//" \t\n\r".toCharArray()
        xs = new ArrayList<>(INITIAL_CAPACITY);
        ys = new ArrayList<>(INITIAL_CAPACITY);
        zs = new ArrayList<>(INITIAL_CAPACITY);
        ras = new ArrayList<>(INITIAL_CAPACITY);
        decs = new ArrayList<>(INITIAL_CAPACITY);
        beginAccumulate = false;
    }

    private void setSpec(String input) {
        _error = Error.Ok;
        _format = Format.Unkown;
        _geometry = Geometry.Null;
        _ct = 0;
        _spec = input;
        if (_targetregion != null) {
            _targetregion.Clear();
        }
        if (_spec == null) {
            _tokens = null;
        } else if (_spec.length() > 0) {
            _tokens = _spec.split(_delims);
        } else {
            _tokens = null;
        }
    }

    private String getSpec() {
        return _spec;
    }

    public void setRegion(final Region _targetregion) {
        this._targetregion = _targetregion;
    }

    /// <summary>
    /// True, if there are any tokens left to parse. ismore() skips over white spaces
    /// </summary>
    private boolean ismore() {
        if (_tokens == null)
            return false;
        if (_tokens[0] == null)
            return false;

        while (_ct < _tokens.length && _tokens[_ct].length() == 0) {
            _ct++;
        }
        if (_ct < _tokens.length && beginAccumulate) {
            try {
                Double.parseDouble(_tokens[_ct]);
            } catch (Exception e) {
                return false;
            }
        }
        return _ct < _tokens.length;
    }

    /// <summary>
    /// Is the current token same as pattern?
    /// </summary>
    /// <param name="pattern"></param>
    /// <param name="casesensitive"></param>
    /// <returns></returns>
    private boolean match_current(String pattern, boolean casesensitive) {
        if (!ismore()) {
            return false;
        }
        if (casesensitive) {
            return (_tokens[_ct].equals(pattern));
        } else {
            return (_tokens[_ct].toLowerCase().equals(pattern));
        }
    }

    private Geometry peekGeometry() {
        // Peek into the string, and return the type of geometric object
        // the parser will build.
        if (_tokens == null)
            return Geometry.Null;
        if (match_current("convex", false))
            return Geometry.Convex;
        if (match_current("region", false))
            return Geometry.Region;
        if (match_current("halfspace", false))
            return Geometry.Halfspace;
        if (match_current("poly", false))
            return Geometry.Poly;
        if (match_current("chull", false))
            return Geometry.Chull;
        if (match_current("rect", false))
            return Geometry.Rect;
        if (match_current("circle", false))
            return Geometry.Circle;
        return Geometry.Null;
    }

    private void advance() {
        _ct++;
    }

    private void clearPoints() {
        xs.clear();
        ys.clear();
        zs.clear();
        ras.clear();
        decs.clear();
    }

    private Format peekFormat() {

        // Peek into the string, and return the type of geometric object
        // the parser will build.

        if (_tokens == null)
            return Format.Null;
        if (match_current("cartesian", false)) {
            return Format.Cartesian;
        } else if (match_current("j2000", false)) {
            return Format.J2000;
        } else if (match_current("latlon", false)) {
            return Format.Latlon;
        } else {
            return Format.Null;
        }
    }

    private Double getdouble() throws Exception {
        Double res;
        if (!ismore()) {
            _error = Error.errEol;
            //throw new ParserException(Error.errEol);
            throw new Exception(Error.errEol.toString());
        }
        res = Double.parseDouble(_tokens[_ct]);
        advance();
        return res;
    }

    private Triple<Double, Double, Double> getNormalVector() throws Exception {
        Double x = getdouble();
        Double y = getdouble();
        Double z = getdouble();
        double norm = x * x + y * y + z * z;
        norm = Math.sqrt(norm);
        x /= norm;
        y /= norm;
        z /= norm;
        return new Triple<>(x, y, z);
    }

    /************************************* ***************** *************************/
    /************************************* SPECIALTY PARSERS *************************/
    /************************************* ***************** *************************/
    /* ******************************************** RECT ***/
    private Error parse_rectangle() {

        Region region = _targetregion;
        int npoints = 0;
        double ra, dec, x, y, z;

        if (region == null)
            return Error.Ok;
        advance(); // Skip over 'RECT'
        _format = peekFormat();
        if (ismore() && _format != Format.Null) {
            advance();
        }
        while (ismore()) {
            switch (_format) {
                case Null:
                case Cartesian:
                    try {
                        Triple<Double, Double, Double> norm = getNormalVector();
                        x = norm.getX();
                        y = norm.getY();
                        z = norm.getZ();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    xs.add(x);
                    ys.add(y);
                    zs.add(z);
                    Pair<Double, Double> raDec = Cartesian.Xyz2Radec(x, y, z);
                    ra = raDec.getX();
                    dec = raDec.getY();
                    ras.add(ra);
                    decs.add(dec);
                    npoints++;
                    break;
                case J2000:
                case Latlon:
                    try {
                        ra = this.getdouble();
                        dec = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    ras.add(ra);
                    decs.add(dec);
                    npoints++;
                    break;
                default:
                    break;
            }
            if (npoints >= 2) {
                break;
            }
        }
        if (npoints != 2) {
            _error = Error.errNot2forRect;
            return _error;
        }
        //
        // Rectangle's static methods will create a new convex
        // 
        //
        // WAS:rect = new Rectangle(region);
        Convex con;
        if (_format == Format.Latlon) {
            con = Rectangle.Make(decs.get(0), ras.get(0), decs.get(1), ras.get(1));
        } else {
            con = Rectangle.Make(ras.get(0), decs.get(0), ras.get(1), decs.get(1));
        }
        _targetregion.Add(con);
        return Error.Ok;
    }

    /************************** END RECT *******************************/
    /* ******************************************** POLY ***/
    private Error parse_polygon() {

        int npoints = 0;
        Double ra, dec, x, y, z;

        if (_targetregion == null)
            return Error.Ok;

        advance(); // Skip over "poly"
        _format = peekFormat();
        if (ismore() && _format != Format.Null) {
            advance();
        }
        while (ismore()) {
            //Problematic
//            if (npoints >= this._current_capacity) {
//                increaseCapacity();
//            }
            switch (_format) {
                case Null:
                case Cartesian:
                    try {
                        Triple<Double, Double, Double> norm = getNormalVector();
                        x = norm.getX();
                        y = norm.getY();
                        z = norm.getZ();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    xs.add(x);
                    ys.add(y);
                    zs.add(z);
                    npoints++;
                    beginAccumulate = true;
                    break;
                case J2000:
                case Latlon:
                    try {
                        ra = this.getdouble();
                        dec = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    if (_format == Format.Latlon) {
                        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(dec, ra);
                        x = res.getX();
                        y = res.getY();
                        z = res.getZ();
                    } else {
                        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(ra, dec);
                        x = res.getX();
                        y = res.getY();
                        z = res.getZ();
                    }
                    xs.add(x);
                    ys.add(y);
                    zs.add(z);
                    npoints++;
                    beginAccumulate = true;
                    break;
                default:
                    break;
            }
        }
        if (npoints < 3) {
            _error = Error.errNotenough4Poly;
            return _error;
        }
        // poly = new Polygon(region);
        // WARNING!!! more than one kinf of error. The other one is for
        // degenerate edges, two points are too close... or you can eliminate them!

        Pair<Convex, Polygon.Error> polygonRes = Polygon.Make(xs, ys, zs, npoints);
        Polygon.Error err = polygonRes.getY();
        Convex con = polygonRes.getX();
        if (con != null) {
            _targetregion.Add(con);
        }
        // = poly.make(xs, ys, zs, npoints);
        if (err == Polygon.Error.errBowtieOrConcave) {
            _error = Error.errPolyBowtie;
            return _error;
        } else if (err == Polygon.Error.errZeroLength) {
            _error = Error.errPolyZeroLength;
            return _error;
        }
        beginAccumulate = false;
        return Error.Ok;
    }
    /************************** END POLYGON *******************************/
    /* ******************************************** CHULL ***/
    private Error parse_chull() throws Exception {
        Region region = _targetregion;
        int npoints = 0;
        double ra, dec, x, y, z;

        if (region == null) {
            _error = Error.Ok;
            return _error;
        }
        advance(); // Skip over 'CHULL'
        _format = peekFormat();
        if (ismore() && _format != Format.Null) {
            advance();
        }
        while (ismore()) {


            switch (_format) {
                case Null:
                case Cartesian:
                    try {
                        Triple<Double, Double, Double> norm = getNormalVector();
                        x = norm.getX();
                        y = norm.getY();
                        z = norm.getZ();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    xs.add(x);
                    ys.add(y);
                    zs.add(z);
                    npoints++;
                    break;
                case J2000:
                case Latlon:
                    try {
                        ra = this.getdouble();
                        dec = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    if (_format == Format.Latlon) {
                        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(dec, ra);
                        x = res.getX();
                        y = res.getY();
                        z = res.getZ();
                    } else {
                        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(ra, dec);
                        x = res.getX();
                        y = res.getY();
                        z = res.getZ();
                    }
                    xs.add(x);
                    ys.add(y);
                    zs.add(z);
                    npoints++;
                    break;
                default:
                    break;
            }
        }
        if (npoints < 3) {
            _error = Error.errNotenough4Poly;
            return _error;
        }

        Pair<Convex, Chull.Cherror> chullRes = Chull.Make(xs, ys, zs);
        Chull.Cherror err = chullRes.getY();
        Convex con = chullRes.getX();
        if (con != null) {
            _targetregion.Add(con);
        }

        if (err == Chull.Cherror.BiggerThanHemisphere) {
            _error = Error.errChullTooBig;
            return _error;
        } else if (err == Chull.Cherror.Coplanar) {
            _error = Error.errChullCoplanar;
            return _error;
        }

        /* */
        //hull = new Chull(region);
        //if (hull.addCloud(xs, ys, zs) == false) {
        //    _error = Error.errChullTooBig;
        //    return _error;
        //}
        return Error.Ok;
    }

    /************************** END CHULL *******************************/
    /* ******************************************** CIRCLE ***/
    private Error parse_circle() {

        Region region = _targetregion;
        // Circle circle = null;
        Convex con;
        double ra, dec, x, y, z, rad;

        if (region == null)
            return Error.Ok;
        advance(); // Skip over 'CIRCLE'
        _format = peekFormat();
        if (ismore() && _format != Format.Null) {
            advance();
        }
        if (ismore()) {
            switch (_format) {
                case Null:
                case Cartesian:
                    try {
                        Triple<Double, Double, Double> norm = getNormalVector();
                        x = norm.getX();
                        y = norm.getY();
                        z = norm.getZ();
                        rad = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    con = Circle.Make(x, y, z, rad);
                    _targetregion.Add(con);
                    break;
                case J2000:
                case Latlon:
                    try {
                        ra = this.getdouble();
                        dec = this.getdouble();
                        rad = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    if (_format == Format.J2000) {
                        con = Circle.Make(ra, dec, rad);
                    } else {
                        con = Circle.Make(dec, ra, rad);

                    }
                    _targetregion.Add(con);
                    break;
                default:
                    break;
            }
        }
        return Error.Ok;
    }

    /************************** END CIRCLE *******************************/
    // ////////////////////////////// parse CONVEX
    private Error parse_convex() {

        Region region = _targetregion;
        Convex convex = null;
        Geometry nextitem;
        double ra, dec, x, y, z, D;

        if (region == null) {
            _error = Error.Ok;
            return _error;
        }
        advance(); // Skip over 'CONVEX'
        _format = peekFormat();
        if (ismore() && _format != Format.Null) {
            advance();
        }
        convex = new Convex();
        region.Add(convex);
        while (ismore()) {
            nextitem = peekGeometry();
            if (nextitem != Geometry.Null)
                break;
            switch (_format) {
                case Null:
                case Cartesian:
                    try {
                        Triple<Double, Double, Double> norm = getNormalVector();
                        x = norm.getX();
                        y = norm.getY();
                        z = norm.getZ();
                        D = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    convex.Add(new Halfspace(new Cartesian(x, y, z, false), D));
                    break;
                case J2000:
                case Latlon:
                    try {
                        ra = this.getdouble();
                        dec = this.getdouble();
                        D = this.getdouble();
                    } catch (Exception e) {
                        if (_error != Error.errEol)
                            _error = Error.errIllegalNumber;
                        return _error;
                    }
                    if (_format == Format.J2000) {
                        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(ra, dec);
                        x = res.getX();
                        y = res.getY();
                        z = res.getZ();
                    } else {
                        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(dec, ra);
                        x = res.getX();
                        y = res.getY();
                        z = res.getZ();
                    }
                    convex.Add(new Halfspace(new Cartesian(x, y, z, false), D));
                    break;
                default:
                    break;
            }
        }
        return Error.Ok;
    }

    /// <summary>
    /// Extracts all the points from a convex hull specification
    /// </summary>
    /// <param name="textSpec"></param>
    /// <returns>a linear array of x, y, z values</returns>
    public static List<Double> extract(String textSpec) throws Exception {
        Region reg = new Region();
        Parser par = new Parser();
        Parser.Error err;
        par.setSpec(textSpec);
        par.setRegion(reg); // tell parser where to put the finished product
        err = par.parse();

        if (err != Error.Ok) {
            throw new Exception(err.toString());//ParserException
        }
        List<Double> result = null;
        if (par._geometry == Parser.Geometry.Chull) {
            result = new ArrayList<>(par.xs.size());
            for (int i = 0; i < par.xs.size(); i++) {
                result.add(par.xs.get(i));
                result.add(par.ys.get(i));
                result.add(par.zs.get(i));
            }
        }
        return result;
    }


    private Error parse() throws Exception {
        // smarter parser, allows mixed geometries and object
        // Skip the first 'REGION', else assume that
        boolean multiple = false; // parse a region, use multiple, else just one object

        if (!ismore()) {
            return Error.errNullSpecification;
        }
        _geometry = peekGeometry();
        if (_geometry == Geometry.Region) {
            advance();
            multiple = true;
        }
        while (ismore()) {            // We loop on each CONVEX, whatever it may be...
            _geometry = peekGeometry();
            this.clearPoints();
            switch (_geometry) {
                case Circle:
                    if (parse_circle() != Error.Ok) {
                        return _error; // Adds circle convex to _targetreion
                    }
                    break;
                case Convex:
                    if (parse_convex() != Error.Ok) {
                        return _error;
                    }
                    break;
                case Rect:
                    if (parse_rectangle() != Error.Ok) {
                        return _error;
                    }
                    break;
                case Poly:
                    if (parse_polygon() != Error.Ok) {
                        return _error;
                    }
                    break;
                case Chull:
                    if (parse_chull() != Error.Ok) {
                        return _error;
                    }
                    break;
                case Null:
                    _error = Error.errUnknown;
                    return _error;
                // break;
            }
            if (!multiple)
                break;
        }
        return _error;
    }

    /// <summary>
    /// Compile a text description of a region into a new Region object.
    ///
    /// The Shape Grammar is given in these
    /// <a href="../../HtmPrimer/regiongrammar.html">specification.</a>
    /// If there is an error in the specification, compile will
    /// throw a ParserException with an appropriate text message, and and internal
    /// Parser.Error variable.
    /// </summary>
    /// <param name="textSpec"></param>
    /// <returns></returns>
    public static Region compile(String textSpec) throws Exception {
        Region reg = new Region();
        Parser par = new Parser();
        Parser.Error err;
        par.setSpec(textSpec);
        par._targetregion = reg; // tell parser where to put the finished product
        err = par.parse();

        if (err != Error.Ok) {
            throw new Exception(err.toString());
        }
        // reg.SimpleSimplify();
        return reg;
    }
    // after you make an instance with par = new Parser(spec);
    // par.region = null;
    // par.parse... if region is null, then no Region is generated.
    // you can then extract the getX getY getX list
}
