package spherical.quickhull;

import spherical.refs.Cartesian;

public class Face
{
    //
    // Static Fields
    //
    public static final int VISIBLE = 1;

    public static final int NON_CONVEX = 2;

    public static final int DELETED = 3;

    //
    // Fields
    //
    public HalfEdge he0;

    private Cartesian normal;

    public double area;

    private Cartesian centroid;

    private double planeOffset;

    private int numVerts;

    public Face next;

    public int mark = Face.VISIBLE;

    public Vertex outside;

    //
    // Constructors
    //
    public Face ()
    {
        this.normal = new Cartesian();
        this.centroid = new Cartesian();
        this.mark = Face.VISIBLE;
    }

    //
    // Static Methods
    //
    public static Face create (Vertex[] vtxArray, int[] indices) throws Exception {
        Face face = new Face();
        HalfEdge halfEdge = null;
        for (int i = 0; i < indices.length; i++) {
            HalfEdge halfEdge2 = new HalfEdge (vtxArray [indices [i]], face);
            if (halfEdge != null) {
                halfEdge2.setPrev (halfEdge);
                halfEdge.setNext (halfEdge2);
            } else {
                face.he0 = halfEdge2;
            }
            halfEdge = halfEdge2;
        }
        face.he0.setPrev (halfEdge);
        halfEdge.setNext (face.he0);
        face.computeNormalAndCentroid ();
        return face;
    }

    public static Face createTriangle (Vertex v0, Vertex v1, Vertex v2, double minArea)
    {
        Face face = new Face();
        HalfEdge halfEdge = new HalfEdge (v0, face);
        HalfEdge halfEdge2 = new HalfEdge (v1, face);
        HalfEdge halfEdge3 = new HalfEdge (v2, face);
        halfEdge.prev = halfEdge3;
        halfEdge.next = halfEdge2;
        halfEdge2.prev = halfEdge;
        halfEdge2.next = halfEdge3;
        halfEdge3.prev = halfEdge2;
        halfEdge3.next = halfEdge;
        face.he0 = halfEdge;
        face.computeNormalAndCentroid (minArea);
        return face;
    }

    public static Face createTriangle (Vertex v0, Vertex v1, Vertex v2)
    {
        return Face.createTriangle (v0, v1, v2, 0.0);
    }

    //
    // Methods
    //
    private double areaSquared (HalfEdge hedge0, HalfEdge hedge1)
    {
        Cartesian pnt = hedge0.tail ().pnt;
        Cartesian pnt2 = hedge0.head ().pnt;
        Cartesian pnt3 = hedge1.head ().pnt;
        double num = pnt2.getX() - pnt.getX();
        double num2 = pnt2.getY() - pnt.getY();
        double num3 = pnt2.getZ() - pnt.getZ();
        double num4 = pnt3.getX() - pnt.getX();
        double num5 = pnt3.getY() - pnt.getY();
        double num6 = pnt3.getZ() - pnt.getZ();
        double num7 = num2 * num6 - num3 * num5;
        double num8 = num3 * num4 - num * num6;
        double num9 = num * num5 - num2 * num4;
        return num7 * num7 + num8 * num8 + num9 * num9;
    }

    private void checkConsistency () throws Exception {
        HalfEdge halfEdge = this.he0;
        double num = 0.0;
        int num2 = 0;
        if (this.numVerts < 3) {
            throw new Exception ("degenerate face: " + this.getVertexString ());
        }
        HalfEdge opposite;
        Face face;
        while (true) {
            opposite = halfEdge.getOpposite ();
            if (opposite == null) {
                break;
            }
            if (opposite.getOpposite () != halfEdge) {
                throw new Exception (
                        "face " +
                                this.getVertexString () +
                                ": opposite half edge " +
                                opposite.getVertexString () +
                                " has opposite " +
                                opposite.getOpposite ().getVertexString ());
            }
            if (opposite.head () != halfEdge.tail () || halfEdge.head () != opposite.tail ()) {
                throw new Exception (
                        "face " +
                                this.getVertexString () +
                                ": half edge " +
                                halfEdge.getVertexString () +
                                " reflected by " +
                                opposite.getVertexString ());
            }
            face = opposite.face;
            if (face == null) {
                throw new Exception ("face " + this.getVertexString () +
                        ": no face on half edge " + opposite.getVertexString ());
            }
            if (face.mark == Face.DELETED) {
                throw new Exception (
                        "face " +
                                this.getVertexString () +
                                ": opposite face " +
                                face.getVertexString () +
                                " not on hull");
            }
            double num3 = Math.abs (this.distanceToPlane (halfEdge.head().pnt));
            if (num3 > num) {
                num = num3;
            }
            num2++;
            halfEdge = halfEdge.next;
            if (halfEdge == this.he0) {
                if (num2 != this.numVerts) {
                    throw new Exception (
                            "face " +
                                    this.getVertexString () +
                                    " numVerts=" +
                                    this.numVerts +
                                    " should be " +
                                    num2);
                }
            }
        }
        throw new Exception ("face " + this.getVertexString () + 
                ": unreflected half edge " + halfEdge.getVertexString ());
    }

