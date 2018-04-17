package spherical.refs;

import spherical.util.Pair;
import spherical.util.Triple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Convex {
    //
    // Static Fields
    //
    public static String Revision = "$Revision: 1.48 $";

    //
    // Fields
    //
    private List<Halfspace> halfspaceList;

    private List<Patch> patchList;

    private boolean sorted;

    private boolean simplified;

    private ESign eSign;

    private double area;

    //
    // Properties
    //
    public Double getArea() {
        return this.area;
    }

    //public int Capacity() {
        //return this.halfspaceList.Capacity;
    //}


    public int Count() {
        return this.halfspaceList.size();
    }

    public ESign getESign()

    {
        if (this.eSign == ESign.Unknown) {
            this.eSign = this.halfspaceList.get(0).getESign();
            for (int i = 1; i < this.halfspaceList.size(); i++) {
                if (this.eSign != this.halfspaceList.get(i).getESign()) {
                    this.eSign = ESign.Mixed;
                    break;
                }
            }
        }
        return this.eSign;
    }

    public List<Halfspace> HalfspaceList() {
        return Collections.unmodifiableList(this.halfspaceList);
    }


    public List<Patch> PatchList() {
        return this.patchList;
    }


    public boolean Simplified()

    {
        return this.simplified;
    }

    public void setSimplified(boolean value) {
        if (this.simplified == value) {
            return;
        }
        if (!value) {
            this.simplified = false;
            this.patchList = null;
            this.area = 0;
            return;
        }
        this.simplified = true;
    }

    public boolean Sorted() {
        return this.sorted;
    }


    public List<Halfspace> XmlSerialization() {
        return this.halfspaceList;
    }


    //
    // Constructors
    //
    public Convex(List<Halfspace> halfspaces) {
        this(halfspaces.size());
        this.halfspaceList.addAll(halfspaces);
    }

    public Convex(List<Cartesian> list, PointOrder order) {
        this(list.size());

        if (list.size() < 3) {
            throw new IllegalArgumentException("Convex..ctor(list,order): Not enough points : list for a polygon!");
        }
        if (order == PointOrder.CW) {
            Collections.reverse(list);
        } else if (order == PointOrder.Safe) {
            Halfspace halfspace = new Halfspace(list.get(0).Cross(list.get(1), true), 0.0, 1.0);
            if (!halfspace.Contains(list.get(2), Constant.CosTolerance, Constant.SinTolerance)) {
                Collections.reverse(list);
            }
        } else if (order == PointOrder.Random) {
            throw new IllegalArgumentException("Convex..ctor(list,order): random order not implemented");
        }
        Arc[] array = new Arc[list.size()];
        Cartesian p;
        Cartesian cartesian;
        Halfspace halfspace2;
        for (int i = 1; i < list.size(); i++) {
            p = list.get(i - 1);
            cartesian = list.get(i);
            halfspace2 = new Halfspace(p.Cross(cartesian, true), 0.0, 1.0);
            array[i - 1] = new Arc(halfspace2, p, cartesian);
            this.halfspaceList.add(halfspace2);
        }
        p = list.get(list.size() - 1);
        cartesian = list.get(0);
        halfspace2 = new Halfspace(p.Cross(cartesian, true), 0.0, 1.0);
        array[list.size() - 1] = new Arc(halfspace2, p, cartesian);
        this.halfspaceList.add(halfspace2);
        this.patchList = new ArrayList<>(1);
        Patch patch = new Patch(array);
        this.patchList.add(patch);
        this.simplified = true;
        this.area = patch.getArea();
        this.Sort();
    }

    public Convex(Convex convex) {
        this(convex.halfspaceList.size());

        this.area = convex.area;
        this.sorted = convex.sorted;
        this.eSign = convex.eSign;
        this.simplified = convex.simplified;
        this.halfspaceList.addAll(convex.halfspaceList);
        if (convex.patchList == null) {
            this.patchList = null;
            return;
        }
        this.patchList = new ArrayList<>(convex.patchList.size());
        for (Patch current : convex.patchList) {
            this.patchList.add(new Patch(current));
        }
    }

    public Convex(Collection<Halfspace> halfspaces)
    {
        this(4);
        this.halfspaceList.addAll(halfspaces);
    }

    public Convex(Cartesian p1, Cartesian p2, Cartesian p3)

    {
        this(3);
        Arc[] array = new Arc[]{
                new Arc(p1, p2),
                new Arc(p2, p3),
                new Arc(p3, p1)
        };
        if (!new Halfspace(p1.Cross(p2, true), 0.0, 1.0).Contains(p3, Constant.CosTolerance, Constant.SinTolerance)) {
            throw new IllegalArgumentException("Convex..ctor(p1,p2,p3): incorrect winding!");
        }
        this.sorted = true;
        Arc[] array2 = array;
        for (int i = 0; i < array2.length; i++) {
            Arc arc = array2[i];
            this.halfspaceList.add(arc.Circle());
        }
        this.patchList = new ArrayList<>(1);
        this.patchList.add(new Patch(array));
        this.simplified = true;
        this.area =  Cartesian.SphericalTriangleArea(p1, p2, p3);
    }

    public Convex(Halfspace halfspace, boolean simplify)

    {
        this(1);
        this.halfspaceList.add(halfspace);
        this.sorted = true;
        if (simplify) {
            this.simplified = true;
            if (halfspace.IsEmpty()) {
                this.patchList = new ArrayList<>();
                this.area = 0.0;
                return;
            }
            Patch patch = new Patch(new Arc[]{
                    new Arc(halfspace, halfspace.GetPointWest())
            });
            this.patchList = Arrays.asList(patch);
            if (halfspace.IsAll()) {
                this.area = Constant.WholeSphereInSquareDegree;
                return;
            }
            this.area = patch.getArea();
        }
    }

    public Convex(Halfspace halfspace)
    {
        this(halfspace,false);
    }

    public Convex(int capacity) {
        this.halfspaceList = new ArrayList<>(capacity);
        this.patchList = new ArrayList<>(capacity);
        this.simplified = false;
        this.sorted = false;
        this.eSign = ESign.Unknown;
    }

    public Convex() {
        this(4);
    }

    public Convex(Double ra, Double dec, Double arcmin) {
        this(new Halfspace(ra, dec, arcmin));
    }

    //
    // Static Methods
    //
    public static Convex Clone(Convex c) {
        return new Convex(c);
    }

    public static int ComparisonAreaAscending(Convex c, Convex k) {
        return c.getArea().compareTo(k.getArea());
    }

    public static int ComparisonAreaDescending(Convex c, Convex k) {
        return k.getArea().compareTo(c.getArea());
    }

    public static Pair<Boolean, DynSymMatrix> GetCollisions(List<Convex> convexList) {
        DynSymMatrix mat = new DynSymMatrix(convexList.size());
        boolean result = true;
        for (int i = 0; i < convexList.size(); i++) {
            Convex convex = convexList.get(i);
            for (int j = 0; j < i; j++) {
                Convex c = convexList.get(j);
                if (convex.DoesCollide(c)) {
                    mat.set(i, j, true);
                    result = false;
                }
            }
        }
        return new Pair<>(result, mat);
    }

    public static Convex Parse(String repr, boolean normalize) {
        String separator = " ";

        String text = repr.trim().replace(',', ' ').replace("\r\n", " ")
                .replace('\n', ' ').replace('\t', ' ');
        String text2 = "";
        while (text2 != text) {
            text2 = text;
            text = text2.replace("  ", " ");
        }
        String[] array = text2.split(separator);
        if (array[0] != Constant.KeywordConvex) {
            throw new IllegalArgumentException("Convex.Parse(repr,norm): keyword does not match");
        }
        if (array.length % 4 != 1) {
            throw new IllegalArgumentException(String.format("Convex.Parse(): # of input values : String are wrong: {0}"
                    + array.length, new Object[0]));
        }
        Convex convex = new Convex();
        for (int i = 1; i < array.length; i += 4) {
            Double x = Double.parseDouble(array[i]);
            Double y = Double.parseDouble(array[i + 1]);
            Double z = Double.parseDouble(array[i + 2]);
            Double num = Double.parseDouble(array[i + 3]);
            convex.halfspaceList.add(new Halfspace(new Cartesian(x, y, z, normalize), num, Math.sqrt(1.0 - num * num)));
        }
        return convex;
    }

    //
    // Methods
    //
    public void Add(Halfspace h) {
        this.sorted = false;
        this.setSimplified(false);
        this.halfspaceList.add(h);
        if (this.eSign != ESign.Mixed) {
            this.eSign = ESign.Unknown;
        }
    }

    public void AddRange(Collection<Halfspace> collection) {
        this.sorted = false;
        this.setSimplified(false);
        this.halfspaceList.addAll(collection);
        if (this.eSign != ESign.Mixed) {
            this.eSign = ESign.Unknown;
        }
    }

    public void clear() {
        this.eSign = ESign.Unknown;
        this.sorted = false;
        this.setSimplified(false);
        this.halfspaceList.clear();
    }

    public boolean Contains(Cartesian p) {
        return this.Contains(p, Constant.CosTolerance, Constant.SinTolerance);
    }

    public boolean Contains(Cartesian p, Double costol, Double sintol) {
        if (this.halfspaceList.size() == 0) {
            return false;
        }
        for (Halfspace current : this.halfspaceList) {
            if (!current.Contains(p, costol, sintol)) {
                return false;
            }
        }
        return true;
    }

    public Region Difference(Convex convex) {
        Region region = new Region();
        for (int i = 0; i < convex.halfspaceList.size(); i++) {
            Convex convex2 = new Convex(this);
            for (int j = 0; j < i; j++) {
                convex2.Intersect(convex.halfspaceList.get(j));
            }
            convex2.Intersect(convex.halfspaceList.get(i).Inverse());
            region.Union(convex2);
        }
        return region;
    }

    public boolean DoesCollide(Convex c) {
        if (this.MecDisjoint(c)) {
            return false;
        }
        double cosTolerance = Constant.CosTolerance;
        double sinTolerance = Constant.SinTolerance;
        for (Patch current : this.PatchList()) {
            Arc[] arcList = current.getArcArray();
            for (int i = 0; i < arcList.length; i++) {
                Arc arc = arcList[i];
                if (c.Contains(arc.getPoint1(), cosTolerance, sinTolerance)) {
                    boolean result = true;
                    return result;
                }
            }
        }
        for (Patch current2 : c.PatchList()) {
            Arc[] arcList2 = current2.getArcArray();
            for (int j = 0; j < arcList2.length; j++) {
                Arc arc2 = arcList2[j];
                if (this.Contains(arc2.getPoint1(), cosTolerance, sinTolerance)) {
                    boolean result = true;
                    return result;
                }
            }
        }
        Double tolerance = Constant.Tolerance;
        for (Patch current3 : this.PatchList()) {
            for (Patch current4 : c.PatchList()) {
                Arc[] arcList3 = current3.getArcArray();
                for (int k = 0; k < arcList3.length; k++) {
                    Arc arc3 = arcList3[k];
                    Arc[] arcList4 = current4.getArcArray();
                    for (int l = 0; l < arcList4.length; l++) {
                        Arc arc4 = arcList4[l];

                        Triple<Topo, Integer, Pair<Cartesian, Cartesian>> topoRes = arc3.Circle().GetTopo(arc4.Circle());
                        Topo topo = topoRes.getX();
                        int num = topoRes.getY();
                        Cartesian x = topoRes.getZ().getX();
                        Cartesian x2 = topoRes.getZ().getY();

                        if (topo == Topo.Intersect) {
                            Double angle = arc3.GetAngle(x);
                            Double angle2 = arc4.GetAngle(x);
                            if (angle < arc3.Angle() + tolerance && angle2 < arc4.Angle() + tolerance
                                    && angle > -tolerance && angle2 > -tolerance) {
                                boolean result = true;
                                return result;
                            }
                            Double angle3 = arc3.GetAngle(x2);
                            Double angle4 = arc4.GetAngle(x2);
                            if (angle3 < arc3.Angle() + tolerance && angle4 < arc4.Angle() + tolerance
                                    && angle3 > -tolerance && angle4 > -tolerance) {
                                boolean result = true;
                                return result;
                            }
                        } else if (topo == Topo.Same) {
                            Double angle5 = arc3.GetAngle(arc4.getPoint1());
                            Double angle6 = arc3.GetAngle(arc4.getPoint2());
                            if ((angle5 < arc3.Angle() + tolerance && angle5 > -tolerance)
                                    || (angle6 < arc3.Angle() + tolerance && angle6 > -tolerance)) {
                                boolean result = true;
                                return result;
                            }
                            angle5 = arc4.GetAngle(arc3.getPoint1());
                            angle6 = arc4.GetAngle(arc3.getPoint2());
                            if ((angle5 < arc4.Angle() + tolerance && angle5 > -tolerance)
                                    || (angle6 < arc4.Angle() + tolerance && angle6 > -tolerance)) {
                                boolean result = true;
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    boolean ElimCovers(Convex that) {
        if (!this.simplified || !this.sorted) {
            throw new IllegalArgumentException("Convex.ElimCovers(): this should be simplified and sorted");
        }
        if (!that.simplified || !that.sorted) {
            throw new IllegalArgumentException("Convex.ElimCovers(): that should be simplified and sorted");
        }
        if (this.halfspaceList.size() > that.halfspaceList.size()) {
            return false;
        }
        Halfspace h;
        Halfspace halfspace;
        int count = this.halfspaceList.size();
        int count2 = that.halfspaceList.size();
        int num = 0;
        int i = 0;

        //TODO: Problematic
        while (i < this.halfspaceList.size()) {
            h = this.halfspaceList.get(i);
            boolean continueIL_CD = false;
            while (count - i <= count2 - num) {
                halfspace = that.halfspaceList.get(num);
                int num2 = Halfspace.ComparisonRadiusXYZ(h, halfspace);
                if (num2 == 0 || h.GetTopo(halfspace).getX() == Topo.Same) {
                    num++;
                    i++;
                    continueIL_CD = true;
                    break;
                }
                num++;
            }
            if(!continueIL_CD) {
                return false;
            }
        }
        return true;
    }

//    public boolean Exists(Predicate<Halfspace> match) {
//        return this.halfspaceList.Exists(match);
//    }

    public Convex Grow(Double arcmin) {
        this.setSimplified(false);
        for (int i = 0; i < this.halfspaceList.size(); i++) {
            Halfspace halfspace = this.halfspaceList.get(i);
            this.halfspaceList.set(i, halfspace.Grow(arcmin));
        }
        return this;
    }

    public boolean HasCommonHalfspace(Convex c) {
        Convex convex = new Convex(this);
        convex.AddRange(c.halfspaceList);
        convex.Sort();
        Halfspace h = null;
        for (Halfspace current : convex.halfspaceList) {
            if (current.GetTopo(h).getX() == Topo.Same) {
                return true;
            }
            h = current;
        }
        return false;
    }

    public boolean HasESignNegative() {
        if (!this.sorted) {
            this.Sort();
        }
        return this.halfspaceList.get(this.halfspaceList.size() - 1).getESign() == ESign.Negative;
    }

    public boolean HasOnlyOneCommonHalfspace(Convex c) {
        Convex convex = new Convex(this);
        convex.AddRange(c.halfspaceList);
        convex.Sort();
        Halfspace h = null;
        int num = 0;
        for (Halfspace current : convex.halfspaceList) {
            if (current.GetTopo(h).getX() == Topo.Same && num++ > 1) {
                return false;
            }
            h = current;
        }
        return num == 1;
    }

    public boolean HasOnlyOneESignNegative() {
        boolean flag = this.HasESignNegative();
        return (flag && this.halfspaceList.size() == 1) || (flag
                && this.halfspaceList.get(this.halfspaceList.size() - 2).getESign() != ESign.Negative);
    }

    public Pair<Integer, Integer> IndexFirstCommonHalfspacePair(Convex c) throws Exception {
        if (!this.sorted) {
            this.Sort();
        }
        if (!c.sorted) {
            c.Sort();
        }
        int num = 0;
        int num2 = 0;
        Halfspace h = this.halfspaceList.get(num);
        Halfspace halfspace = c.halfspaceList.get(num2);
        boolean flag = false;
        while (!flag) {
            int num3 = Halfspace.ComparisonRadiusXYZ(h, halfspace);
            Topo topo = h.GetTopo(halfspace).getX();
            if (num3 == 0 || topo == Topo.Same) {
                flag = true;
            } else if (num3 == 1) {
                if (num2 == c.halfspaceList.size() - 1) {
                    throw new Exception("Convex.IndexFirstCommonHalfspacePair(): no common");
                }
                num2++;
                halfspace = c.halfspaceList.get(num2);
            } else {
                if (num == this.halfspaceList.size() - 1) {
                    throw new Exception("Convex.IndexFirstCommonHalfspacePair(): no common");
                }
                num++;
                h = this.halfspaceList.get(num);
            }
        }
        return new Pair(num2, num);
    }

    public void Intersect(Halfspace h) {
        this.Add(h);
    }

    public void Intersect(Convex c) {
        this.AddRange(c.halfspaceList);
    }

    void InvertHalfspaces() {
        this.setSimplified(false);
        this.sorted = false;
        //Problematic
        this.halfspaceList.replaceAll(Halfspace::Inverse);
    }

    public boolean MecContains(Cartesian x) {
        return this.MecContains(x, Constant.CosTolerance, Constant.SinTolerance);
    }

    public boolean MecContains(Cartesian x, Double costol, Double sintol) {
        for (Patch current : this.patchList) {
            if (current.mec().Contains(x, costol, sintol)) {
                return true;
            }
        }
        return false;
    }

    public boolean MecDisjoint(Convex c) {
        for (Patch current : this.patchList) {
            for (Patch current2 : c.patchList) {
                Topo topo = current.mec().GetTopo(current2.mec()).getX();
                if (topo != Topo.Disjoint && topo != Topo.Inverse) {
                    return false;
                }
            }
        }
        return true;
    }

    public void RemoveAt(int index) {
        this.eSign = ESign.Unknown;
        this.setSimplified(false);
        this.halfspaceList.remove(index);
    }

    public void RemoveRange(int index, int count) {
        this.eSign = ESign.Unknown;
        this.setSimplified(false);
        //RemoveRange
        this.halfspaceList.subList(index, index+count).clear();
    }

    public boolean Same(Convex c) {
        if (c.halfspaceList.size() != this.halfspaceList.size()) {
            return false;
        }
        if (!this.Sorted()) {
            this.Sort();
        }
        if (!c.Sorted()) {
            c.Sort();
        }
        for (int i = 0; i < this.halfspaceList.size(); i++) {
            Topo topo = c.halfspaceList.get(i).GetTopo(this.halfspaceList.get(i)).getX();
            if (topo != Topo.Same) {
                return false;
            }
        }
        return true;
    }

    boolean SimpleSimplify() throws Exception {
        List<Integer> iDelete = new ArrayList<>();
        boolean empty = false;

        // if empty, done
        if (halfspaceList.size() == 0) {
            return false;
        }

        // make sure halfspaces are sorted by radius (ascending)
        if (!this.sorted)
        {
            this.Sort();
        }

        // if smallest halfspace is infinitesimal, empty it and done
        if (halfspaceList.get(0).IsEmpty()) {
            halfspaceList.clear();
            this.sorted = false;
            return false;
        }

        // check pairwise relations
        for (int i = 0; i < halfspaceList.size() && !empty; i++) {
            if (iDelete.contains(i)) continue;
            Halfspace Hi = halfspaceList.get(i);
            for (int j = 0; j < i; j++)
            {
                if (iDelete.contains(j)) continue;

                Halfspace Hj = halfspaceList.get(j);
                // mark 'i' for removal and break continue with next halfspace
                Triple<Topo, Integer, Pair<Cartesian, Cartesian>> topoRes = Hj.GetTopo(Hi);
                Cartesian pos = topoRes.getZ().getX();
                Cartesian neg = topoRes.getZ().getY();
                Topo t = topoRes.getX();
                if (t == Topo.Same || t == Topo.Inner)
                {
                    iDelete.add(i);
                    break;
                }
                else if (t == Topo.Inverse || t == Topo.Disjoint)
                {
                    empty = true;
                    break;
                }
            }
        }

        // if empty - purge halfspaces
        if (empty) {
            this.halfspaceList.clear();
            this.sorted = false;
        }
        else // remove redundant halfspaces
        {
            iDelete.sort(Comparator.naturalOrder());
            Collections.reverse(iDelete);
            for (int i : iDelete)
                halfspaceList.remove(i);

            if (halfspaceList.size() == 0)
                throw new Exception("Convex.SimpleSimplify(): no halfspaces left? shouldn't happen");
        }
        return !empty;
    }

    public boolean Simplify(boolean simple_simplify) throws Exception {
        if (this.simplified) {
            return true;
        }

        if (simple_simplify && !this.SimpleSimplify()) {
            this.simplified = true;
            this.area = 0.0;
            return false;
        }

        if (!this.sorted) {
            this.Sort();
        }
        PatchFactory patchFactory = new PatchFactory(this);
        this.patchList = patchFactory.DerivePatches(true);

        this.simplified = true;
        this.area = 0.0;
        if (this.halfspaceList == null) {
            throw new Exception("Convex.Simplify(): HalfspaceList is NULL");
        }
        if (this.halfspaceList.size() == 0) {
            return false;
        }
        for (Patch current : this.patchList) {
            this.area += current.getArea();
        }
        if (this.halfspaceList.get(0).IsAll() && this.area < Constant.TolArea) {
            this.area += Constant.WholeSphereInSquareDegree;
        }
        if (this.area < 0.0) {
            boolean flag = false;
            for (Halfspace current2 : this.halfspaceList) {
                if (current2.getESign() != ESign.Negative) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                this.area = 0.0;
                return false;
            } else {
                this.area += Constant.WholeSphereInSquareDegree;
            }
        }
        return true;
    }

    public boolean SmartContains(Cartesian x, Double costol, Double sintol) {
        return this.MecContains(x, costol, sintol) && this.Contains(x, costol, sintol);
    }

    public boolean SmartContains(Cartesian x) {
        return this.SmartContains(x, Constant.CosTolerance, Constant.SinTolerance);
    }

    public Pair<Region, Boolean> SmartDifference(Convex convex) throws Exception {
        Convex convex2 = new Convex(this);
        boolean flag = convex2.SmartIntersect(convex);
        Region region = new Region();
        if (!flag) {
            region.Add(new Convex(this));
            region.UpdateArea();
        } else if (!this.Same(convex2)) {
            region = this.Difference(convex2);
            region.Simplify(true, false, false, false, null);
        }
        return new Pair(region, flag);
    }

    public boolean SmartIntersect(Convex c) throws Exception {
        if (this.MecDisjoint(c)) {
            this.halfspaceList.clear();
            this.patchList = null;
            this.area = 0.0;
            this.simplified = true;
            return false;
        }
        Double arg_46_0 = this.getArea();
        this.Intersect(c);
        return this.Simplify(true);
    }

    public void Sort() {
        this.halfspaceList.sort((l, r) -> Halfspace.ComparisonRadiusXYZ(l, r));
        this.sorted = true;
    }

    @Override
    public String toString() {
        return this.toString("\r\n\t");
    }

    public String toString(String sep) {
        String text = Constant.KeywordConvex;
        for (Halfspace current : this.halfspaceList) {
            text = text + sep + current.toString();
        }
        return text;
    }

//    public boolean TrueForAll(Predicate<Halfspace> match) {
//        return this.halfspaceList.TrueForAll(match);
//    }
}
