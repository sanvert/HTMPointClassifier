package spherical;

/// <summary>
/// Markup is a classification of a trixel with respect to the way it intersects a
/// region.
/// </summary>
public enum Markup {
    /// <summary>trixel is completely inside</summary>
    Inner,
    /// <summary>trixel non-trivially intersects</summary>
    Partial,
    /// <summary>trixel is completely outside</summary>
    Reject,
    /// <summary>trixel's status is not known</summary>
    Undefined,
    /// <summary>
    /// used for requesting trixels that are either Inner or Partial
    /// </summary>
    Outer
};
