package spherical.refs;

public class Constant {

    public static String NameSpace = "ivo://voservices.org/spherical";

    public static final String KeywordRegion = "REGION";

    public static final double Degree2Radian = 0.017453292519943295;

    public static final double Tolerance = 2E-08;

    public static final String KeywordConvex = "CONVEX";

    public static final double SafeLimit = 1E-07;

    public static final double Radian2Degree = 1.0 / Degree2Radian;

    public static final double SquareRadian2SquareDegree = Radian2Degree * Radian2Degree;

    public static final double WholeSphereInSquareDegree = 12.566370614359172 * SquareRadian2SquareDegree;

    public static final double Arcmin2Radian = Degree2Radian / 60.0;

    public static final double Radian2Arcmin = 1.0 / Arcmin2Radian;

    public static final double TolHalf = Tolerance / 2.0;

    public static final double SinSafe = Math.sin(SafeLimit);

    public static final double CosSafe = Math.cos(SafeLimit);

    public static final double SinHalf = Math.sin(TolHalf);

    public static final double CosHalf = Math.cos(TolHalf);

    public static final double SinTolerance = Math.sin(Tolerance);

    public static final double CosTolerance = Math.cos(Tolerance);

    public static final double DoublePrecision = Math.pow(2.0, -53.0);

    public static final double DoublePrecision4x = 4.0 * DoublePrecision;

    public static final double DoublePrecision2x = 2.0 * DoublePrecision;

    public static final double TolArea = TolHalf * TolHalf * 3.1415926535897931;

    public static final String Revision = "$Revision: 1.8 $";
}
