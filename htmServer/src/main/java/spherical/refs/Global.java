package spherical.refs;

public class Global {
    //
    // Static Fields
    //
    private static final Global instance = new Global();

    public static final String Revision = "$Revision: 1.8 $";

    //
    // Fields
    //
    public static int MAXITER;

    //
    // Static Properties
    //
    public static Global Parameter()

    {
        return Global.instance;
    }


    //
    // Properties
    //
    public int MaximumIteration()

    {
        return this.MAXITER;
    }

    //
    // Constructors
    //

    private Global() {
        this.MAXITER = 20000;
    }

    //
    // Methods
    //
    @Override
    public String toString() {
        return String.format ("Maximum number of iterations: {%d}", this.MAXITER);
    }
}
