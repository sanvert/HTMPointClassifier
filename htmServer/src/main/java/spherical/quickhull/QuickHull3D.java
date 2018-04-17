package spherical.quickhull;

import spherical.refs.Cartesian;

import java.util.ArrayList;
import java.util.List;

public class QuickHull3D {
    //
    // Static Fields
    //
    private static final int NONCONVEX_WRT_LARGER_FACE = 1;

    private static final int NONCONVEX = 2;

    public static final int CLOCKWISE = 1;

    public static final int INDEXED_FROM_ONE = 2;

    public static final int INDEXED_FROM_ZERO = 4;

    public static final int POINT_RELATIVE = 8;

    public static final Double AUTOMATIC_TOLERANCE = -1.0;

    private static final Double DOUBLE_PREC = 2.2204460492503131E-16;
    //
    // Fields
    //
    protected Double tolerance;

    protected int numPoints;

    protected int numFaces;

    protected int numVertices;

    private VertexList claimed = new VertexList ();

    private VertexList unclaimed = new VertexList ();

    private FaceList newFaces = new FaceList();

    protected List<HalfEdge> horizon = new ArrayList<>(16);

    private Face[] discardedFaces = new Face[3];

    private Vertex[] minVtxs = new Vertex[3];

    private Vertex[] maxVtxs = new Vertex[3];

    protected int[] vertexPointIndices = new int[0];

    protected Vertex[] pointBuffer = new Vertex[0];

    protected  boolean debug;

    protected Double charLength;

    protected int findIndex = -1;

    protected List<Face> faces = new ArrayList<> (16);

    protected Double explicitTolerance = QuickHull3D.AUTOMATIC_TOLERANCE;

    //
    // Constructors
    //
    public QuickHull3D (Double[] coords) throws Exception {
        this.build(coords, coords.length / 3);
    }

    public QuickHull3D (List<Cartesian> points) throws Exception {
        this.build(points, points.size());
    }

    public QuickHull3D()
    {
    }

    //
    // Methods
    //
    private HalfEdge addAdjoiningFace (Vertex eyeVtx, HalfEdge he)
    {
        Face face = Face.createTriangle (eyeVtx, he.tail (), he.head ());
        this.faces.add(face);
        face.getEdge (-1).setOpposite (he.getOpposite ());
        return face.getEdge (0);
    }

    protected void addNewFaces (FaceList newFaces, Vertex eyeVtx, List<HalfEdge> horizon)
    {
        newFaces.clear ();
        HalfEdge halfEdge = null;
        HalfEdge halfEdge2 = null;
        for (HalfEdge current : horizon) {
            HalfEdge halfEdge3 = this.addAdjoiningFace (eyeVtx, current);
            if (halfEdge != null) {
                halfEdge3.next.setOpposite (halfEdge);
            } else {
                halfEdge2 = halfEdge3;
            }
            newFaces.add (halfEdge3.getFace ());
            halfEdge = halfEdge3;
        }
        halfEdge2.next.setOpposite (halfEdge);
    }

    private void addPointToFace (Vertex vtx, Face face)
    {
        vtx.face = face;
        if (face.outside == null) {
            this.claimed.add (vtx);
        } else {
            this.claimed.insertBefore (vtx, face.outside);
        }
        face.outside = vtx;
    }

    protected void addPointToHull (Vertex eyeVtx) throws Exception {
        this.horizon.clear();
        this.unclaimed.clear();
        this.removePointFromFace (eyeVtx, eyeVtx.face);
        this.calculateHorizon (eyeVtx.pnt, null, eyeVtx.face, this.horizon);
        this.newFaces.clear ();
        this.addNewFaces (this.newFaces, eyeVtx, this.horizon);
        for (Face face = this.newFaces.first (); face != null; face = face.next) {
            if (face.mark == Face.VISIBLE) {
                while (this.doAdjacentMerge(face, QuickHull3D.NONCONVEX_WRT_LARGER_FACE)) {
                }
            }
        }
        for (Face face2 = this.newFaces.first (); face2 != null; face2 = face2.next) {
            if (face2.mark == Face.NON_CONVEX) {
                face2.mark = Face.VISIBLE;
                while (this.doAdjacentMerge(face2, QuickHull3D.NONCONVEX)) {
                }
            }
        }
        this.resolveUnclaimedPoints (this.newFaces);
    }

