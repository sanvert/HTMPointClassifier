package spherical;

import spherical.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Hidranges {

    public List<Pair<Long, Long>> pairList = new ArrayList<>();

    public Hidranges() {
    }

    Hidranges(List<SmartTrixel> nodeList, boolean sortandcheck, int deepest) throws Exception {
        if (nodeList != null) {
            for (SmartTrixel qt : nodeList) {
                if (qt.Level <= deepest) {
                    Pair range = Trixel.Extend(qt.Hid(), 20);
                    this.AddRange(range);
                } else {
                    break;
                }
            }
            if (sortandcheck) {
                this.Sort();
                this.Compact();
                this.Check();
            }
        }
    }

    Hidranges(List<SmartTrixel> nodeList, boolean sortandcheck) throws Exception {
        if (nodeList != null) {
            for (SmartTrixel qt : nodeList) {
                Pair range = Trixel.Extend(qt.Hid(), 20);
                this.AddRange(range);
            }
            if (sortandcheck) {
                this.Sort();
                this.Compact();
                this.Check();
            }
        }
    }

    static private boolean isDisjoint(Pair<Long, Long> a, Pair<Long, Long> b) {
        return (a.getY() < b.getX() || b.getY() < a.getX());
    }

    public static List<Pair<Long, Long>> Combine(List<Pair<Long, Long>> ranges, List<Pair<Long, Long>> newranges) {
        List<Pair<Long, Long>> result = new ArrayList<>(ranges.size());
        Pair<Long, Long> proto = new Pair<>();
        // Pop one list into proto
        boolean protomod;
        boolean protovalid;
        if (newranges.size() < 1) {
            for (Pair p : ranges) {
                result.add(p);
            }
            return result;
        }

        // Build a new range
        int left = newranges.size() + ranges.size();
        int i = 0, j = 0;
        if (ranges.size() > 0 && ranges.get(0).getX() <= newranges.get(0).getX()) {
            proto.setX(ranges.get(0).getX());
            proto.setY(ranges.get(0).getY());
            protomod = true;
            protovalid = true;
            i++;
        } else {
            proto.setX(newranges.get(0).getX());
            proto.setY(newranges.get(0).getY());
            protomod = true;
            protovalid = true;
            j++;
        }
        left--;
        while (left > 0) {
            protomod = false; // See if this iteration modifies proto..
            if (!protovalid) {
                if (i >= ranges.size()) { // main list is exhausted
                    proto.setX(newranges.get(j).getX());
                    proto.setY(newranges.get(j).getY());
                    protomod = true;
                    protovalid = true;
                    j++;
                } else if (j >= newranges.size()) {   // newrange exhausted
                    proto.setX(ranges.get(i).getX());
                    proto.setY(ranges.get(i).getY());
                    protomod = true;
                    protovalid = true;
                    i++;
                } else if (ranges.size() > 0 && ranges.get(i).getX() <= newranges.get(j).getX()) {
                    proto.setX(ranges.get(i).getX());
                    proto.setY(ranges.get(i).getY());
                    protomod = true;
                    protovalid = true;
                    i++;
                } else {
                    proto.setX(newranges.get(j).getX());
                    proto.setY(newranges.get(j).getY());
                    protomod = true;
                    protovalid = true;
                    j++;
                }
                left--;
            }
            if (i < ranges.size()) {
                if (protovalid && !isDisjoint(proto, ranges.get(i))) {
                    proto.setX(Math.min(ranges.get(i).getX(), proto.getX()));
                    proto.setY(Math.max(ranges.get(i).getY(), proto.getY()));
                    i++;
                    left--;
                    protomod = true;
                }
            }
            // proto may have been modified above
            //
            if (j < newranges.size()) {
                if (protovalid && !isDisjoint(proto, newranges.get(j))) {
                    proto.setX(Math.min(newranges.get(j).getX(), proto.getX()));
                    proto.setY(Math.max(newranges.get(j).getY(), proto.getY()));
                    j++;
                    left--;
                    protomod = true;
                }
            }
            if (!protomod) { // proto was disjoint from both rangetops
                result.add(proto);
                protomod = false;
                protovalid = false;
            }
            // only continue, if both lists are nonempty
            //if (j >= newranges.Count || i >= pairList.Count) {
            //    break;
            //}
        }
        if (protovalid) {
            result.add(proto);
            protovalid = false;
        }
        return result;
    }
    /// <summary>
    /// merge a given list into the current list of pairs
    /// </summary>
    /// <param name="newranges"></param>


    void Merge(List<Pair<Long, Long>> newranges) {
        pairList = Combine(this.pairList, newranges);
    }

    /// <summary>
    /// Add pair of hids to  list of pairs. Does not do checking
    /// </summary>
    /// <param name="pair"></param>
    //TODO: make 
    public void AddRange(Pair pair) {
        pairList.add(pair);
    }

    /// <summary>
    /// add pair of hids to  list of pairs. Does not do checking
    /// </summary>
    /// <param name="lo"></param>
    /// <param name="hi"></param>
    //TODO: deprecate:
    public void AddRange(Long lo, Long hi) {
        Pair tp = new Pair();
        tp.setX(lo);
        tp.setY(hi);
        pairList.add(tp);
        return;
    }

    public void Clear() {
        pairList.clear();
    }

    /// <summary>
    /// Comparator for sorting ranges
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    public static int CompareTo(Pair<Long, Long> a, Pair<Long, Long> b) {
        return a.getX().compareTo(b.getX());
    }

    /// <summary>
    /// Ensure that ranges are disjoint
    /// </summary>
    public void Check() throws Exception {
        for (int i = 1; i < pairList.size(); i++) {
            if (pairList.get(i - 1).getY() > pairList.get(i).getX()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Range.check i={0}: ({1}, {2}), ({3}, {4})",
                        i, pairList.get(i - 1).getX(), pairList.get(i - 1).getY(),
                        pairList.get(i).getX(), pairList.get(i).getY()));

                throw new Exception(sb.toString());
            }
        }
    }

    /// <summary>
    /// Sort Interval by lows
    /// </summary>
    public void Sort() {
        pairList.sort((l, r) -> CompareTo(l, r));
    }

    /// <summary>
    /// perform sanity check for the health of sorted pairs
    /// throws an exception if check fails, else returns quietly
    /// </summary>
    /// <param name="sortedpairs"></param>
    /// <returns></returns>
    public static boolean IsWellFormed(List<Pair<Long, Long>> sortedpairs) throws Exception {
        for (int i = 1; i < sortedpairs.size(); i++) {
            if (sortedpairs.get(i - 1).getX() > sortedpairs.get(i).getX()) {
                throw new Exception("sorted pair is not sorted");
            }
            if (sortedpairs.get(i - 1).getY() + 1 >= sortedpairs.get(i).getX()) {
                throw new Exception("sorted pair is not disjoint from neighbor");
            }
        }
        return true;
    }

    /// <summary>
    /// Merge adjacent intervals, i.e., eliminate zero gaps
    /// </summary>
    //TODO: eliminate one of these (body of code)
    //TODO: work from back to front
    public void Compact() {
        Pair<Long, Long> tp = new Pair();
        int count;
        count = pairList.size();
        count--; // because we compare i to t+1
        for (int i = 0; i < count; ) { // third part purposely empty
            if (pairList.get(i).getY() + 1 == pairList.get(i + 1).getX()) {
                tp.setX(pairList.get(i).getX());
                tp.setY(pairList.get(i + 1).getY());
                pairList.remove(i + 1);
                pairList.remove(i);
                pairList.add(i, tp);
                count--;
            } else {
                i++;
            }
        }
        return;
    }

    /// <summary>
    /// Merge adjacent intervals
    /// </summary>
    /// <param name="sortedPairs"></param>
    public static void Compact(List<Pair<Long, Long>> sortedPairs) {
        Pair<Long, Long> tp = new Pair();
        int count;
        count = sortedPairs.size();
        count--; // because we compare i to t+1
        for (int i = 0; i < count; ) { // third part purposely empty
            if (sortedPairs.get(i).getY() + 1 == sortedPairs.get(i + 1).getX()) {
                tp.setX(sortedPairs.get(i).getX());
                tp.setY(sortedPairs.get(i + 1).getY());
                sortedPairs.remove(i + 1);
                sortedPairs.remove(i);
                sortedPairs.add(i, tp);
                count--;
            } else {
                i++;
            }
        }
        return;
    }
}