    public void computeCentroid ()
    {
        this.centroid.Set (0.0, 0.0, 0.0, false);
        HalfEdge halfEdge = this.he0;
        do {
            this.centroid = this.centroid.Add (halfEdge.head ().pnt);
            halfEdge = halfEdge.next;
        } while (halfEdge != this.he0);
        this.centroid.Scale (1.0 / (double)this.numVerts);
    }

    public void computeNormal ()
    {
        HalfEdge halfEdge = this.he0.next;
        HalfEdge halfEdge2 = halfEdge.next;
        Cartesian pnt = this.he0.head ().pnt;
        Cartesian pnt2 = halfEdge.head ().pnt;
        double num = pnt2.getX() - pnt.getX();
        double num2 = pnt2.getY() - pnt.getY();
        double num3 = pnt2.getZ() - pnt.getZ();
        this.normal.Set (0.0, 0.0, 0.0, false);
        this.numVerts = 2;
        while (halfEdge2 != this.he0) {
            double num4 = num;
            double num5 = num2;
            double num6 = num3;
            pnt2 = halfEdge2.head ().pnt;
            num = pnt2.getX() - pnt.getX();
            num2 = pnt2.getY() - pnt.getY();
            num3 = pnt2.getZ() - pnt.getZ();
            this.normal.setX(this.normal.getX() + (num5 * num3 - num6 * num2));
            this.normal.setY(this.normal.getY() + (num6 * num - num4 * num3));
            this.normal.setZ(this.normal.getZ() + (num4 * num2 - num5 * num));
            halfEdge2 = halfEdge2.next;
            this.numVerts++;
        }
        this.area = this.normal.Norm();
        this.normal.Scale (1.0 / this.area);
    }

    private void computeNormal (double minArea)
    {
        this.computeNormal ();
        if (this.area < minArea) {
            HalfEdge halfEdge = null;
            double num = 0.0;
            HalfEdge halfEdge2 = this.he0;
            do {
                double num2 = halfEdge2.lengthSquared ();
                if (num2 > num) {
                    halfEdge = halfEdge2;
                    num = num2;
                }
                halfEdge2 = halfEdge2.next;
            } while (halfEdge2 != this.he0);
            Cartesian pnt = halfEdge.head ().pnt;
            Cartesian pnt2 = halfEdge.tail ().pnt;
            double num3 = Math.sqrt (num);
            double num4 = (pnt.getX() - pnt2.getX()) / num3;
            double num5 = (pnt.getY() - pnt2.getY()) / num3;
            double num6 = (pnt.getZ() - pnt2.getZ()) / num3;
            double num7 = this.normal.getX() * num4 + this.normal.getY() * num5 + this.normal.getZ() * num6;
            this.normal.setX(this.normal.getX() - num7 * num4);
            this.normal.setY(this.normal.getY() - num7 * num5);
            this.normal.setZ(this.normal.getZ() - num7 * num6);
            this.normal.Normalize ();
        }
    }

    private void computeNormalAndCentroid (double minArea)
    {
        this.computeNormal (minArea);
        this.computeCentroid ();
        this.planeOffset = this.normal.Dot (this.centroid);
    }

    private void computeNormalAndCentroid () throws Exception {
        this.computeNormal ();
        this.computeCentroid ();
        this.planeOffset = this.normal.Dot (this.centroid);
        int num = 0;
        HalfEdge halfEdge = this.he0;
        do {
            num++;
            halfEdge = halfEdge.next;
        } while (halfEdge != this.he0);
        if (num != this.numVerts) {
            throw new Exception (
                    "face " +
                    this.getVertexString () +
                    " numVerts=" +
                    this.numVerts +
                    " should be " +
                    num);
        }
    }

    private Face connectHalfEdges (HalfEdge hedgePrev, HalfEdge hedge) throws Exception {
        Face result = null;
        if (hedgePrev.oppositeFace () == hedge.oppositeFace ()) {
            Face face = hedge.oppositeFace ();
            if (hedgePrev == this.he0) {
                this.he0 = hedge;
            }
            HalfEdge opposite;
            if (face.numVertices () == 3) {
                opposite = hedge.getOpposite ().prev.getOpposite ();
                face.mark = Face.DELETED;
                result = face;
            } else {
                opposite = hedge.getOpposite ().next;
                if (face.he0 == opposite.prev) {
                    face.he0 = opposite;
                }
                opposite.prev = opposite.prev.prev;
                opposite.prev.next = opposite;
            }
            hedge.prev = hedgePrev.prev;
            hedge.prev.next = hedge;
            hedge.opposite = opposite;
            opposite.opposite = hedge;
            face.computeNormalAndCentroid ();
        } else {
            hedgePrev.next = hedge;
            hedge.prev = hedgePrev;
        }
        return result;
    }

