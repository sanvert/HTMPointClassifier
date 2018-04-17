package spherical.util;

public class AugPair<T> {

    boolean full;
    T x;
    T y;

    public AugPair(Pair<T, T> val, boolean full) {
        this.x = val.getX();
        this.y = val.getY();
        this.full = full;
    }

    public T getX() {
        return x;
    }

    public T getY() {
        return y;
    }
}
