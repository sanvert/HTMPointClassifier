package spherical.refs;

import spherical.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class PatchFactory {
    //
    // Static Fields
    //
    public static final String Revision ="$Revision: 1.21 $";

    //
    // Fields
    //
    private Convex convex;

    private boolean[] hasRoot;

    private Cartesian[] west;

    private List<Root> roots;

    private int[] ego;

    private List<Arclet> arclets;

    private boolean hasDegeneracy;

    //
    // Constructors
    //
    public PatchFactory(Convex convex) {
        int num = convex.HalfspaceList().size();
        this.west = new Cartesian[num];
        this.hasRoot = new boolean[num];
        this.convex = convex;
        this.hasDegeneracy = false;

        // derive roots
        this.roots = new ArrayList<>(num * (num - 1));
        Cartesian p1, p2;
        int nroots;
        // all the pairs of halfspaces
        for (int i=0; i<num; i++) {
            Halfspace Hi = convex.HalfspaceList().get(i);
            west[i] = Hi.GetPointWest();
            for (int j=0; j<i; j++)
            {
                Halfspace Hj = convex.HalfspaceList().get(j);
                Pair<Integer, Pair<Cartesian, Cartesian>> hiRootsRes = Hi.Roots(Hj);
                nroots = hiRootsRes.getX();
                p1 = hiRootsRes.getY().getX();
                p2 = hiRootsRes.getY().getY();

                // # of roots is 2 - otherwise no roots
                if (nroots == 2)
                {
                    roots.add(new Root(p1, convex, i, j));
                    roots.add(new Root(p2, convex, j, i));
                    hasRoot[i] = true;
                    hasRoot[j] = true;
                }
            }
        }
        // set the last west point as well
        //west[num - 1] = convex.HalfspaceList[num - 1].GetPointWest();

        // add west point for those who have no roots
        for (int i = 0; i < hasRoot.length; i++) {
            if (!hasRoot[i])
                roots.add(new Root(west[i], convex, i, i));
        }

        // set ego of roots
        ego = new int[roots.size()];
        for (int i = 0; i < ego.length; i++) {
            ego[i] = i;
        }

        // alter-ego (degeneracy)
        // set identical point for roots with the same location
        // watch out for their status!!!
        Root Ri, Rj;
        for (int i = 0; i < roots.size(); i++) {
            Ri = roots.get(i);
            for (int j = 0; j < i; j++) {
                Rj = roots.get(j);
                //if (Ri.Status == Rj.Status && Ri.Point.Same(Rj.Point))
                if (Ri.getPoint().Mirrored().Same(Rj.getPoint())) {
                    if (Ri.getStatus() == RootStatus.Inside) Rj.setStatus(Ri.getStatus());
                    else if (Rj.getStatus() == RootStatus.Inside) Ri.setStatus(Rj.getStatus());
                    Ri.setPoint(new Cartesian(Rj.getPoint(), false));
                    ego[i] = ego[j];
                    this.hasDegeneracy = true;
                }
            }
        }
        // setup empty arclet list
        arclets = new ArrayList<>();
    }

    //
    // Methods
    //
    private void BuildArclets() {
        for (int i = 0; i < this.convex.HalfspaceList().size(); i++) {
            int[] array = this.SelectRootsOnCircle(i);

            if (array.length == 0) continue;

            for (int j = 1; j < array.length; j++) {
                this.arclets.add(new PatchFactory.Arclet(i, array[j - 1], array[j]));
            }
            this.arclets.add(new PatchFactory.Arclet(i, array[array.length - 1], array[0]));
        }
    }

    private List<Integer> ConnectArclets(List<Integer> iArclet) throws Exception {
        List<Integer> taken = new ArrayList<>();

        // first element of good arcs is where we start
        // and its start point is where we want to arrive
        int iFirst = iArclet.get(0);
        Arclet aFirst = arclets.get(iFirst);
        int first1 = aFirst.iRoot1;

        // return array of arclets and remove the ones used
        List<Integer> iPatch = new ArrayList<>();
        iPatch.add(iFirst);
        iArclet.remove(0);

        int prev2 = aFirst.iRoot2;
        boolean done = false;

        // done if its endpoint is the first
        if (ego[prev2] == ego[first1])
        {
            return iPatch;
        }

        // assemble a patch
        Arclet a;
        while (!done)
        {
            boolean found = false;
            for (int i : iArclet)
            {
                if (taken.contains(i)) continue;
                a = arclets.get(i);

                // if matching part, add
                if (ego[a.iRoot1] == ego[prev2])
                {
                    found = true;

                    // if same circle as before p1, reject
                    if (roots.get(a.iRoot1).Parent1() == a.iCircle)
                    {
                        taken.add(i);
                    } else // otherwise add to patch
                    {
                        iPatch.add(i);
                        prev2 = arclets.get(i).iRoot2;
                        taken.add(i);
                    }
                    break;
                }
            }

            if (!found)
                throw new Exception("PatchFactory.ConnectArclets(): no matching arclet found!" + this.convex);

            if (ego[prev2] == ego[first1])
                done = true;

            // remove 'taken' arcs
            for (int i : taken) {
                iArclet.remove(i);
            }
            taken.clear();
        }

        return iPatch;

    }

    List<Patch> DerivePatches(boolean simplify_convex) throws Exception {
        // make all possible arcs (with proper ordering)

        this.BuildArclets();

        // arclets with middle point inside convex
        List<Integer> iGoodArclet = this.SelectArcletsVisible(RootStatus.Inside);

        // prune degenerate arclets
        if (hasDegeneracy) PruneGoodArclets(iGoodArclet);

        // prune good circles/roots
        List<Integer> iGoodCirc = this.SelectCirclesWithArclets(iGoodArclet);
        //List<int> iGoodRoot = this.SelectRootsInArclets(iGoodArclet);

        // add halfspaces needed to mask outside roots
        List<Integer> iVisible = this.SelectCompleteCircles(iGoodCirc);

        //List<int> iGoodArcletCopy = new List<int>(iGoodArclet);

        // assemble patches one by one and update iGoodArclet list until it's empty
        List<Patch> patches = new ArrayList<>();
        while (iGoodArclet.size() > 0)
        {
            try
            {
                List<Integer> iPatch = this.ConnectArclets(iGoodArclet); // update iGoodArclet

                Arc[] arcs = new Arc[iPatch.size()];
                for (int j = 0; j < iPatch.size(); j++)
                {
                    int i = iPatch.get(j);
                    arcs[j] = this.MakeArc(arclets.get(i));
                }

                Patch p = new Patch(arcs);
                if (arcs.length == 2 && arcs[0].Middle().Same(arcs[1].Middle()))
                {
                    // force area to be zero
                    p.setArea(0.0);
                }

                patches.add(p);
            }
            catch (Exception e) // hack
            {
                e.printStackTrace();
            }
        }

        // simplify convex
        if (simplify_convex)
        {
            for (int i = this.convex.HalfspaceList().size() - 1; i >= 0; i--)
            {
                if (!iGoodCirc.contains(i))
                {
                    this.convex.RemoveAt(i);
                }
            }
        }

        return patches;

    }

    private void InitRootGoodness(List<Integer> S) {
        for(Root current : this.roots){
            if (current.getStatus() == RootStatus.Inside) {
                current.setGood(1);
            } else if (!S.contains(current.Parent1()) || !S.contains(current.Parent2())) {
                current.setGood(-1);
            } else if (current.MaskedByExists(S)) {
                current.setGood(0);
            } else {
                current.setGood(-2);
            }
        }
    }

    private Arc MakeArc(PatchFactory.Arclet arclet) {
        Arc result;
        if (arclet.iRoot1 == arclet.iRoot2) {
            result = new Arc(new Halfspace(this.convex.HalfspaceList().get(arclet.iCircle)),
                    this.roots.get(arclet.iRoot1).getPoint());
        } else {
            result = new Arc(new Halfspace(this.convex.HalfspaceList().get(arclet.iCircle)),
                    this.roots.get(arclet.iRoot1).getPoint(),
                    this.roots.get(arclet.iRoot2).getPoint());
        }
        return result;
    }

    private void PruneGoodArclets(List<Integer> iArclet) {
        List<Integer> redundant = new ArrayList<>();
        for (int i : iArclet)
        {
            if (redundant.contains(i)) continue;

            for (int j : iArclet)
            {
                if (i==j || redundant.contains(j)) continue;

                if (ego[arclets.get(i).iRoot1] == ego[arclets.get(j).iRoot1] &&
                        ego[arclets.get(i).iRoot2] == ego[arclets.get(j).iRoot2])
                {
                    Arc ai = this.MakeArc(arclets.get(i));
                    Arc aj = this.MakeArc(arclets.get(j));
                    Cartesian mi = ai.Middle();
                    Cartesian mj = aj.Middle();
                    double di = mi.Dot(aj.Circle().Vector()) - aj.Circle().getCos0();
                    double dj = mj.Dot(ai.Circle().Vector()) - ai.Circle().getCos0();
                    if (di < dj)
                        redundant.add(i);
                    else
                        redundant.add(j);
                }
            }
        }
        for (int r : redundant)
        {
            iArclet.remove(r);
        }

    }

    private List<Integer> SelectArcletsVisible(RootStatus status) {
        List<Integer> iVis = new ArrayList<>();
        Cartesian test;

        double costol, sintol;
        costol = Constant.CosTolerance;
        sintol = Constant.SinTolerance;

        for (int i = 0; i < arclets.size(); i++)
        {
            Arclet a = arclets.get(i);

            // if endpoints have different status than requested, continue
            if (status != RootStatus.Any)
            {
                if (roots.get(a.iRoot1).getStatus() != status || roots.get(a.iRoot2).getStatus() != status)
                {
                    continue;
                }
            }
            // if circle is not the relevant parent of the roots...
            if (a.iCircle != roots.get(a.iRoot1).Parent2()) continue;
            if (a.iCircle != roots.get(a.iRoot2).Parent1()) continue;

            // is same roots, infinitsimal arc rejected
            //if (a.iRoot1 != a.iRoot2 && roots[a.iRoot1].Point == roots[a.iRoot2].Point) continue;
            if (a.iRoot1 != a.iRoot2 && ego[a.iRoot1] == ego[a.iRoot2]) continue;

            // materialize arc
            Arc arc = this.MakeArc(a);

            test = arc.Middle();

            boolean inside = true;
            for (int j = 0; j < convex.HalfspaceList().size(); j++)
            {
                if (j == a.iCircle) continue;
                if (!convex.HalfspaceList().get(j).Contains(test, costol, sintol))
                {
                    inside = false;
                    break;
                }
            }
            if (inside) iVis.add(i);
        }
        return iVis;

    }

    private List<Integer> SelectCirclesWithArclets(List<Integer> iGoodArclets) {
        List<Integer> iCirc = new ArrayList<>();
        int circ;
        for (int i : iGoodArclets)
        {
            circ = arclets.get(i).iCircle;
            if (!iCirc.contains(circ)) {
                iCirc.add(circ);
            }
        }
        return iCirc;

    }

    private List<Integer> SelectCompleteCircles(List<Integer> S) throws Exception {
        // everybody in S is visible
        List<Integer> visible = new ArrayList<>(S);

        double costol, sintol;
        costol = Constant.CosTolerance;
        sintol = Constant.SinTolerance;

        while (true) {
            // set 'good' flag of roots
            this.InitRootGoodness(S);

            // find halfspace that covers most roots with good=-2

            boolean hasMinusTwo = false;
            for (Root r : roots)
                if (r.Good() == -2) {
                    hasMinusTwo = true;
                    break;
                }
            if (!hasMinusTwo) break;
            //Cartesian[] pts = Cartesian.SelectPoints(roots, MinusTwo);
            int nmax = -1, imax = 99999999, n;
            for (int i = 0; i < convex.HalfspaceList().size(); i++) {
                if (S.contains(i)) continue;
                // # of roots masked by halfspace 'i'

                n = 0;
                for (Root r : roots) {
                    if (r.Good() != -2) continue;
                    if (!convex.HalfspaceList().get(i).Contains(r.getPoint(), costol, sintol)) {
                        n++;
                    }
                }
                if (n > nmax) {
                    nmax = n;
                    imax = i;
                }
            }
            // throw up if nobody from the rest covers any of the good=-2 roots
            if (nmax == -1)
                throw new Exception("PatchFactory.SelectCompleteCircles(): nmax=-1 should never happen!");

            // add it to minimal set and invisible
            S.add(imax);
        }
        return visible;
    }

    //Problematic - sort two lists
    private int[] SelectRootsOnCircle(int c) {
        List<Integer> r = new ArrayList<>();
        // loop over roots and include if circle 'c' is a parent
        for (int i = 0; i < roots.size(); i++)
        {
            if (c == roots.get(i).Parent1() || c == roots.get(i).Parent2())
            {
                r.add(i);
            }
        }

        Arc arc = new Arc(this.convex.HalfspaceList().get(c), this.west[c]);

        return r.stream().map(element -> {
                    Cartesian point = this.roots.get(element).getPoint();
                    double angle = arc.GetAngle(point);
                    return new Pair<>(angle, element); })
                .sorted(Comparator.comparingDouble(Pair::getX))
                .mapToInt(Pair::getY).toArray();
    }

    public String ToString(RootStatus status) {
        String text = "Roots:";
        for (int i = 0; i < this.roots.size(); i++) {
            if (status == RootStatus.Any || this.roots.get(i).getStatus() == status) {
                text += "\\n\\t" +  i + this.ego[i] + this.roots.get(i);
            }
        }
        if (this.arclets.size() > 0) {
            text += "\\nArclets:";
            for (int j = 0; j < this.arclets.size(); j++) {
                text += "\\n\\t" + j + this.roots.get(j);
            }
        }
        return text;
    }

    @Override
    public String toString() {
        return this.ToString(RootStatus.Any);
    }

    //
    // Nested Types
    //
    private class Arclet

    {
        public int iCircle;

        public int iRoot1;

        public int iRoot2;

        public Arclet( int iCircle, int iRoot1, int iRoot2)
        {
            this.iCircle = iCircle;
            this.iRoot1 = iRoot1;
            this.iRoot2 = iRoot2;
        }

        @Override
        public String toString() {
            return this.iCircle + "->" + this.iRoot1 + " on " + this.iRoot2;
        }
    }
}