    public void build (List<Double> xs, List<Double> ys, List<Double> zs) throws Exception {
        int count = xs.size();
        if (count == 3) {
            throw new CoplanarException();
        }
        if (count < 3) {
            throw new NotEnoughPoints();
        }
        this.initBuffers (count);
        this.setPoints (xs, ys, zs, count);
        this.buildHull();
    }

    public void build (List<Cartesian> points, int nump) throws Exception {
        if (nump < 4) {
            throw new Exception("Less than four input points specified");
        }
        if (points.size() < nump) {
            throw new Exception("Point array too small for specified number of points");
        }
        this.initBuffers(nump);
        this.setPoints(points, nump);
        this.buildHull();
    }

    public void build (List<Cartesian> points) throws Exception {
        this.build (points, points.size());
    }

    public void build (Double[] coords, int nump) throws Exception {
        if (nump < 4) {
            throw new Exception("Less than four input points specified");
        }
        if (coords.length / 3 < nump) {
            throw new Exception("Coordinate array too small for specified number of points");
        }
        this.initBuffers (nump);
        this.setPoints (coords, nump);
        this.buildHull ();
    }

    public void build (Double[] coords) throws Exception {
        this.build (coords, coords.length / 3);
    }

    protected void buildHull () throws Exception {
        int num = 0;
        this.computeMaxAndMin();
        this.createInitialSimplex();
        Vertex eyeVtx;
        while ((eyeVtx = this.nextPointToAdd ()) != null) {
            this.addPointToHull (eyeVtx);
            num++;
        }
        this.reindexFacesAndVertices();
    }

    protected void calculateHorizon (Cartesian eyePnt, HalfEdge edge0, Face face, List<HalfEdge> horizon)
    {
        this.deleteFacePoints (face, null);
        face.mark = Face.DELETED;
        HalfEdge halfEdge;
        if (edge0 == null) {
            edge0 = face.getEdge (0);
            halfEdge = edge0;
        } else {
            halfEdge = edge0.getNext ();
        }
        do {
            Face face2 = halfEdge.oppositeFace ();
            if (face2.mark == Face.VISIBLE) {
                if (face2.distanceToPlane (eyePnt) > this.tolerance) {
                    this.calculateHorizon (eyePnt, halfEdge.getOpposite (), face2, horizon);
                } else {
                    horizon.add(halfEdge);
                }
            }
            halfEdge = halfEdge.getNext ();
        } while (halfEdge != edge0);
    }

    protected void computeMaxAndMin ()
    {
        for (int i = 0; i < 3; i++) {
            this.maxVtxs [i] = (this.minVtxs [i] = this.pointBuffer [0]);
        }
        Cartesian cartesian = new Cartesian (this.pointBuffer [0].pnt, false);
        Cartesian cartesian2 = new Cartesian (this.pointBuffer [0].pnt, false);
        for (int j = 1; j < this.numPoints; j++) {
            Cartesian pnt = this.pointBuffer [j].pnt;
            if (pnt.getX() > cartesian.getX()) {
                cartesian.setX(pnt.getX());
                this.maxVtxs [0] = this.pointBuffer [j];
            } else if (pnt.getX() < cartesian2.getX()) {
                cartesian2.setX(pnt.getX());
                this.minVtxs [0] = this.pointBuffer [j];
            }
            if (pnt.getY() > cartesian.getY()) {
                cartesian.setY(pnt.getY());
                this.maxVtxs [1] = this.pointBuffer [j];
            } else if (pnt.getY() < cartesian2.getY()) {
                cartesian2.setY(pnt.getY());
                this.minVtxs [1] = this.pointBuffer [j];
            }
            if (pnt.getZ() > cartesian.getZ()) {
                cartesian.setZ(pnt.getZ());
                this.maxVtxs [2] = this.pointBuffer [j];
            } else if (pnt.getZ() < cartesian2.getZ()) {
                cartesian2.setZ(pnt.getZ());
                this.maxVtxs [2] = this.pointBuffer [j];
            }
        }
        this.charLength = Math.max(cartesian.getX() - cartesian2.getX(), cartesian.getY() - cartesian2.getY());
        this.charLength = Math.max(cartesian.getZ() - cartesian2.getZ(), this.charLength);
        if (this.explicitTolerance == QuickHull3D.AUTOMATIC_TOLERANCE) {
            this.tolerance = 3.0 * QuickHull3D.DOUBLE_PREC *
                    (Math.max(Math.abs (cartesian.getX()),
                            Math.abs (cartesian2.getX())) + Math.max(Math.abs (cartesian.getY()),
                            Math.abs (cartesian2.getY())) + Math.max(Math.abs (cartesian.getZ()),
                            Math.abs (cartesian2.getZ())));
            return;
        }
        this.tolerance = this.explicitTolerance;
    }

