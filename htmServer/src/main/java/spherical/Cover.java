package spherical;

import spherical.refs.Arc;
import spherical.refs.Constant;
import spherical.refs.Convex;
import spherical.refs.IPatch;
import spherical.refs.Outline;
import spherical.refs.Patch;
import spherical.refs.Region;
import spherical.util.AugPair;
import spherical.util.Pair;
import spherical.util.Triple;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;

import static spherical.Markup.Inner;
import static spherical.Markup.Outer;
import static spherical.Markup.Partial;

public class Cover {
    /// <summary>
    /// stores the Region object
    /// </summary>
    Region reg;
    /// <summary>
    /// The list of patches making up the outline of the region.
    /// </summary>
    Outline outline;
    // <summary>
    /// Dictionary that returns a SmartArc
    /// </summary>
    private Map<Arc, SmartArc> smartArcTable;
    /// <summary>
    /// Keep track of all partial trixels for each level
    /// </summary>
    Stack<List<SmartTrixel>> stackOfPartialLists;
    /// <summary>
    /// List of inner trixels
    /// </summary>
    List<SmartTrixel> listOfInners;
    /// <summary>
    /// Current list of partials. Always selected from the stack of partials
    /// </summary>
    private List<SmartTrixel> savedListOfPartials;
    /// <summary>
    /// The queue for storing node : breadth-first search
    /// </summary>
    Deque<SmartTrixel> smartQue = new LinkedBlockingDeque<>(100);
    int currentLevel;
    private int previousLevel;
    private HaltCondition haltcondition;
    /// <summary>
    /// Used for evaluating halt condition. Prevents going too far
    /// </summary>
    int maxRanges;
    /// <summary>
    /// used for evaluating halt condition. Prevents stopping too early
    /// </summary>
    int minRanges;
    /// <summary>
    /// used for evaluating halt condition. Prevents stopping too early
    /// </summary>
    int minLevel;
    /// <summary>
    /// Used for evaluating halt condition. Prevents going too far
    /// </summary>
    int maxLevel;

    /// <summary>
    /// Process current smart trixel. called from machine's Step() function
    /// </summary>
    /// <param name="sq"></param>
    private void Visit(SmartTrixel sq) throws Exception {
        Markup m = sq.GetMarkup();
        switch (m) {
            case Inner:
                listOfInners.add(sq);
                sq.Terminal = true;
                break;
            case Reject:
                sq.Terminal = true;
                break;
            case Partial:
                stackOfPartialLists.peek().add(sq);
                break;
        }
    }

    /// <summary>
    /// Evaluate current cost : terms of number of
    /// </summary>
    /// <param name="kind"></param>
    /// <returns></returns>
    int Cost(Markup kind) throws Exception {
        switch (kind) {
            case Outer:
                return RowCost(stackOfPartialLists.peek(), listOfInners);

            case Inner:
                return RowCost(null, listOfInners);

            case Partial:
                return RowCost(stackOfPartialLists.peek(), null);

        }
        return -1;
    }

    /// <summary>
    /// Compute the number of rows that would be returned
    /// </summary>
    /// <param name="partials">list of partial trixels</param>
    /// <param name="inners">list of inner trixels</param>
    /// <returns></returns>
    private static int RowCost(List<SmartTrixel> partials, List<SmartTrixel> inners) throws Exception {
        Hidranges PR = new Hidranges(partials, true);
        Hidranges ran = new Hidranges(inners, true);
        ran.Merge(PR.pairList);
        ran.Sort();
        ran.Compact();
        ran.Check();
        return ran.pairList.size();
    }
    /// <summary>
    /// Given some idea about the size of features : a region,
    /// this allows limiting the levels to which search for cover proceeds
    /// </summary>

    void estimateMinMaxLevels() {

        Double featuresize = 1.0;
        Double a;

        Double minArea = Constant.WholeSphereInSquareDegree, maxArea = 0.0;
        for (Convex con : this.reg.ConvexList()) {
            for (Patch p : con.PatchList()) {
                a = p.getArea();
                if (a < 0.0 + Trixel.DblTolerance) {
                    a += Constant.WholeSphereInSquareDegree;
                }
                if (minArea > a) minArea = a;
                if (maxArea < a) maxArea = a;
            }
        }


        featuresize = Math.sqrt(maxArea * 2.0);
        featuresize *= Constant.Degree2Radian;
        minLevel = HtmState.getInstance().getLevel(featuresize);

        featuresize = Math.sqrt(minArea / 2.0);
        featuresize *= Constant.Degree2Radian;
        maxLevel = HtmState.getInstance().getLevel(featuresize);
        if (maxLevel < minLevel + HtmState.getInstance().hdelta()) {
            maxLevel += HtmState.getInstance().hdelta() / 2;
            minLevel -= HtmState.getInstance().hdelta() / 2;
        }
        //
        // Min must never be below 3
        //
        int nudgeup = minLevel - 3;
        if (nudgeup < 0) {
            minLevel -= nudgeup;
            maxLevel -= nudgeup;
        }

        maxLevel += HtmState.getInstance().deltalevel();
        minLevel += HtmState.getInstance().deltalevel();
    }

