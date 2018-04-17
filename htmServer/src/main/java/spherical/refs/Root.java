package spherical.refs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Root {
    //
    // Static Fields
    //
    public static final String Revision = "$Revision: 1.14 $";

    //
    // Fields
    //
    private Cartesian point;

    private int parent1;

    private int parent2;

    private List<Integer> coveredBy;

    private List<Integer> maskedBy;

    private RootStatus status;

    private int good;

    //
    // Properties
    //
    public int Good()

    {
        return this.good;
    }

    public void setGood(final int good) {
        this.good = good;
    }

    public int Parent1()

    {
        return this.parent1;
    }

    public int Parent2()

    {
        return this.parent2;
    }


    public Cartesian getPoint()

    {
        return this.point;
    }

    public RootStatus getStatus() {
        return this.status;
    }

    public void setPoint(final Cartesian point) {
        this.point = point;
    }

    public void setStatus(final RootStatus status) {
        this.status = status;
    }

    //
    // Constructors
    //
    public Root(Cartesian point, Convex c, int parent1, int parent2) {

        this.point = point;
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.good = 0;

        double costol, sintol;
        costol = Constant.CosHalf;
        sintol = Constant.SinHalf;

        // save who else covers and masks it
        this.coveredBy = new ArrayList<>();
        this.maskedBy = new ArrayList<>();
        this.status = RootStatus.Inside;
        for (int i = 0; i < c.HalfspaceList().size(); i++)
        {
            Halfspace Hi = c.HalfspaceList().get(i);
            if (i == parent1 || i == parent2)
            {
                continue;
            }
            if (Hi.Contains(point, costol, sintol))
            {
                this.coveredBy.add(i);
            }
            else
            {
                this.maskedBy.add(i);
                this.status = RootStatus.Outside;
            }
        }
    }

    //
    // Methods
    //
    public boolean MaskedByExists(List<Integer> other) {
        return !Collections.disjoint(this.maskedBy, other);
    }

    @Override
    public String toString() {
        return "(" + this.parent1 +
                ", " + this.parent2 + ") "
                + this.status
                + this.point;
    }
}
