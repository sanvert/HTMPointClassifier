package spherical.refs;

import java.util.List;

/**
 * Created by sanvertarmur on 5.03.2018.
 */
public interface IPatch
{
    List<Arc> getArcList();

    Halfspace mec();

    Arc[] getArcArray();

    boolean containsOnEdge(Cartesian p);
}