    /// <summary>
    /// Relate an Arc to a SmartArc
    /// </summary>
    /// <param name="ol"></param>
    private void buildArcTable(Outline ol) throws Exception {
        this.smartArcTable = new HashMap<>();
        for (IPatch patch : ol.getPatchList()) {
            for (Arc a : patch.getArcList()) {
                this.smartArcTable.put(a, new SmartArc(a));
            }
        }
        return;
    }

    // PRIVATE MEMBERS

    /// <summary>
    /// Try to return at least this many ranges, default value
    /// </summary>
    static int DefaultMinRanges = 12;
    /// <summary>
    /// Try to return at most this many ranges, default value
    /// </summary>
    static int DefaultMaxRanges = 20;

    private SmartVertex[] points;

    /// <summary>
    /// Initialize internals
    /// </summary>
    /// <param name="r"></param>
    public void Init(Region r) throws Exception {
        this.reg = r;

        this.listOfInners = new ArrayList<>();
        this.stackOfPartialLists = new Stack<>();
        this.stackOfPartialLists.push(new ArrayList<>());

        this.savedListOfPartials = null; // When we pop, we save one popped level
        this.currentLevel = 0;

        this.previousLevel = -1;
        this.estimateMinMaxLevels(); // , out _minlevel, out _maxlevel);
        this.minRanges = Cover.DefaultMinRanges;
        this.maxRanges = Cover.DefaultMaxRanges;

        //
        // Make outline
        //
        this.outline = new Outline(this.reg.Patches());
        buildArcTable(this.outline);

        List<IPatch> plist = new ArrayList<>();
        for (IPatch p : outline.PartList()) {
            plist.add(p);
        }

        // /////////////// Initialize base polyhedron

        points = new SmartVertex[6];
        HtmState htm = HtmState.getInstance();
        for (int i = 0; i < 6; i++) {
            points[i] = new SmartVertex(htm.Originalpoint(i), true);
            points[i].SetParentArcsAndTopo(plist, reg);
        }

        SmartTrixel root = new SmartTrixel(this);
        for (int i = 0; i < 8; i++) {
            Pair<Long, Triple<Integer, Integer, Integer>> res = htm.Face(i);
            this.smartQue.add(
                    new SmartTrixel(root, res.getX(),
                            points[res.getY().getX()], points[res.getY().getY()], points[res.getY().getZ()]));
        }
    }

    /// <summary>
    /// Create a Cover object with the given Region, and initialize the
    /// cover machine.
    /// </summary>
    /// <param name="reg">Region object for which a trixel covering will be generated</param>
    public Cover(Region reg) throws Exception {
        this.Init(reg);
    }

    /// <summary>
    /// Get the SmartArc related to the given Arc
    /// </summary>
    /// <param name="arc"></param>
    /// <returns>The SmartArc, or null</returns>
    SmartArc GetSmartArc(Arc arc) {
        return this.smartArcTable.get(arc);
    }

    /// <summary>
    /// Get the Region object associated with this Cover.
    /// </summary>
    /// <returns>the Region object</returns>
    public Region getRegion() {
        return this.reg;
    }

    /// <summary>
    /// Compute the pseudoare of selected part of the covering
    ///
    /// </summary>
    /// <param name="kind">One of {Inner, Outer, Partial}</param>
    /// <returns></returns>
    public Long GetPseudoArea(Markup kind) {
        Long pa = 0L;
        if (kind == Inner || kind == Outer) {
            for (SmartTrixel qt : this.listOfInners) {
                pa += Cover.PseudoArea(qt.Hid());
            }
        }
        if (kind == Outer || kind == Partial) {
            for (SmartTrixel qt : this.stackOfPartialLists.peek()) {
                pa += Cover.PseudoArea(qt.Hid());
            }
        }
        return pa;
    }

    /// <summary>
    /// Compute the cost : resources : the current state of the Cover object.
    /// </summary>
    /// <returns>the cost</returns>
    public int Cost() throws Exception {
        return this.Cost(Outer);
    }

    /// <summary>
    /// Return the current level of trixels : the current state of the covering.
    /// Levels run from 0 to 24.
    /// </summary>
    /// <returns>level number</returns>
    public int getLevel() {
        return this.currentLevel;
    }

    /// <summary>
    /// Get the current maximum level : effect
    /// </summary>
    /// <returns>integer between 0 and 20, inclusive</returns>
    public int getMaxLevel() {
        return this.maxLevel;
    }

