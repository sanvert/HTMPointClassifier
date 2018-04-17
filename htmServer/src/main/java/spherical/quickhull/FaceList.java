package spherical.quickhull;

public class FaceList
{
    //
    // Fields
    //
    private Face head;

    private Face tail;

    //
    // Methods
    //
    public void add (Face vtx)
    {
        if (this.head == null) {
            this.head = vtx;
        } else {
            this.tail.next = vtx;
        }
        vtx.next = null;
        this.tail = vtx;
    }

    public void clear ()
    {
        this.head = (this.tail = null);
    }

    public Face first ()
    {
        return this.head;
    }

    public boolean isEmpty ()
    {
        return this.head == null;
    }
}
