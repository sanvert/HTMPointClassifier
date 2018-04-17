package spherical;

import spherical.refs.Arc;
import spherical.refs.Topo;

public class SortableRoot {
    double Lower;
    double Upper;
    /// <summary>
    /// 
    /// </summary>
    public Arc ParentArc;
    public Topo topo;
    /// <summary>
    /// Constructor 
    /// </summary>
    /// <param name="low"></param>
    /// <param name="high"></param>
    /// <param name="arc"></param>
    /// <param name="top"></param>
    SortableRoot(Double low, Double high, Arc arc, Topo top) {
        this.Lower = low;
        this.Upper = high;
        this.ParentArc = arc;
        this.topo = top;
        // this.incoming = false; // COMPILER AUTOMATICALLY MAKES IT FALSE  CA1805 
    }
    /// <summary>
    /// Constructor
    /// </summary>
    /// <param name="angle"></param>
    /// <param name="arc"></param>
    public SortableRoot(Double angle, Arc arc) {
        this(angle, angle, arc, Topo.Intersect);
    }
    /// <summary>
    /// True if Sortable Root is or conatins 0
    /// </summary>
    /// <returns></returns>
    public boolean isZero() {
        return (Lower <= Trixel.DblTolerance);
    }
    /// <summary>
    /// True if Sortable Root is or conatins 1
    /// </summary>
    /// <returns></returns>
    public boolean isOne() {
        return (Upper >= 1.0 - Trixel.DblTolerance);
    }

    public Double getLower() {
        return Lower;
    }

    public Double getUpper() {
        return Upper;
    }

    /// <summary>
    /// Display as text
    /// </summary>
    /// <returns>string</returns>
    @Override
    public String toString() {
        char letter;
        char inc;

        if (this.topo == Topo.Intersect) {
            letter = 'X';
        } else if (topo == Topo.Same) {
            letter = 'S';
        } else if (topo == Topo.Inverse) {
            letter = 'I';
        } else {
            letter = '?';
        }
        inc = ' ';
        if (this.Upper - this.Lower < Trixel.DblTolerance) {
            return String.format("{0}{2}:({1})", letter, this.Lower, inc);
        }

        return String.format("{0}{3}:(_{1}  _{2})", letter, this.Lower, this.Upper, inc);
    }

}