    protected void createInitialSimplex() throws Exception {
        Double num = 0.0;
        int num2 = 0;
        for (int i = 0; i < 3; i++) {
            Double num3 = this.maxVtxs[i].pnt.Get(i) - this.minVtxs[i].pnt.Get(i);
            if (num3 > num) {
                num = num3;
                num2 = i;
            }
        }
        if (num <= this.tolerance) {
            throw new Exception ("Input points appear to be coincident");
        }
        Vertex[] array = new Vertex[4];
        array [0] = this.maxVtxs [num2];
        array [1] = this.minVtxs [num2];
        Cartesian p = new Cartesian();
        Cartesian v = null;
        Double num4 = 0.0;
        Cartesian cartesian = array [1].pnt.Sub(array [0].pnt);
        cartesian.Normalize();
        for (int j = 0; j < this.numPoints; j++) {
            Cartesian p2 = this.pointBuffer [j].pnt.Sub(array [0].pnt);
            v = cartesian.Cross (p2, false);
            Double num5 = v.Norm() * v.Norm();
            if (num5 > num4 && this.pointBuffer [j] != array [0] && this.pointBuffer [j] != array [1]) {
                num4 = num5;
                array [2] = this.pointBuffer [j];
                p.Set(v, false);
            }
        }
        if (Math.sqrt (num4) <= 100.0 * this.tolerance) {
            throw new Exception("Input points appear to be colinear");
        }
        p.Normalize ();
        Double num6 = 0.0;
        Double num7 = array [2].pnt.Dot (p);
        for (int k = 0; k < this.numPoints; k++) {
            Double num8 = Math.abs (this.pointBuffer [k].pnt.Dot (p) - num7);
            if (num8 > num6 && this.pointBuffer [k] != array [0] && this.pointBuffer [k] != array [1]
                    && this.pointBuffer [k] != array [2]) {
                num6 = num8;
                array [3] = this.pointBuffer [k];
            }
        }
        if (Math.abs (num6) <= 100.0 * this.tolerance) {
            throw new QuickHull3D.CoplanarException ();
        }
        Face[] array2 = new Face[4];
        if (array [3].pnt.Dot (p) - num7 < 0.0) {
            array2 [0] = Face.createTriangle (array [0], array [1], array [2]);
            array2 [1] = Face.createTriangle (array [3], array [1], array [0]);
            array2 [2] = Face.createTriangle (array [3], array [2], array [1]);
            array2 [3] = Face.createTriangle (array [3], array [0], array [2]);
            for (int l = 0; l < 3; l++) {
                int num9 = (l + 1) % 3;
                array2 [l + 1].getEdge (1).setOpposite (array2 [num9 + 1].getEdge (0));
                array2 [l + 1].getEdge (2).setOpposite (array2 [0].getEdge (num9));
            }
        } else {
            array2 [0] = Face.createTriangle (array [0], array [2], array [1]);
            array2 [1] = Face.createTriangle (array [3], array [0], array [1]);
            array2 [2] = Face.createTriangle (array [3], array [1], array [2]);
            array2 [3] = Face.createTriangle (array [3], array [2], array [0]);
            for (int m = 0; m < 3; m++) {
                int num10 = (m + 1) % 3;
                array2 [m + 1].getEdge (0).setOpposite (array2 [num10 + 1].getEdge (1));
                array2 [m + 1].getEdge (2).setOpposite (array2 [0].getEdge ((3 - m) % 3));
            }
        }
        for (int n = 0; n < 4; n++) {
            this.faces.add(array2[n]);
        }
        for (int num11 = 0; num11 < this.numPoints; num11++) {
            Vertex vertex = this.pointBuffer [num11];
            if (vertex != array [0] && vertex != array [1] && vertex != array [2] && vertex != array [3]) {
                num6 = this.tolerance;
                Face face = null;
                for (int num12 = 0; num12 < 4; num12++) {
                    Double num13 = array2 [num12].distanceToPlane (vertex.pnt);
                    if (num13 > num6) {
                        face = array2 [num12];
                        num6 = num13;
                    }
                }
                if (face != null) {
                    this.addPointToFace (vertex, face);
                }
            }
        }
    }

