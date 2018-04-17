package spherical;

class PositionAngle {
    public enum Direction {
        Begin,
        End,
        Undefined
    }

    private double Angle;
    private PositionAngle.Direction State;

    public Double getAngle() {
        return Angle;
    }

    public Direction getState() {
        return State;
    }

    /// <summary>
    /// Constructor. Specifies both quantities
    /// </summary>
    /// <param name="angle">in radians</param>
    /// <param name="state">Begin, End or Undefined</param>
    PositionAngle(Double angle, Direction state) {
        if (angle < 0.0) {
            this.Angle = angle + 2.0 * Math.PI;
        } else {
            this.Angle = angle;
        }
        this.State = state;
    }
    /// <summary>
    /// Comparator for sorting
    /// </summary>
    /// <param name="a"></param>
    /// <param name="b"></param>
    /// <returns></returns>
    public static int CompareTo(PositionAngle a, PositionAngle b) {
        return a.getAngle().compareTo(b.Angle);
    }
}
