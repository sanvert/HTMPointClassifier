package spherical.quickhull;

import spherical.refs.Cartesian;

public class Vertex
{
    //
    // Fields
    //
    public Cartesian pnt;

    public int index;

    public Vertex prev;

    public Vertex next;

    public Face face;

    //
    // Constructors
    //
    public Vertex () {
        this.pnt = new Cartesian();
    }

    public Vertex (double x, double y, double z, int idx)
    {
        this.pnt = new Cartesian(x, y, z, false);
        this.index = idx;
    }
}