    protected void deleteFacePoints (Face face, Face absorbingFace)
    {
        Vertex vertex = this.removeAllPointsFromFace (face);
        if (vertex != null) {
            if (absorbingFace == null) {
                this.unclaimed.addAll (vertex);
                return;
            }
            Vertex vertex2 = vertex;
            for (Vertex vertex3 = vertex2; vertex3 != null; vertex3 = vertex2) {
                vertex2 = vertex3.next;
                Double num = absorbingFace.distanceToPlane (vertex3.pnt);
                if (num > this.tolerance) {
                    this.addPointToFace (vertex3, absorbingFace);
                } else {
                    this.unclaimed.add (vertex3);
                }
            }
        }
    }

    private  boolean doAdjacentMerge (Face face, int mergeType) throws Exception {
        HalfEdge halfEdge = face.he0;
         boolean flag = true;
        while (true) {
            Face face2 = halfEdge.oppositeFace ();
             boolean flag2 = false;
            if (mergeType == QuickHull3D.NONCONVEX) {
                if (this.oppFaceDistance (halfEdge) > -this.tolerance || this.oppFaceDistance (halfEdge.opposite) > -this.tolerance) {
                    flag2 = true;
                }
            } else if (face.area > face2.area) {
                if (this.oppFaceDistance (halfEdge) > -this.tolerance) {
                    flag2 = true;
                } else if (this.oppFaceDistance (halfEdge.opposite) > -this.tolerance) {
                    flag = false;
                }
            } else if (this.oppFaceDistance (halfEdge.opposite) > -this.tolerance) {
                flag2 = true;
            } else if (this.oppFaceDistance (halfEdge) > -this.tolerance) {
                flag = false;
            }
            if (flag2) {
                break;
            }
            halfEdge = halfEdge.next;
            if (halfEdge == face.he0) {
                break;
            }
        }

        if (!flag) {
            face.mark = Face.NON_CONVEX;
            return false;
        } else {
            int num = face.mergeAdjacentFace(halfEdge, this.discardedFaces);
            for (int i = 0; i < num; i++) {
                this.deleteFacePoints (this.discardedFaces [i], face);
            }
            return true;
        }
    }

    private HalfEdge findHalfEdge (Vertex tail, Vertex head)
    {
        for (Face current : this.faces) {
            HalfEdge halfEdge = current.next.findEdge (tail, head);
            if (halfEdge != null) {
                return halfEdge;
            }
        }
        return null;
    }

    public  boolean getDebug ()
    {
        return this.debug;
    }

    public Double getDistanceTolerance ()
    {
        return this.tolerance;
    }

    public Double getExplicitDistanceTolerance ()
    {
        return this.explicitTolerance;
    }

    private void getFaceIndices (int[] indices, Face face, int flags)
    {
         boolean flag = (flags & QuickHull3D.CLOCKWISE) == 0;
         boolean flag2 = (flags & QuickHull3D.INDEXED_FROM_ONE) != 0;
         boolean flag3 = (flags & QuickHull3D.POINT_RELATIVE) != 0;
        HalfEdge halfEdge = face.he0;
        int num = 0;
        do {
            int num2 = halfEdge.head ().index;
            if (flag3) {
                num2 = this.vertexPointIndices [num2];
            }
            if (flag2) {
                num2++;
            }
            indices [num++] = num2;
            halfEdge = (flag ? halfEdge.next : halfEdge.prev);
        } while (halfEdge != face.he0);
    }

