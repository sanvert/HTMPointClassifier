package skiplist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IntervalSkipList implements SkipList {
    /**
     * Unique id.
     */
    private String id;
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
    public IntervalSkipList(String listId) {
        id = listId;
        head = new Node(Long.MIN_VALUE, Long.MIN_VALUE, 1);
        tail = new Node(Long.MAX_VALUE, Long.MAX_VALUE, 1);
        head.next.set(0, tail);
        nodeCounts = new ArrayList<>();
        nodeCounts.add(0); // NOT counting head and tail
    }

    /**
     * Checks if key is inside any of intervals and retrieve the one contains key.
     * @param key
     * @return Node contains key.
     */
    public Node isInside(long key) {
        Node curr = head;
        int level = head.next.size() - 1;
        while (level >= 0) {
            while (curr.next.get(level).end < key) {
                if(curr.next.get(level).compareTo(key) == 0) {
                    return curr;
                }
                curr = curr.next.get(level);
            }
            level--;
        }

        return curr.next.get(0).compareTo(key) == 0 ? curr.next.get(0) : null;
    }

    public void addInterval(long start, long end) {
        if(start > end) {
            long temp = start;
            start = end;
            end = temp;
        } else if(start == end) {
            add(start);
            return;
        }

        List<Node> lastStart = find(start);
        List<Node> lastEnd = find(end);
        if(lastStart.get(0).next.get(0).compareTo(start) == 0 &&
                lastEnd.get(0).next.get(0).compareTo(end) == 0) {
            start = Math.min(lastStart.get(0).next.get(0).start, start);
            end = Math.max(lastEnd.get(0).next.get(0).end, end);
            deleteNodes(lastStart, lastEnd);
            addInterval(start, end);
        } else if (lastStart.get(0).next.get(0).compareTo(start) == 0) {
            lastStart.get(0).next.get(0).end = end;
        } else if (lastEnd.get(0).next.get(0).compareTo(end) == 0) {
            lastEnd.get(0).next.get(0).start = start;
        } else {
            addNewNode(lastStart, start, end);
        }

    }

    public void deleteNodes(List<Node> lastStart, List<Node> lastEnd) {
        deleteNode(lastStart, lastStart.get(0).next.get(0));
        deleteNode(lastEnd, lastEnd.get(0).next.get(0));
    }

    private void addNewNode(List<Node> last, long start, long end) {
        // not found -- add
        int newHeight = chooseHeight();

        // ensure that the head node and the last references are
        // at least newHeight high
        while (head.next.size() < newHeight) {
            head.addLevel();
            tail.addLevel();
            head.next.set(head.next.size() - 1, tail);
            last.add(head);
            nodeCounts.add(0);
        }

        // make the new node
        Node newNode = new Node(start, end, newHeight);

        for (int h = 0; h < newHeight; h++) {
            // count the new node
            nodeCounts.set(h, nodeCounts.get(h) + 1);
            // link up the new node
            Node oldNext = last.get(h).next.get(h);
            last.get(h).next.set(h, newNode);
            newNode.next.set(h, oldNext);
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
     * <code>last.get(0).next.get(0).equals(key)</code>
     *
     * @param key a long in [Long.MIN_VALUE+1, Long.MAX_VALUE]
     * @return a list of nodes before the key at each level
     */
    private List<Node> find(long key) {
        List<Node> last = new ArrayList<>(head.next.size());
        for (int i = 0; i < head.next.size(); i++) {
            last.add(head);
        }

        Node curr = head;
        int level = head.next.size() - 1;
        while (level >= 0) {
            while (curr.next.get(level).end < key) {
                curr = curr.next.get(level);
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

        return last.next.get(0).compareTo(value) == 0;
    }

    /**
     * Adds the given key to this skip list if it is not already present.
     *
     * @param key a long in [Long.MIN_VALUE+2, Long.MAX_VALUE-2]
     */
    public void add(long key) {
        List<Node> last = find(key);
        Node lastNode = last.get(0);
        Node curr = last.get(0).next.get(0);

        if (curr.start - 1 == key && lastNode.end + 1 == key) {
            lastNode.end = curr.end;
            deleteNode(last, curr);
        } else if (curr.start - 1 == key) {
            curr.start = key;
        } else if (lastNode.end + 1 == key) {
            lastNode.end = key;
        } else if (curr.compareTo(key) != 0) {
            addNewNode(last, key, key);
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
        Node toDelete = last.get(0).next.get(0);

        if (toDelete.compareTo(value) == 0) {
            // found -- delete
            deleteNode(last, toDelete);
            for (long i = toDelete.start; i <= toDelete.end; i++) {
                if(i != value) {
                    add(i);
                }
            }
        }
    }

    /**
     * Deletes the given node from this skip list.
     *
     * @param last the largest node strictly before toDelete at each level
     * @param toDelete a node in this skip list other than the head and tail
     */
    private void deleteNode(List<Node> last, Node toDelete) {
        // link around deleted node
        int height = toDelete.next.size();
        for (int h = 0; h < height; h++) {
            last.get(h).next.set(h, toDelete.next.get(h));
            nodeCounts.set(h, nodeCounts.get(h) - 1);
        }

        // remove extra levels in head and tail nodes
        while (head.next.size() > 1 && nodeCounts.get(head.next.size() - 1) == 0) {
            head.next.remove(head.next.size() - 1);
            tail.next.remove(tail.next.size() - 1);
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
        Node curr = last.next.get(0);

        // now look at that node -- does it contain n?
        if (curr.compareTo(n) != 0) {
            // no? then n itself is the next excluded
            return n;
        } else {
            return curr.end + 1;
        }
    }

    /**
     * Returns a printable representation of this skip list.
     *
     * @return a printable representation of this skip list
     */
    public String toString() {
        StringBuilder out = new StringBuilder();
        Node curr = head.next.get(0);
        while (curr != tail)
        {
            out.append(curr.toString());
            curr = curr.next.get(0);
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
        while(Math.random() < 0.5) {
            height++;
        }
        return height;
    }

    public String getId() {
        return id;
    }

    /**
     * A node in a skip list.
     */
    public static class Node implements Serializable {
        private long start;
        private long end;

        private List<Node> next;

        /**
         * Creates a new node of the given level holding the given data.
         *
         * @param start the interval start
         * @param end the interval end
         * @param h a non-negative integer for the height of the new node
         */
        public Node(long start, long end, int h) {
            this.start = start;
            this.end = end;
            next = new ArrayList<>(h);
            for (int i = 0; i < h; i++) {
                next.add(null);
            }
        }

        /**
         * Adds a level to this node.
         */
        public void addLevel()
        {
            next.add(null);
        }

        public int compareTo(long n) {
            return n > this.end ? 1 : n < this.start ? -1 : 0;
        }

        public String toString()
        {
            return "<" + start + ":" + end + "; " + next.size() + ">";
        }
    }

    // Tests
    public static void main(String[] args) {
        IntervalSkipList s = new IntervalSkipList("0");

        int[] testValues = {2, 1, 4, 6, 8, 3, 9, 10, 5, 7, 23, 24, 25, 34, 35, 36, 37, 45, 46, 58, 59, 60};

        for (int i : testValues) {
            System.out.printf("=== ADDING %d ===\n", i);
            s.add(i);
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
            System.out.printf("=== REMOVING %d ===\n", i);
            s.remove(i);
            System.out.println(s);
        }

        System.out.println("=== ADDING BACK ===");
        for (int i : testValues) {
            s.add(i);
        }
        System.out.println(s);

        s = new IntervalSkipList("0");
        System.out.println(s.isInside(25));
        s.addInterval(24, 45);
        System.out.println(s);
        System.out.println(s.isInside(25));
        s.addInterval(101, 111);
        s.addInterval(10, 19);
        s.addInterval(88, 99);
        System.out.println(s);
        System.out.println(s.isInside(90));

    }

}