    public double distanceToPlane (Cartesian p)
    {
        return this.normal.getX() * p.getX() + this.normal.getY() * p.getY()
                + this.normal.getZ() * p.getZ() - this.planeOffset;
    }

    public HalfEdge findEdge (Vertex vt, Vertex vh)
    {
        HalfEdge halfEdge = this.he0;
        while (halfEdge.head () != vh || halfEdge.tail () != vt) {
            halfEdge = halfEdge.next;
            if (halfEdge == this.he0) {
                return null;
            }
        }
        return halfEdge;
    }

    public Cartesian getCentroid ()
    {
        return this.centroid;
    }

    public HalfEdge getEdge (int i)
    {
        HalfEdge prev = this.he0;
        while (i > 0) {
            prev = prev.next;
            i--;
        }
        while (i < 0) {
            prev = prev.prev;
            i++;
        }
        return prev;
    }

    public HalfEdge getFirstEdge ()
    {
        return this.he0;
    }

    public Cartesian getNormal ()
    {
        return this.normal;
    }

    public void getVertexIndices (int[] idxs)
    {
        HalfEdge halfEdge = this.he0;
        int num = 0;
        do {
            idxs [num++] = halfEdge.head ().index;
            halfEdge = halfEdge.next;
        } while (halfEdge != this.he0);
    }

    public String getVertexString ()
    {
        String text = null;
        HalfEdge halfEdge = this.he0;
        do {
            if (text == null) {
                text = "" + halfEdge.head ().index;
            } else {
                text = text + " " + halfEdge.head ().index;
            }
            halfEdge = halfEdge.next;
        } while (halfEdge != this.he0);
        return text;
    }

    public int mergeAdjacentFace (HalfEdge hedgeAdj, Face[] discarded) throws Exception {
        Face face = hedgeAdj.oppositeFace ();
        int result = 0;
        discarded [result++] = face;
        face.mark = Face.DELETED;
        HalfEdge opposite = hedgeAdj.getOpposite ();
        HalfEdge prev = hedgeAdj.prev;
        HalfEdge halfEdge = hedgeAdj.next;
        HalfEdge prev2 = opposite.prev;
        HalfEdge halfEdge2 = opposite.next;
        while (prev.oppositeFace () == face) {
            prev = prev.prev;
            halfEdge2 = halfEdge2.next;
        }
        while (halfEdge.oppositeFace () == face) {
            prev2 = prev2.prev;
            halfEdge = halfEdge.next;
        }
        for (HalfEdge halfEdge3 = halfEdge2; halfEdge3 != prev2.next; halfEdge3 = halfEdge3.next) {
            halfEdge3.face = this;
        }
        if (hedgeAdj == this.he0) {
            this.he0 = halfEdge;
        }
        Face face2 = this.connectHalfEdges (prev2, halfEdge);
        if (face2 != null) {
            discarded [result++] = face2;
        }
        face2 = this.connectHalfEdges (prev, halfEdge2);
        if (face2 != null) {
            discarded [result++] = face2;
        }
        this.computeNormalAndCentroid ();
        this.checkConsistency ();
        return result;
    }

    public int numVertices ()
    {
        return this.numVerts;
    }

    public void triangulate (FaceList newFaces, double minArea) throws Exception {
        if (this.numVertices () < 4) {
            return;
        }
        Vertex v = this.he0.head ();
        HalfEdge halfEdge = this.he0.next;
        HalfEdge opposite = halfEdge.opposite;
        Face face = null;
        for (halfEdge = halfEdge.next; halfEdge != this.he0.prev; halfEdge = halfEdge.next) {
            Face face2 = Face.createTriangle (v, halfEdge.prev.head (), halfEdge.head (), minArea);
            face2.he0.next.setOpposite (opposite);
            face2.he0.prev.setOpposite (halfEdge.opposite);
            opposite = face2.he0;
            newFaces.add (face2);
            if (face == null) {
                face = face2;
            }
        }
        halfEdge = new HalfEdge (this.he0.prev.prev.head (), this);
        halfEdge.setOpposite (opposite);
        halfEdge.prev = this.he0;
        halfEdge.prev.next = halfEdge;
        halfEdge.next = this.he0.prev;
        halfEdge.next.prev = halfEdge;
        this.computeNormalAndCentroid (minArea);
        this.checkConsistency ();
        for (Face face3 = face; face3 != null; face3 = face3.next) {
            face3.checkConsistency ();
        }
    }
}