    public List<Face> GetFaceList()
    {
        List<Face> list = new ArrayList<> (this.faces.size());
        for (Face current : this.faces) {
            list.add(current);
        }
        return list;
    }

    public int[][] getFaces ()
    {
        return this.getFaces (0);
    }

    public int[][] getFaces (int indexFlags)
    {
        int[][] array = new int[this.faces.size()][];
        int num = 0;
        for (Face current : this.faces) {
            array [num] = new int[current.numVertices ()];
            this.getFaceIndices (array [num], current, indexFlags);
            num++;
        }
        return array;
    }

    public int getNumFaces ()
    {
        return this.faces.size();
    }

    public int getNumVertices ()
    {
        return this.numVertices;
    }

    public int[] getVertexPointIndices ()
    {
        int[] array = new int[this.numVertices];
        for (int i = 0; i < this.numVertices; i++) {
            array [i] = this.vertexPointIndices [i];
        }
        return array;
    }

    public Cartesian[] getVertices ()
    {
        Cartesian[] array = new Cartesian[this.numVertices];
        for (int i = 0; i < this.numVertices; i++) {
            array [i] = this.pointBuffer [this.vertexPointIndices [i]].pnt;
        }
        return array;
    }

    public int getVertices (Double[] coords)
    {
        for (int i = 0; i < this.numVertices; i++) {
            Cartesian pnt = this.pointBuffer [this.vertexPointIndices [i]].pnt;
            coords [i * 3] = pnt.getX();
            coords [i * 3 + 1] = pnt.getY();
            coords [i * 3 + 2] = pnt.getZ();
        }
        return this.numVertices;
    }

    protected void initBuffers(int nump) {
        if (this.pointBuffer.length < nump) {
            Vertex[] array = new Vertex[nump];
            this.vertexPointIndices = new int[nump];
            for (int i = 0; i < this.pointBuffer.length; i++) {
                array [i] = this.pointBuffer [i];
            }
            for (int j = this.pointBuffer.length; j < nump; j++) {
                array[j] = new Vertex ();
            }
            this.pointBuffer = array;
        }
        this.faces.clear();
        this.claimed.clear ();
        this.numFaces = 0;
        this.numPoints = nump;
    }

    private void markFaceVertices (Face face, int mark)
    {
        HalfEdge firstEdge = face.getFirstEdge ();
        HalfEdge halfEdge = firstEdge;
        do {
            halfEdge.head ().index = mark;
            halfEdge = halfEdge.next;
        } while (halfEdge != firstEdge);
    }

    protected Vertex nextPointToAdd ()
    {
        if (!this.claimed.isEmpty ()) {
            Face face = this.claimed.first ().face;
            Vertex result = null;
            Double num = 0.0;
            Vertex vertex = face.outside;
            while (vertex != null && vertex.face == face) {
                Double num2 = face.distanceToPlane (vertex.pnt);
                if (num2 > num) {
                    num = num2;
                    result = vertex;
                }
                vertex = vertex.next;
            }
            return result;
        }
        return null;
    }

    protected Double oppFaceDistance (HalfEdge he)
    {
        return he.face.distanceToPlane (he.opposite.face.getCentroid ());
    }

    protected void reindexFacesAndVertices () throws Exception {
        for (int i = 0; i < this.numPoints; i++) {
            this.pointBuffer [i].index = -1;
        }
        this.numFaces = 0;
        for (int j = this.faces.size() - 1; j >= 0; j--) {
            if (this.faces.get(j).mark != Face.VISIBLE) {
                this.faces.remove(j);
            }
        }
        for (Face current : this.faces) {
            if (current.mark != Face.VISIBLE) {
                throw new Exception ("reindex error");
            }
            this.markFaceVertices (current, 0);
            this.numFaces++;
        }

        this.numVertices = 0;
        for (int k = 0; k < this.numPoints; k++) {
            Vertex vertex = this.pointBuffer [k];
            if (vertex.index == 0) {
                this.vertexPointIndices [this.numVertices] = k;
                vertex.index = this.numVertices++;
            }
        }
    }

