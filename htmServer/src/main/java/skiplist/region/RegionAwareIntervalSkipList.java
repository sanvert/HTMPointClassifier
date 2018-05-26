package skiplist.region;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegionAwareIntervalSkipList implements IntervalSkipList {
    /**
     * Head node in skip list.
     */
    private Node head;

    /**
     * Tail node in skip list.
     */
    private Node tail;

    /**
     * The count of other nodes than head and tail at each level.
     */
    private List<Integer> nodeCounts;

    /**
     * Creates an empty skip list.
     */
    public RegionAwareIntervalSkipList() {
        Set<Integer> initSet = new HashSet<>();
        head = new Node(initSet, Long.MIN_VALUE, Long.MIN_VALUE, 1);
        tail = new Node(initSet, Long.MAX_VALUE, Long.MAX_VALUE, 1);
        head.getNext().set(0, tail);
        nodeCounts = new ArrayList<>();
        nodeCounts.add(0); // NOT counting head and tail
    }

    /**
     * Checks if key is inside any of intervals and retrieve the one contains key.
     *
     * @param key
     * @return Node contains key.
     */
    public Node isInside(long key) {
        Node curr = head;
        int level = head.getNext().size() - 1;
        while (level >= 0) {
            while (curr.getNext().get(level).getEnd() < key) {
                if (curr.getNext().get(level).compareTo(key) == 0) {
                    return curr;
                }
                curr = curr.getNext().get(level);
            }
            level--;
        }

        return curr.getNext().get(0).compareTo(key) == 0 ? curr.getNext().get(0) : null;
    }

    public void addInterval(int regionId, long start, long end) {
        Set<Integer> regionIdSet = new HashSet<>();
        regionIdSet.add(regionId);

        addInterval(regionIdSet, start, end);
    }

    private void addInterval(Set<Integer> regionIdSet, long start, long end) {
        if (start == end) {
            add(regionIdSet, start);
            return;
        }

        if (start > end) {
            long temp = start;
            start = end;
            end = temp;
        }

        addIntervalRange(regionIdSet, start, end);
    }


    private void addIntervalRange(Set<Integer> regionIdSet, long start, long end) {
        //OK
        List<Node> lastStart = find(start);
        List<Node> lastEnd = find(end);
        if (lastStart.get(0).getNext().get(0).compareTo(start) == 0 &&
                lastEnd.get(0).getNext().get(0).compareTo(end) == 0) {

            Node currentStart = lastStart.get(0).getNext().get(0);
            Set<Integer> currentStartNodeSet = currentStart.getRegionIdSet();
            Node currentEnd = lastEnd.get(0).getNext().get(0);
            Set<Integer> currentEndNodeSet = currentEnd.getRegionIdSet();

            if(currentStart == currentEnd) {
                currentStart.addRegionIds(regionIdSet);
            } else {
                removeInterval(lastStart, lastEnd);

                currentStartNodeSet.removeAll(regionIdSet);
                currentEndNodeSet.removeAll(regionIdSet);
                if(currentStartNodeSet.isEmpty() && currentEndNodeSet.isEmpty()) {
                    addInterval(regionIdSet, currentStart.getStart(), currentEnd.getEnd());
                } else if(currentStartNodeSet.isEmpty()) {
                    //Add start node interval combined non-conflicting part of new interval.
                    addInterval(regionIdSet, currentStart.getStart(), currentEnd.getStart() - 1);
                    //Add new interval conflicting part.
                    regionIdSet.addAll(currentEnd.getRegionIdSet());
                    addInterval(regionIdSet, currentEnd.getStart(), Math.min(end, currentEnd.getEnd()));
                    //In case end is less than current end.
                    if(end < currentEnd.getEnd()) {
                        addInterval(currentEnd.getRegionIdSet(), end + 1, currentEnd.getEnd());
                    }
                } else if(currentEndNodeSet.isEmpty()) {
                    //Add end node interval combined non-conflicting part of new interval.
                    addInterval(regionIdSet, currentStart.getEnd() + 1, currentEnd.getEnd());
                    //Add new interval conflicting part.
                    regionIdSet.addAll(currentStart.getRegionIdSet());
                    addInterval(regionIdSet, Math.max(start, currentStart.getStart()), currentStart.getEnd());
                    if(start > currentStart.getStart()) {
                        addInterval(currentStart.getRegionIdSet(), currentStart.getStart(), start - 1);
                    }
                } else {
                    //Add left interval.
                    if(start > currentStart.getStart()) {
                        addInterval(currentStart.getRegionIdSet(), currentStart.getStart(), start - 1);
                    }
                    Set<Integer> leftConflictingSet = new HashSet<>(regionIdSet);
                    leftConflictingSet.addAll(currentStart.getRegionIdSet());
                    addInterval(leftConflictingSet, start, currentStart.getEnd());
                    //Add medium interval.
                    addInterval(regionIdSet, currentStart.getEnd() + 1, currentEnd.getStart() - 1);
                    //Add right interval.
                    Set<Integer> rightConflictingSet = new HashSet<>(regionIdSet);
                    rightConflictingSet.addAll(currentEnd.getRegionIdSet());
                    addInterval(rightConflictingSet, currentEnd.getStart(), end);
                    if(end < currentEnd.getEnd()) {
                        addInterval(currentEnd.getRegionIdSet(), end + 1, currentEnd.getEnd());
                    }
                }
            }

        } else if (lastStart.get(0).getNext().get(0).compareTo(start) == 0) {
            //OK
            Node current = lastStart.get(0).getNext().get(0);
            Set<Integer> currentNodeSet = current.getRegionIdSet();
            currentNodeSet.removeAll(regionIdSet);
            if (currentNodeSet.isEmpty()) {
                current.setEnd(end);
            } else {
                if(current.getStart() == start) {
                    start = current.getEnd() + 1;
                    current.addRegionIds(regionIdSet);
                } else {
                    current.setEnd(start - 1);
                    regionIdSet.addAll(current.getRegionIdSet());
                }
                addInterval(regionIdSet, start, end);
            }
        } else if (lastEnd.get(0).getNext().get(0).compareTo(end) == 0) {
            //OK
            Node current = lastEnd.get(0).getNext().get(0);
            Set<Integer> currentNodeSet = current.getRegionIdSet();
            currentNodeSet.removeAll(regionIdSet);
            if (currentNodeSet.isEmpty()) {
                current.setStart(start);
            } else {
                if(current.getEnd() == end) {
                    end = current.getStart() - 1;
                    current.addRegionIds(regionIdSet);
                } else {
                    current.setStart(end + 1);
                    regionIdSet.addAll(current.getRegionIdSet());
                }
                addInterval(regionIdSet, start, end);
            }
        } else {
            //OK
            addNewNode(regionIdSet, lastStart, start, end);
        }
    }

    public void removeInterval(List<Node> lastStart, List<Node> lastEnd) {
        deleteNode(lastStart, lastStart.get(0).getNext().get(0));
        deleteNode(lastEnd, lastEnd.get(0).getNext().get(0));
    }

    private void addNewNode(Set<Integer> regionIdSet, List<Node> last, long start, long end) {
        int newHeight = chooseHeight();

        // ensure that the head node and the last references are
        // at least newHeight high
        while (head.getNext().size() < newHeight) {
            head.addLevel();
            tail.addLevel();
            head.getNext().set(head.getNext().size() - 1, tail);
            last.add(head);
            nodeCounts.add(0);
        }

        // make the new node
        Node newNode = new Node(regionIdSet, start, end, newHeight);

        for (int h = 0; h < newHeight; h++) {
            // count the new node
            nodeCounts.set(h, nodeCounts.get(h) + 1);
            // link up the new node
            Node oldNext = last.get(h).getNext().get(h);
            last.get(h).getNext().set(h, newNode);
            newNode.getNext().set(h, oldNext);
        }
    }

    /**
     * Returns the size of this skip list.
     *
     * @return the size of this skip list
     */
    public int size() {
        // from an external point of view, size does not include the head
        return nodeCounts.get(0);
    }

    /**
     * Finds the node with the given value in this skip list.  The returned
     * list is a list of nodes, one per level, such that the next references
     * at the nodes from the corresponding levels is either the node containing
     * the data to search for or the 1st node beyond it.  The value to search
     * for is in this skip list if and only if
     * <code>last.get(0).getNext().get(0).equals(key)</code>
     *
     * @param key a long in [Long.MIN_VALUE+1, Long.MAX_VALUE]
     * @return a list of nodes before the key at each level
     */
    private List<Node> find(long key) {
        List<Node> last = new ArrayList<>(head.getNext().size());
        for (int i = 0; i < head.getNext().size(); i++) {
            last.add(head);
        }

        Node curr = head;
        int level = head.getNext().size() - 1;
        while (level >= 0) {
            while (curr.getNext().get(level).getEnd() < key) {
                curr = curr.getNext().get(level);
            }
            last.set(level, curr);
            level--;
        }

        return last;
    }

    /**
     * Determines if the given value is present in this skip list.
     *
     * @param value a long in [Long.MIN_VALUE+1, Long.MAX_VALUE-1]
     * @return true if that value is in this skip list and false otherwise
     */
    public boolean contains(long value) {
        Node last = find(value).get(0);

        return last.getNext().get(0).compareTo(value) == 0;
    }

    /**
     * Adds the given key to this skip list if it is not already present.
     *
     * @param key a long in [Long.MIN_VALUE+2, Long.MAX_VALUE-2]
     */
    public void add(Set<Integer> regionIdSet, long key) {
        List<Node> last = find(key);
        Node lastNode = last.get(0);
        Node curr = last.get(0).getNext().get(0);

        if (curr.getStart() - 1 == key && lastNode.getEnd() + 1 == key) {
            //OK
            deleteNode(last, curr);

            Set<Integer> lastNodeConflictingSet = lastNode.getRegionIdSet();
            lastNodeConflictingSet.removeAll(regionIdSet);

            Set<Integer> currNodeConflictingSet = curr.getRegionIdSet();
            currNodeConflictingSet.removeAll(regionIdSet);

            if(lastNodeConflictingSet.isEmpty() && currNodeConflictingSet.isEmpty()) {
                lastNode.setEnd(curr.getEnd());
            } else if(lastNodeConflictingSet.isEmpty()) {
                lastNode.setEnd(key);
                addNewNode(curr.getRegionIdSet(), last, curr.getStart(), curr.getEnd());
            } else if(currNodeConflictingSet.isEmpty()) {
                addNewNode(curr.getRegionIdSet(), last, key, curr.getEnd());
            } else {
                addNewNode(regionIdSet, last, key, key);
                last = find(curr.getStart());
                addNewNode(curr.getRegionIdSet(), last, curr.getStart(), curr.getEnd());
            }
        } else if (curr.getStart() - 1 == key) {
            //OK
            Set<Integer> diffSet = curr.getRegionIdSet();
            diffSet.removeAll(regionIdSet);
            if(diffSet.isEmpty()) {
                curr.setStart(key);
            } else {
                addNewNode(regionIdSet, last, key, key);
            }
        } else if (lastNode.getEnd() + 1 == key) {
            //OK
            Set<Integer> diffSet = curr.getRegionIdSet();
            diffSet.removeAll(regionIdSet);
            if(diffSet.isEmpty()) {
                lastNode.setEnd(key);
            } else {
                addNewNode(regionIdSet, last, key, key);
            }
        } else if (curr.compareTo(key) != 0) {
            //OK
            addNewNode(regionIdSet, last, key, key);
        }
    }

    /**
     * Removes the given long from this set if it is in the set.
     *
     * @param value a long
     */
    public void remove(long value) {
        // find the previous value at each level
        List<Node> last = find(value);
        Node toDelete = last.get(0).getNext().get(0);

        if (toDelete.compareTo(value) == 0) {
            // found -- delete
            deleteNode(last, toDelete);
            for (long i = toDelete.getStart(); i <= toDelete.getEnd(); i++) {
                if (i != value) {
                    add(toDelete.getRegionIdSet(), i);
                }
            }
        }
    }

    /**
     * Deletes the given node from this skip list.
     *
     * @param last     the largest node strictly before toDelete at each level
     * @param toDelete a node in this skip list other than the head and tail
     */
    private void deleteNode(List<Node> last, Node toDelete) {
        // link around deleted node
        int height = toDelete.getNext().size();
        for (int h = 0; h < height; h++) {
            last.get(h).getNext().set(h, toDelete.getNext().get(h));
            nodeCounts.set(h, nodeCounts.get(h) - 1);
        }

        // remove extra levels in head and tail nodes
        while (head.getNext().size() > 1 && nodeCounts.get(head.getNext().size() - 1) == 0) {
            head.getNext().remove(head.getNext().size() - 1);
            tail.getNext().remove(tail.getNext().size() - 1);
            nodeCounts.remove(nodeCounts.size() - 1);
        }
    }

    /**
     * Finds the smallest long greater than or equal to the given
     * long that is not in this set.
     *
     * @param n a long
     */
    public long nextExcluded(long n) {
        // find the node before where n would go...
        Node last = find(n).get(0);

        // ...and the node after that node
        Node curr = last.getNext().get(0);

        // now look at that node -- does it contain n?
        if (curr.compareTo(n) != 0) {
            // no? then n itself is the next excluded
            return n;
        } else {
            return curr.getEnd() + 1;
        }
    }

    /**
     * Returns a printable representation of this skip list.
     *
     * @return a printable representation of this skip list
     */
    public String toString() {
        StringBuilder out = new StringBuilder();
        Node curr = head.getNext().get(0);
        while (curr != tail) {
            out.append(curr.toString());
            curr = curr.getNext().get(0);
        }
        return out.toString();
    }

    /**
     * Randomly returns a height.  Height 1 is returned with probability 1/2;
     * 2 with probability 1/4, ...
     *
     * @return a randomly chosen height
     */
    private int chooseHeight() {
        int height = 1;
        while (Math.random() < 0.5) {
            height++;
        }
        return height;
    }

    // Test
    public static void main(String[] args) {
        RegionAwareIntervalSkipList s = new RegionAwareIntervalSkipList();

        int[] testValues = {2, 1, 4, 6, 8, 3, 9, 10, 5, 7, 23, 24, 25, 34, 35, 36, 37, 45, 46, 58, 59, 60};

        Set<Integer> def = new HashSet<>();
        def.add(1);

        for (int i : testValues) {
            System.out.printf("-- ADDING %d --\n", i);
            s.add(def, i);
            System.out.println(s);
            for (int j = 0; j <= 11; j++) {
                System.out.printf("nextExcluded(%d) = %d\n", j, s.nextExcluded(j));
            }
        }

        for (int i = 23; i <= 34; i++) {
            System.out.println(i + ": " + s.contains(i));
        }
        System.out.println(s);

        int[] removeValues = {1, 10, 5, 2, 4, 8, 7, 6, 9, 3};
        for (int i : removeValues) {
            System.out.printf("-- REMOVING %d --\n", i);
            s.remove(i);
            System.out.println(s);
        }

        System.out.println("-- ADDING BACK --");
        for (int i : testValues) {
            s.add(def, i);
        }
        System.out.println(s);

        s = new RegionAwareIntervalSkipList();
        System.out.println(s.isInside(25));
        s.addInterval(def, 24, 45);
        System.out.println(s);
        System.out.println(s.isInside(25));
        s.addInterval(def, 101, 111);
        s.addInterval(def, 10, 19);
        s.addInterval(def, 88, 99);
        System.out.println(s);
        System.out.println(s.isInside(90));

    }

}