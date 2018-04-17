package spherical.refs;

import spherical.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Region {
    //
    // Static Fields
    //
    public static final String Revision = "$Revision: 1.56 $";

    //
    // Fields
    //
    private List<Convex> convexList;

    private double area;

    public double getArea() {
        return area;
    }

    public void setArea(final double area) {
        this.area = area;
    }

    public final Collection<Convex> ConvexList()

    {
        return Collections.unmodifiableCollection(this.convexList);
    }


    public boolean Simplified()

    {
        for(Convex current : this.ConvexList()){
            if (!current.Simplified()) {
                return false;
            }
        }
        return true;
    }

    //
    // Constructors
    //
    public Region(double ra, double dec, double arcmin) {
        this(new Convex(ra, dec, arcmin), false);
    }

    public Region(Halfspace halfspace) {
        this(new Convex(halfspace), false);
    }

    public Region(Region region) {
        region.convexList.replaceAll(Convex::Clone);
        this.area = region.area;
    }

    public Region(Collection<Convex> collection) {
        this.convexList = new ArrayList<>(collection);
        this.area = 0.0;
    }

    public Region(List<Convex> list) {
        this.convexList = list;
        this.area = 0.0;
    }

    public Region() {
        this(1);
    }

    public Region(int capacity) {
        this.convexList = new ArrayList<>(capacity);
        this.area = 0.0;
    }

    public Region(Convex convex, boolean clone) {
        this(1);
        if (clone) {
            this.convexList.add(new Convex(convex));
            return;
        }
        this.convexList.add(convex);
    }

    //
    // Static Methods
    //
    public static Region Parse(String repr, boolean normalize) {
        String separator = " ";
        String separator2 = ",";

        String text = repr.trim().replace(',', ' ').replace("\r\n", " ")
                .replace('\n', ' ').replace('\t', ' ');
        String text2 = "";
        while (text2 != text) {
            text2 = text;
            text = text2.replace("  ", " ");
        }
        String[] array = text2.split(separator, 2);
        if (array[0] != Constant.KeywordRegion) {
            throw new IllegalArgumentException("Region.Parse(): wrong keyword");
        }
        String text3 = array[1].replace(Constant.KeywordConvex, "," + Constant.KeywordConvex);
        array = text3.split(separator2);
        Region region = new Region();
        for (int i = 1; i < array.length; i++) {
            region.convexList.add(Convex.Parse(array[i], normalize));
        }
        return region;
    }

    //
    // Methods
    //
    public void Add(Convex c) {
        this.area = 0.0;
        this.convexList.add(c);
    }

    public void AddRange(List<Convex> collection) {
        this.area = 0.0;
        int num = this.convexList.size() + collection.size();
        this.convexList.addAll(collection);

    }

    public void AddRange(Collection<Convex> collection) {
        this.area = 0.0;
        this.convexList.addAll(collection);
    }

    public void Clear() {
        this.area = 0.0;
        this.convexList.clear();
    }

    public boolean Contains(Cartesian p) {
        return this.Contains(p, Constant.CosTolerance, Constant.SinTolerance);
    }

    public boolean Contains(Cartesian p, double costol, double sintol) {
        for (Convex current : this.convexList) {
            if (current.Contains(p, costol, sintol)) {
                return true;
            }
        }
        return false;
    }

    public Region Difference(Convex C) {
        Region region = new Region();
        for (int i = 0; i < C.HalfspaceList().size(); i++) {
            Region region2 = new Region(this);
            for (int j = 0; j < i; j++) {
                region2.Intersect(C.HalfspaceList().get(j));
            }
            region2.Intersect(C.HalfspaceList().get(i).Inverse());
            region.Union(region2);
        }
        return region;
    }

    public void Difference(Halfspace h) {
        this.Intersect(h.Inverse());
    }

    public boolean DoesCollide(Convex c) {
        for(Convex current : this.ConvexList()){
            if (current.DoesCollide(c)) {
                return true;
            }
        }
        return false;
    }

    private void EliminateConvexes(DynSymMatrix collision) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < this.convexList.size(); i++) {
            if (!list.contains(i)) {
                Convex convex = this.convexList.get(i);
                for (int j = 0; j < i; j++) {
                    if (collision.get(i, j)){
                        Convex convex2 = this.convexList.get(j);
                        if (!list.contains(j)) {
                            if (convex.HalfspaceList().size() >= convex2.HalfspaceList().size()) {
                                if (convex2.ElimCovers(convex)) {
                                    list.add(i);
                                }
                            } else if (convex.ElimCovers(convex2)) {
                                list.add(j);
                            }
                        }
                    }
                }
            }
        }
        list.sort(Comparator.naturalOrder());
        Collections.reverse(list);
        for(int current : list){
            this.convexList.remove(current);
            collision.removeAt(current);
        }
    }

    private void EliminateConvexes() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < this.convexList.size(); i++) {
            if (!list.contains(i)) {
                Convex convex = this.convexList.get(i);
                for (int j = 0; j < i; j++) {
                    Convex convex2 = this.convexList.get(j);
                    if (!list.contains(j)) {
                        if (convex.HalfspaceList().size() >= convex2.HalfspaceList().size()) {
                            if (convex2.ElimCovers(convex)) {
                                list.add(i);
                            }
                        } else if (convex.ElimCovers(convex2)) {
                            list.add(j);
                        }
                    }
                }
            }
        }
        list.sort(Comparator.naturalOrder());
        Collections.reverse(list);
        for( int current : list){
            this.convexList.remove(current);
        }
    }

    public List<IPatch> Patches() {
        return this.convexList.stream().map(c -> c.PatchList())
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public String Format() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("REGION ");
        for(Convex current : this.convexList){
            stringBuilder.append("CONVEX CARTESIAN ");
            for(Halfspace current2 : current.HalfspaceList()) {
                stringBuilder.append(String.format("{0,20:0.000000000000000} {1,20:0.000000000000000} {2,20:0.000000000000000} {3,20:0.000000000000000} ",
                            new Object[]{
                                current2.Vector().getX(),
                                current2.Vector().getY(),
                                current2.Vector().getZ(),
                                current2.getCos0()}
                            ));
            }
        }
        return stringBuilder.toString();
    }

    public int GetNumberOfHalfspaces() {
        int num = 0;
        for(Convex current : this.convexList){
            num += current.HalfspaceList().size();
        }
        return num;
    }

    public int GetNumberOfPatches() {
        int num = 0;
        for (Convex current : this.convexList) {
            num += current.PatchList().size();
        }
        return num;
    }

    public void Grow(double arcmin) {
        for (Convex current : this.convexList) {
            current.Grow(arcmin);
        }
    }

    public void Intersect(Convex c) {
        this.area = 0.0;
        for (Convex current : this.convexList) {
            current.AddRange(c.HalfspaceList());
        }
    }

    public Region Intersect(Region region) {
        Region region2 = new Region(this.convexList.size() * region.convexList.size());
        for(Convex current : this.convexList){
            for(Convex current2 : region.convexList) {
                Convex convex = new Convex(current.HalfspaceList().size() + current2.HalfspaceList().size());
                convex.AddRange(current.HalfspaceList());
                convex.AddRange(current2.HalfspaceList());
                region2.convexList.add(convex);
            }
        }
        return region2;
    }

    public void Intersect(Halfspace h) {
        this.area = 0.0;
        for (Convex current : this.convexList) {
            current.Add(h);
        }
    }

    private void MakeDisjoint(DynSymMatrix collision) throws Exception {
        int maximumIteration = Global.MAXITER;
        int i;
        for (i = 0; i < maximumIteration; i++) {

            //Problematic
            List<Pair<Double, Integer>> listPair = new ArrayList<>();
            for (int j = 0; j < this.convexList.size(); j++) {
                listPair.add(new Pair(this.convexList.get(j).getArea(), j));
            }
            List<Integer> arr2List = listPair.stream().sorted(Comparator.comparingDouble(Pair::getX))
                    .map(Pair::getY).collect(Collectors.toList());
            Collections.reverse(arr2List);

            Pair<Boolean, Pair<Integer, Integer>> fvResult = collision.FindValue(true, arr2List);
            int num = fvResult.getY().getX();
            int num2 = fvResult.getY().getY();
            if (!fvResult.getX()) {
                return;
            }
            Convex convex = this.convexList.get(num);
            Convex convex2 = this.convexList.get(num2);

            Pair<Region, Boolean> sdResult = convex2.SmartDifference(convex);
            boolean flag = sdResult.getY();
            if (!flag) {
                collision.set(num, num2, false);
            } else {
                List<Boolean> list = collision.Row(num2);
                list.remove(num2);
                collision.removeAt(num2);
                this.convexList.remove(num2);
                if (num > num2) {
                    num--;
                }
                int count = this.convexList.size();
                for(Convex current : sdResult.getX().convexList) {
                    if (current.getArea() > Constant.TolArea
                            && current.getArea() < Constant.WholeSphereInSquareDegree - Constant.TolArea) {
                        this.convexList.add(current);
                    }
                }
                collision.setDim(this.convexList.size());
                for (int k = count; k < this.convexList.size(); k++) {
                    Convex convex3 = this.convexList.get(k);
                    for (int l = 0; l < count; l++) {
                        if (l != num) {
                            Convex c = this.convexList.get(l);
                            if (list.get(l) && convex3.DoesCollide(c)) {
                                collision.set(k, l, true);
                            }
                        }
                    }
                }
            }
        }
        if (i == maximumIteration) {
            throw new Exception("Region.MakeDisjoint(): exceeded maximum number of iterations!");
        }
    }

    public boolean MecContains(Cartesian x) {
        double cosTolerance = Constant.CosTolerance;
        double sinTolerance = Constant.SinTolerance;
        for(Convex current : this.convexList){
            for(Patch current2 : current.PatchList()) {
                if (current2.mec().Contains(x, cosTolerance, sinTolerance)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void Remove(Convex item) {
        this.area = 0.0;
        this.convexList.remove(item);
    }

    public void RemoveAt(int index) {
        this.area = 0.0;
        this.convexList.remove(index);
    }

    public void RemoveRange(int index, int count) {
        this.area = 0.0;
        for (int i = index; i<index+count; i++) {
            this.convexList.remove(i);
        }
    }

    public void Simplify(boolean simple_simplify, boolean eliminate,
                         boolean make_disjoint, boolean unify, DynSymMatrix collision) throws Exception {
        List<Convex> list = new ArrayList<>();
        for (Convex current : this.convexList) {
            if (!current.Simplify(simple_simplify)) {
                list.add(current);
            }
        }
        for(Convex current2 : list) {
            this.convexList.remove(current2);
        }
        if (eliminate) {
            this.EliminateConvexes();
        }
        if (make_disjoint) {
            this.convexList.sort((l, r) -> Convex.ComparisonAreaDescending(l, r));
            boolean flag = false;
            if (collision == null) {
                Pair<Boolean, DynSymMatrix> result = Convex.GetCollisions(this.convexList);
                flag = result.getX();
                collision = result.getY();
            }
            if (!flag) {
                this.MakeDisjoint(collision);
            }
        }
        if (unify) {
            this.StitchConvexes();
        }
        this.UpdateArea();
    }

    public void Simplify(boolean simple_simplify, boolean eliminate, boolean make_disjoint, boolean unify)
            throws Exception {
        this.Simplify(simple_simplify, eliminate, make_disjoint, unify, null);
    }

    public void Simplify() throws Exception {
        this.Simplify(true, true, true, true, null);
    }

    public boolean SmartContains(Cartesian x) {
        return this.SmartContains(x, Constant.CosTolerance, Constant.SinTolerance);
    }

    public boolean SmartContains(Cartesian x, double costol, double sintol) {
        for (Convex current : this.convexList) {
            if (current.SmartContains(x, costol, sintol)) {
                return true;
            }
        }
        return false;
    }

    public void SmartIntersect(Convex convex) throws Exception {
        List<Convex> list = new ArrayList<>();
        for (Convex current : this.convexList) {
            if (!current.SmartIntersect(convex)) {
                list.add(current);
            }
        }
        for (Convex current2 : list) {
            this.Remove(current2);
        }
        this.UpdateArea();
    }

    public Region SmartIntersect(Region region, boolean unify) throws Exception {
        Region region2 = new Region();
        for (Convex current : this.convexList) {
            for (Convex current2 : region.convexList) {
                Convex convex = new Convex(current);
                if (convex.SmartIntersect(current2)) {
                    region2.convexList.add(convex);
                }
            }
        }
        if (unify) {
            region2.StitchConvexes();
        }
        region2.UpdateArea();
        return region2;
    }

    public void SmartUnion(Region region, boolean unify) throws Exception {
        int count = this.convexList.size();
        region.convexList.replaceAll(Convex::Clone);
        this.convexList.addAll(region.convexList);
        DynSymMatrix dynSymMatrix = new DynSymMatrix(this.convexList.size());
        for (int i = count; i < this.convexList.size(); i++) {
            Convex convex = this.convexList.get(i);
            for (int j = 0; j < count; j++) {
                Convex c = this.convexList.get(j);
                if (convex.DoesCollide(c)) {
                    dynSymMatrix.set(i, j, true);
                }
            }
        }
        this.EliminateConvexes(dynSymMatrix);
        this.MakeDisjoint(dynSymMatrix);
        if (unify) {
            this.StitchConvexes();
        }
        this.UpdateArea();
    }

    public void Sort() {
        this.convexList.sort((l, r) -> (Convex.ComparisonAreaDescending(l, r)));
    }

    public void Sort(Comparator comparator) {
        this.convexList.sort(comparator);
    }

    private void StitchConvexes() throws Exception{
        List<Convex> list = new ArrayList<>();
        boolean flag = true;
        while (flag) {
            this.convexList.sort((l, r) -> (Convex.ComparisonAreaDescending(l, r)));
            flag = false;
            for (int i = 0; i < this.convexList.size() - 1; i++) {
                Convex convex = this.convexList.get(i);
                if (!list.contains(convex)) {
                    for (int j = i + 1; j < this.convexList.size(); j++) {
                        Convex convex2 = this.convexList.get(j);
                        if (!list.contains(convex2) && (convex.getESign() != ESign.Positive || convex2.getESign() != ESign
                                .Positive) && !convex.MecDisjoint(convex2) && convex.HasCommonHalfspace(convex2)) {
                            Convex convex3 = new Convex(convex2);
                            convex3.InvertHalfspaces();
                            convex3.Sort();
                            if (convex.HasOnlyOneCommonHalfspace(convex3)) {
                                Pair<Integer, Integer> idxResult = convex.IndexFirstCommonHalfspacePair(convex3);
                                int index = idxResult.getX();
                                int num = idxResult.getY();
                                int index2 = convex3.HalfspaceList().size() - 1 - num;
                                Convex convex4 = new Convex(convex2);
                                convex4.RemoveAt(index2);
                                convex4.AddRange(convex.HalfspaceList());
                                convex4.Simplify(true);
                                if (convex.Same(convex4)) {
                                    Convex convex5 = new Convex(convex);
                                    convex5.RemoveAt(index);
                                    convex5.AddRange(convex2.HalfspaceList());
                                    convex5.Simplify(true);
                                    if (convex2.Same(convex5)) {
                                        convex.RemoveAt(index);
                                        convex2.RemoveAt(index2);
                                        convex.AddRange(convex2.HalfspaceList());
                                        convex.Simplify(true);
                                        list.add(convex2);
                                        flag = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (Convex current : list) {
                this.convexList.remove(current);
            }
            list.clear();
        }
    }

    @Override
    public String toString() {
        return this.ToString("\r\n\t");
    }

    public String ToString(String sep) {
        String text = Constant.KeywordRegion;
        for (Convex current : this.convexList) {
            text = text + sep + current.toString(sep);
        }
        return text;
    }

    public void Union(Region region) {
        List<Convex> collection = new ArrayList<>(region.convexList);
        collection.replaceAll(Convex::Clone);
        this.convexList.addAll(collection);
        this.UpdateArea();
    }

    public void Union(Convex convex) {
        this.convexList.add(new Convex(convex));
        this.UpdateArea();
    }

    public void UpdateArea() {
        this.area = 0.0;
        for (Convex current : this.convexList) {
            this.area += current.getArea();
        }
    }
}