    private Vertex removeAllPointsFromFace (Face face)
    {
        if (face.outside != null) {
            Vertex vertex = face.outside;
            while (vertex.next != null && vertex.next.face == face) {
                vertex = vertex.next;
            }
            this.claimed.delete (face.outside, vertex);
            vertex.next = null;
            return face.outside;
        }
        return null;
    }

    private void removePointFromFace (Vertex vtx, Face face)
    {
        if (vtx == face.outside) {
            if (vtx.next != null && vtx.next.face == face) {
                face.outside = vtx.next;
            } else {
                face.outside = null;
            }
        }
        this.claimed.delete (vtx);
    }

    protected void resolveUnclaimedPoints (FaceList newFaces)
    {
        Vertex vertex = this.unclaimed.first ();
        for (Vertex vertex2 = vertex; vertex2 != null; vertex2 = vertex) {
            vertex = vertex2.next;
            Double num = this.tolerance;
            Face face = null;
            for (Face face2 = newFaces.first (); face2 != null; face2 = face2.next) {
                if (face2.mark == Face.VISIBLE) {
                    Double num2 = face2.distanceToPlane (vertex2.pnt);
                    if (num2 > num) {
                        num = num2;
                        face = face2;
                    }
                    if (num > 1000.0 * this.tolerance) {
                        break;
                    }
                }
            }
            if (face != null) {
                this.addPointToFace (vertex2, face);
            }
        }
    }

    public void setDebug ( boolean enable)
    {
        this.debug = enable;
    }

    public void setExplicitDistanceTolerance (Double tol)
    {
        this.explicitTolerance = tol;
    }

    protected void setHull (Double[] coords, int nump, int[][] faceIndices, int numf) throws Exception {
        this.initBuffers (nump);
        this.setPoints (coords, nump);
        this.computeMaxAndMin ();
        for (int i = 0; i < numf; i++) {
            Face face = Face.create(this.pointBuffer, faceIndices[i]);
            HalfEdge halfEdge = face.he0;
            do {
                HalfEdge halfEdge2 = this.findHalfEdge (halfEdge.head (), halfEdge.tail ());
                if (halfEdge2 != null) {
                    halfEdge.setOpposite (halfEdge2);
                }
                halfEdge = halfEdge.next;
            } while (halfEdge != face.he0);
            this.faces.add(face);
        }
    }

    protected void setPoints (Double[] coords, int nump)
    {
        for (int i = 0; i < nump; i++) {
            Vertex vertex = this.pointBuffer [i];
            vertex.pnt.Set (coords [i * 3], coords [i * 3 + 1], coords [i * 3 + 2], false);
            vertex.index = i;
        }
    }

    protected void setPoints (List<Cartesian> pnts, int nump)
    {
        for (int i = 0; i < nump; i++) {
            Vertex vertex = this.pointBuffer [i];
            vertex.pnt.Set (pnts.get(i).getX(), pnts.get(i).getY(), pnts.get(i).getZ(), false);
            vertex.index = i;
        }
    }

    protected void setPoints (List<Double> xs, List<Double> ys, List<Double> zs, int nump)
    {
        for (int i = 0; i < nump; i++) {
            Vertex vertex = this.pointBuffer [i];
            vertex.pnt.Set (xs.get(i), ys.get(i), zs.get(i), false);
            vertex.index = i;
        }
    }

    public void triangulate () throws Exception {
        Double minArea = 1000.0 * this.charLength * QuickHull3D.DOUBLE_PREC;
        this.newFaces.clear ();
        for (Face current : this.faces) {
        if (current.mark == Face.VISIBLE) {
            current.triangulate(this.newFaces, minArea);
        }
    }
        for (Face face = this.newFaces.first (); face != null; face = face.next) {
            this.faces.add (face);
        }
    }

    //
    // Nested Types
    //
    public class ColinearException extends Exception
    {
    }

    public class CoplanarException extends Exception
    {
    }

    public class NotEnoughPoints extends Exception
    {
    }
}
