package skiplist.region;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A node in a skip list.
 */
public class Node implements Serializable {
    private Set<Integer> regionIdSet;
    private long start;
    private long end;

    private List<Node> next;

    /**
     * Creates a new node of the given level holding the given data.
     *
     *
     * @param regionIdSet set of regions tied to trixel set
     * @param start the interval start
     * @param end   the interval end
     * @param h     a non-negative integer for the height of the new node
     */
    public Node(Set<Integer> regionIdSet, long start, long end, int h) {
        this.regionIdSet = new HashSet<>(regionIdSet);

        this.start = start;
        this.end = end;
        next = new ArrayList<>(h);
        for (int i = 0; i < h; i++) {
            next.add(null);
        }
    }

    public List<Node> getNext() {
        return next;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    public long getStart() {
        return start;
    }

    public void setEnd(final long end) {
        this.end = end;
    }

    public long getEnd() {
        return end;
    }


    public void addRegionIds(Set<Integer> newRegionIds) {
        this.regionIdSet.addAll(newRegionIds);
    }

    public Set<Integer> getRegionIdSet() {
        return new HashSet<>(regionIdSet);
    }

    /**
     * Adds a level to this node.
     */
    public void addLevel() {
        next.add(null);
    }

    public int compareTo(long n) {
        return n > this.end ? 1 : n < this.start ? -1 : 0;
    }

    public String toString() {
        return "<" + start + ":" + end + "; " + next.size() + ">";
    }
}
