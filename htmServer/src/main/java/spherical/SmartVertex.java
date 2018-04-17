package spherical;

import spherical.refs.Arc;
import spherical.refs.Cartesian;
import spherical.refs.Constant;
import spherical.refs.IPatch;
import spherical.refs.Region;
import spherical.refs.Topo;
import spherical.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SmartVertex {
    static final Double costol = Constant.CosTolerance;
    static final Double sintol = Constant.SinTolerance;
    Cartesian up, west;

    Cartesian v;
    Topo topo;
    /// <summary>
    /// list of arcs : outline that contain the vertex.
    /// Used : SetParentArcsAndTopo before wedge list is made.
    /// </summary>
    List<Wedge> wedgelist;
    /// <summary>
    /// Get position angle of arc going through this vertex
    /// </summary>
    /// Angle is positive (right hand rule curled) from the "up" direction
    /// <param name="a"></param>
    /// <returns>Radians</returns>
    Double GetArcPosAngle(Arc a) {
        Double gamma, xproj, yproj;

        yproj = -a.Circle().Vector().Dot(up);
        xproj = a.Circle().Vector().Dot(west);
        gamma = Math.atan2(yproj, xproj);
        return gamma;
    }
    /// <summary>
    /// Create a Wedge object for a pair of arcs coming : and out of this vertex
    /// </summary>
    /// called by SetLocalTopo : SmartTrixel
    /// <param name="arcin"></param>
    /// <param name="arcout"></param>
    /// <returns></returns>
    Wedge makeOneWedge(Arc arcin, Arc arcout){
        PositionAngle pain, paout;
        Wedge result;
        Double gamma;

        gamma = GetArcPosAngle(arcin) + Math.PI;
        pain = new PositionAngle(gamma, PositionAngle.Direction.Begin); //-1);

        gamma = GetArcPosAngle(arcout);
        paout = new PositionAngle(gamma, PositionAngle.Direction.End); // 1);

        // Make wedge
        result = new Wedge(paout.getAngle(), pain.getAngle());
        return result;
    }
    /// <summary>
    /// Return sorted list  of angles of arcs : the tangent plane as measured from up (north)
    /// : the positive direction : radians.
    /// </summary>
    /// <param name="parentArcs"></param>
    /// <returns></returns>
    private List<PositionAngle> GetParentAngles(List<Arc> parentArcs) {
        List<PositionAngle> parentAngles = new ArrayList<>(2 * parentArcs.size());
        double arcangle;
        double gamma;
        for(Arc a : parentArcs) {
            // determine incoming, outgoing or through going arc
            arcangle = a.GetAngle(this.Vertex());
            gamma = GetArcPosAngle(a);
            if(a.IsFull()) {
                //
                // Whole Sphere is a special case!
                //
                if(a.Circle().getCos0() < -1.0 + Trixel.DblTolerance) {
                    parentAngles.add(new PositionAngle(0.0, PositionAngle.Direction.End));
                    parentAngles.add(new PositionAngle(2.0 * Math.PI , PositionAngle.Direction.Begin));
                } else {

                    parentAngles.add(new PositionAngle(gamma, PositionAngle.Direction.End)); //1));
                    // then, the incoming
                    gamma += Math.PI;
                    parentAngles.add(new PositionAngle(gamma, PositionAngle.Direction.Begin)); //-1));
                }
            } else {
                // TODO: Sensitive tolerances!
                if(arcangle < Trixel.Epsilon2) {
                    // outgoing arc
                    parentAngles.add(new PositionAngle(gamma, PositionAngle.Direction.End)); //1));

                } else if(arcangle > a.Angle() - Trixel.Epsilon2) {
                    // incoming arc
                    gamma += Math.PI;
                    parentAngles.add(new PositionAngle(gamma, PositionAngle.Direction.Begin));//-1));
                } else {
                    // through is divided into : and out
                    // first, the outgoing arc
                    parentAngles.add(new PositionAngle(gamma, PositionAngle.Direction.End));//1));
                    // then, the incoming
                    gamma += Math.PI;
                    parentAngles.add(new PositionAngle(gamma, PositionAngle.Direction.Begin)); //-1));
                }
            }
        }
        parentAngles.sort(PositionAngle::CompareTo);
        return parentAngles;
    }
    /// <summary>
    ///
    /// </summary>
    /// <param name="parentArcs"></param>
    void makeWedgeList(List<Arc> parentArcs) {
        this.wedgelist = new ArrayList<>();
        // angles from up (north) : positive PositionAngle.Direction
        List<PositionAngle> parentAngles = GetParentAngles(parentArcs);
        // Make wedge list
        int count = parentAngles.size();
        if(count < 2)
            return;

        int i;
        int firstStarter = -1;
        for(i = 0; i < parentAngles.size(); i++) {
            if(parentAngles.get(i).getState() == PositionAngle.Direction.End){ //1) { // Out starting
                firstStarter = i;
                break;
            }

        }
        if(firstStarter < 0)
            return;

        // ow go from firstStarter to fistStarter - 1 MOD count.
        // Count should always be even
        i = firstStarter;
        for(int k = 0; k < count; k += 2) {
            //
            // wedge is parentAngles[i], parentAngles[i+1]
            // aka out, in
            int next = (i + 1) % count;
            wedgelist.add(new Wedge(parentAngles.get(i).getAngle(), parentAngles.get(next).getAngle()));
            i += 2;
        }
    }
    /// <summary>
    /// Sets or gets smartvertex's topo value
    /// </summary>
    public Topo Topo() {
        return topo;
    }

    /// <summary>
    /// Set this vertex Topo and keep all arcs that
    /// contain this vertex on their edges
    ///
    /// </summary>
    /// <param name="plist"></param>
    /// <returns></returns>
    public void SetParentArcsAndTopo(List<IPatch> plist, Region reg) {
        this.topo = reg.Contains(this.Vertex(), costol, sintol) ? Topo.Inner : Topo.Outer;
        //
        // If outer (not inner), there is nothing else to do
        //
        if(this.topo != Topo.Inner) {
            return;
        }
        //
        // Save all arcs on which this vertex is resting
        //
        List<Arc> parentArcs = new ArrayList<>();

        for(IPatch p : plist) {
            for(Arc a : p.getArcList()) {
                if(a.containsOnEdge(this.Vertex())) {
                    parentArcs.add(a);
                    this.topo = Topo.Same;
                }
            }
        }
        makeWedgeList(parentArcs);

    }

    /// <summary>
    /// Vertex is inside the region if it is either on the border or really inside
    /// </summary>
    public boolean IsInside() {

        return (topo == Topo.Inner || topo == Topo.Same);
    }

    public void setInside(boolean value) {
        if (value) {
            topo = Topo.Inner;
        } else {
            topo = Topo.Outer;
        }
    }

    /// <summary>
    /// Property to expose the cartesian part of this smart vertex
    /// </summary>
    public Cartesian Vertex() {
        return v;
    }

    /// <summary>
    /// Constructor may force the topo of smartvertex
    /// </summary>
    /// <param name="v"></param>
    /// <param name="inside"></param>
    public SmartVertex(Cartesian v, boolean inside) {
        this(v);
        if(inside) {
            this.topo = Topo.Inner;
        } else {
            this.topo = Topo.Outer;
        }
    }
    /// <summary>
    /// Constructor
    /// </summary>
    /// <param name="v"></param>
    public SmartVertex(Cartesian v) {
        this.v = v;
        this.topo = Topo.Intersect; // TODO: NEED an undefined enum
        Pair<Cartesian, Cartesian> tangent = this.v.Tangent();
        this.west = tangent.getX();
        this.up = tangent.getY();
    }
}