    /// <summary>
    /// Change the tunable parameters for the generation of the covering
    /// </summary>
    /// <param name="minRanges"></param>
    /// <param name="maxRanges"></param>
    /// <param name="maxLevel"></param>
    public void setTunables(int minr, int maxr, int maxl) {
        if (minr > 0) {
            this.minRanges = minr;
        }
        if (maxr > 0) {
            this.maxRanges = maxr;
        }
        this.maxLevel = maxl;
    }

    /// <summary>
    /// compute haltcondition for this cover generation
    /// </summary>
    /// <returns></returns>
    private HaltCondition evaluateCurrentLevel() throws Exception {
        int computedcost = Cost(Outer);
        // ########################## STOP CRITERIA #############################
        // Stop whenever one of these is true:
        // (1) If the budget was exceeded, output stuff from previous level
        //     (backup)
        // (2) if level exceeds maxlevel, use current level (hold)
        // (3) If the budget was below limit, but above min. cost (hold)
        //
        // Otherwise continue (go on)

        HaltCondition halt = HaltCondition.Continue;
        if (computedcost > maxRanges) { // (1)
            halt = HaltCondition.Backup;
        } else if (this.currentLevel >= maxLevel) { // (2)
            halt = HaltCondition.Hold;
        } else if (computedcost > minRanges && computedcost <= maxRanges) { // (3)
            halt = HaltCondition.Hold;
        }
        return halt;
    }

    /// <summary>
    /// Run this cover machine until completion.
    /// </summary>
    public void Run() throws Exception {
        if (reg == null)
            return;
        haltcondition = HaltCondition.Continue;
        while (haltcondition == HaltCondition.Continue && smartQue.size() > 0) {
            step();
            haltcondition = this.evaluateCurrentLevel();
        }
        if (haltcondition == HaltCondition.Backup) {
            savedListOfPartials = stackOfPartialLists.pop();
            currentLevel--;
        } else {
            savedListOfPartials = null;
        }
    }

    /// <summary>
    /// Step this machine to the next level.
    /// </summary>
    // TODO: Could be problematic
    public void step() throws Exception {
        if (reg == null)
            return;

        boolean finishedLevel = false;

        if (savedListOfPartials != null) { // Maybe we popped during Run, no sense : wasting it
            stackOfPartialLists.push(new ArrayList<>(savedListOfPartials));
            savedListOfPartials = null;
            currentLevel++;
            return;
        }
        if (smartQue.size() > 0) {
            stackOfPartialLists.push(new ArrayList<>());
            currentLevel++;
        }
        while (!finishedLevel && smartQue.size() > 0) {
            SmartTrixel sq = smartQue.peek();
            if (sq.Level != previousLevel) {
                if (previousLevel < 0) {
                    currentLevel = 0;
                    previousLevel = sq.Level;
                } else {
                    finishedLevel = true;
                }
                previousLevel = sq.Level;
            }
            if (!finishedLevel) {
                smartQue.remove();
                Visit(sq);
                if (!sq.Terminal) {
                    sq.Expand();
                }
            }
        }
    }

    /// <summary>
    /// The One function
    /// </summary>
    /// <returns>1</returns>
    public int one() {
        return 1;
    }

    /// <summary>
    /// Convenience wrapper for a one step cover generation.
    /// </summary>
    /// <param name="reg"></param>
    /// <returns>list of (start, end) HtmID pairs of Outer trixels</returns>
    public static List<Long> HidList(Region reg) throws Exception {
        Cover cov = new Cover(reg);
        cov.Run();
        return cov.GetTrixels(Outer);
    }

    /// <summary>
    /// Create augmented range list (3rd column flags partial or inner)
    /// </summary>
    /// <param name="reg"></param>
    /// <returns></returns>
    public static List<AugPair<Long>> HidAugRange(Region reg) throws Exception {
        Cover cov = new Cover(reg);
        cov.Run();
        return cov.GetTriples(Outer);
    }

    /// <summary>
    /// Get range of outer HtmIDs
    /// </summary>
    /// <param name="reg"></param>
    /// <returns></returns>
    public static List<Pair<Long, Long>> HidRange(Region reg) throws Exception {
        Cover cover = new Cover(reg);
        cover.Run();
        return cover.GetPairs(Outer);
    }

    /// <summary>
    /// Compute the PseudoArea (number of level 20 trixels) of the given trixel.
    /// </summary>
    /// <param name="hid">The trixel's HtmID</param>
    /// <returns>64-bit pseudoarea</returns>
    public static Long PseudoArea(Long hid) {
        int level = Trixel.LevelOfHid(hid);
        Long result;
        int iter = (20 - level);
        //
        // 4 ^ (20 - level)
        result = 1L;
        for (iter = (20 - level); iter > 0; iter--) {
            result <<= 2;
        }
        return result;
    }
    /// <summary>
    /// Compute the PseudoArea (number of level 20 trixels) of the given region.
    /// </summary>
    /// <param name="it">Range containing HtmID (start,end) pairs</param>
    /// <returns>64-bit pseudoarea</returns>

