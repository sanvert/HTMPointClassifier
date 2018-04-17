package spherical.refs;

import spherical.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Patch implements IPatch {
    //
    // Static Fields
    //
    public static final String Revision = "$Revision: 1.31 $";

    //
    // Fields
    //
    private Arc[] arcList;

    private Halfspace mec;

    private double area;

    private double length;

    //
    // Properties
    //
    @Override
    public Arc[] getArcArray() {
        return this.arcList;
    }

    @Override
    public List<Arc> getArcList()

    {
        return Arrays.asList(this.arcList);
    }

    public void setArea(final Double area) {
        this.area = area;
    }

    public Double getArea() {
        return this.area;
    }


    public Double Length() {
        return this.length;
    }

    @Override
    public Halfspace mec() {
        return this.mec;
    }

    //
    // Constructors
    //
    public Patch() {
    }

    public Patch(Arc[] list) {
        if (list.length < 1) {
            throw new IllegalArgumentException("Patch(list): empty arclist");
        }
        this.arcList = list;
        Pair<Double, Double> result = this.CalcAreaLength(this.arcList[0].Middle());
        this.area = result.getX();
        this.length = result.getY();
        this.mec = this.GetMinimalEnclosingCircle(this.area);
    }

    public Patch(Patch p) {
        this.mec = p.mec;
        this.area = p.area;
        this.length = p.length;
        this.arcList = new Arc[p.arcList.length];
        for (int i = 0; i < this.arcList.length; i++) {
            this.arcList[i] = new Arc(p.arcList[i]);
        }
    }

    //
    // Methods
    //
    private Pair<Double, Double> CalcAreaLength(Cartesian c) {
        double a = 0,  l = 0;
        for (Arc e : arcList) {
            a += e.CalcTriangleArea(c);
            l += e.Length();
        }
        return new Pair<>(a, l);
    }

    @Override
    public boolean containsOnEdge(Cartesian p) {
        Arc[] array = this.getArcArray();
        for (int i = 0; i < array.length; i++) {
            Arc arc = array[i];
            if (arc.containsOnEdge(p)) {
                return true;
            }
        }
        return false;
    }

    private Halfspace GetMinimalEnclosingCircle(double tarea) {
        Arc a0 = arcList[0];
        Halfspace mec;
        if (arcList.length == 1)
        {
            mec = a0.Circle();
        } else {
            List<Cartesian> pts = new ArrayList<>(arcList.length);
            List<Cartesian> test = new ArrayList<>(1);
            Halfspace largest_hole = Halfspace.UnitSphere;

            boolean hole = true;
            if (tarea > 0) hole = false;

            //int ngc = 0; // # of great circles
            for(Arc a : arcList) {
                if (!hole) {
                    // end points may already be in list
                    if (!pts.contains(a.getPoint1())) pts.add(a.getPoint1());
                    if (!pts.contains(a.getPoint2())) pts.add(a.getPoint2());
                    // middle point cannot
                    pts.add(a.Middle());
                } else {
                    if (largest_hole.getCos0() < a.Circle().getCos0())
                        largest_hole = a.Circle();

                    if (test.size() < 1) test.add(a.Middle());
                }
            }

            if (hole) {
                mec = largest_hole;
            } else {
                mec = Cartesian.MinimalEnclosingCircleOptimalSlow(pts);
            }
        }

        return mec;
    }

    @Override
    public String toString() {
        String str = String.format("# {0}\n", this.arcList.length);
        String[] array = new String[this.arcList.length];
        for (int i = 0; i < this.arcList.length; i++) {
            array[i] = this.arcList[i].toString();
        }
        return str + String.join("\n", array);
    }
}