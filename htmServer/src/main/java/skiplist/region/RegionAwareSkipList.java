package skiplist.region;

import java.io.Serializable;
import java.util.Set;

/**
 * A set of long intervals.
 *
 */

public interface RegionAwareSkipList extends Serializable {
    /**
     * Adds the given long to this set if it is not already present.
     *
     * @param n a long
     */
    void add(Set<Integer> regionIdSet, long n);

    void addInterval(int regionId, long x, long y);
    /**
     * Removes the given long from this set if it is in the set.
     *
     * @param n an integer
     */
    void remove(long n);

    /**
     * Determines if ths given long is in this set.
     *
     * @param n a long
     * @return true if and only if n is in this set
     */
    boolean contains(long n);

    /**
     * Search and return if possible region ids of HTM.
     * @param n point HTM id
     * @return
     */
    Set<Integer> regionIdSet(long n);

    /**
     * Determines the number of longs contained in this set.
     *
     * @return the size of this set
     */
    int size();
}