    public static Long PseudoArea(Hidranges it) {
        Long result = 0L;
        for (Pair<Long, Long> pair : it.pairList) {
            result += (pair.getY() - pair.getX() + 1);
        }
        return result;
    }

    /// <summary>
    /// Get the covering as list of trixels the machine : the current state.
    /// </summary>
    /// <param name="kind">Inner, Outer or Partial</param>
    /// <returns></returns>
    public List<Long> GetTrixels(Markup kind) {
        switch (kind) {
            case Outer:
                return NodesToTrixels(stackOfPartialLists.peek(), listOfInners);

            case Inner:
                return NodesToTrixels(null, listOfInners);

            case Partial:
                return NodesToTrixels(stackOfPartialLists.peek(), null);

        }
        return new ArrayList<>();
    }

    /// <summary>
    /// Get the covering as a list of level 20 HtmID
    /// (start, end) pairs from the machine : the current state
    /// </summary>
    /// <param name="kind">Inner, Outer or Partial</param>
    /// <returns></returns>
    public List<Pair<Long, Long>> GetPairs(Markup kind) throws Exception {
        switch (kind) {
            case Outer:
                return NodesToPairs(stackOfPartialLists.peek(), listOfInners);

            case Inner:
                return NodesToPairs(null, listOfInners);

            case Partial:
                return NodesToPairs(stackOfPartialLists.peek(), null);

        }
        return new ArrayList<>(); // (_savePairs);
    }

    /// <summary>
    /// Get the covering as a list of level 20 HtmID
    /// (start, end, flag) triples from the current state of the machine
    ///
    /// The partial and inner trixels are separated. The flag indicates
    /// whether the (start, end) portion of the triple is from partial
    /// or inner trixels.
    /// </summary>
    /// <param name="kind">Inner, Outer or Partial</param>
    /// <returns></returns>
    public List<AugPair<Long>> GetTriples(Markup kind) throws Exception {
        switch (kind) {
            case Outer:
                return NodesToTriples(stackOfPartialLists.peek(), listOfInners);

            case Inner:
                return NodesToTriples(null, listOfInners);

            case Partial:
                return NodesToTriples(stackOfPartialLists.peek(), null);
        }
        return new ArrayList<>();
    }

    /// <summary>
    /// Make a list of augmented pairs from the given partial and inner trixels
    /// </summary>
    /// <param name="partialNodes"></param>
    /// <param name="innerNodes"></param>
    /// <returns></returns>
    private List<AugPair<Long>> NodesToTriples(List<SmartTrixel> partialNodes, List<SmartTrixel> innerNodes) throws
            Exception {
        List<AugPair<Long>> result = new ArrayList<>();
        Hidranges partials = new Hidranges(partialNodes, true);
        Hidranges inners = new Hidranges(innerNodes, true, currentLevel);
        for (Pair<Long, Long> lohi : partials.pairList) {
            result.add(new AugPair<>(lohi, false));
        }
        for (Pair<Long, Long> lohi : inners.pairList) {
            result.add(new AugPair<>(lohi, true));
        }

        return result;
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="partialNodes"></param>
    /// <param name="innerNodes"></param>
    /// <returns></returns>
    private List<Pair<Long, Long>> NodesToPairs(List<SmartTrixel> partialNodes, List<SmartTrixel> innerNodes) throws
            Exception {
        //
        // Combine the two
        // for the range
        // List<LongPair> result = new List<LongPair>();
        Hidranges partials = new Hidranges(partialNodes, true);
        Hidranges fulls = new Hidranges(innerNodes, true, currentLevel);

        // ///////////////////////////////////////
        // COMBINE PARTIAL AND FULL into allist
        //
        List<Pair<Long, Long>> allList = Hidranges.Combine(partials.pairList, fulls.pairList);
        Hidranges.Compact(allList);
        Hidranges.IsWellFormed(allList); // this will throw exception if there is a problem
        return allList;
    }

    /// <summary>
    /// Make a list of HtmIDs from the given partial and inner nodes
    /// </summary>
    /// <param name="partialNodes"></param>
    /// <param name="innerNodes"></param>
    /// <returns></returns>
    private List<Long> NodesToTrixels(List<SmartTrixel> partialNodes, List<SmartTrixel> innerNodes) {
        List<Long> result = new ArrayList<>();
        if (partialNodes != null) {
            for (SmartTrixel q : partialNodes) {
                result.add(q.Hid());
            }
        }
        if (innerNodes != null) {
            for (SmartTrixel q : innerNodes) {
                if (q.Level <= currentLevel) {
                    result.add(q.Hid());
                }
            }
        }
        return result;
    }
}
