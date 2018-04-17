package spherical.quickhull;

public class HalfEdge
{
    //
    // Fields
    //
    public Vertex vertex;

    public Face face;

    public HalfEdge next;

    public HalfEdge prev;

    public HalfEdge opposite;

    //
    // Constructors
    //
    public HalfEdge (Vertex v, Face f)
    {
        this.vertex = v;
        this.face = f;
    }

    public HalfEdge ()
    {
    }

    //
    // Methods
    //
    public Face getFace ()
    {
        return this.face;
    }

    public HalfEdge getNext ()
    {
        return this.next;
    }

    public HalfEdge getOpposite ()
    {
        return this.opposite;
    }

    public HalfEdge getPrev ()
    {
        return this.prev;
    }

    public String getVertexString ()
    {
        if (this.tail () != null) {
            return
                    "" +
                    this.tail ().index +
                    "-" +
                    this.head ().index;
        }
        return "?-" + this.head ().index;
    }

    public Vertex head ()
    {
        return this.vertex;
    }

    public double length ()
    {
        if (this.tail () != null) {
            return this.head ().pnt.Distance (this.tail ().pnt);
        }
        return -1.0;
    }

    public double lengthSquared ()
    {
        if (this.tail () != null) {
            double num = this.head ().pnt.Distance (this.tail ().pnt);
            return num * num;
        }
        return -1.0;
    }

    public Face oppositeFace ()
    {
        if (this.opposite == null) {
            return null;
        }
        return this.opposite.face;
    }

    public void setNext (HalfEdge edge)
    {
        this.next = edge;
    }

    public void setOpposite (HalfEdge edge)
    {
        this.opposite = edge;
        edge.opposite = this;
    }

    public void setPrev (HalfEdge edge)
    {
        this.prev = edge;
    }

    public Vertex tail ()
    {
        if (this.prev == null) {
            return null;
        }
        return this.prev.vertex;
    }
}
