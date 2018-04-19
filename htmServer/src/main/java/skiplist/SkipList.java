package skiplist;

import java.io.Serializable;

/**
 * A set of long intervals.
 *
 */

public interface SkipList extends Serializable {
    /**
     * Adds the given long to this set if it is not already present.
     *
     * @param n a long
     */
    void add(long n);

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
     * Determines the number of longs contained in this set.
     *
     * @return the size of this set
     */
    int size();

    /**
     * Finds the smallest long greater than or equal to the given
     * long that is not in this set.
     *
     * @param n an integer
     */
    long nextExcluded(long n);
}
