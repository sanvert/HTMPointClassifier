package spherical.refs;

import java.util.ArrayList;
import java.util.List;

public class PatchPart implements IPatch {

    private List<Arc> arcList = new ArrayList<>();

    private Halfspace mec;

    private Double length;

    @Override
    public List<Arc> getArcList() {
        return this.arcList;
    }

    public Double Length() {
        return this.length;
    }
    
    @Override
    public Halfspace mec() {
        return this.mec;
    }

    @Override
    public Arc[] getArcArray() {
        return (Arc[]) this.arcList.toArray();
    }

    public void setMec(Halfspace mec) {
        this.mec = mec;
    }

    @Override
    public boolean containsOnEdge(Cartesian p) {
        for(Arc current : this.arcList){
            if (current.containsOnEdge(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String str = String.format("# {0}\n", this.arcList.size());
        String[] array = new String[this.arcList.size()];
        for (int i = 0; i < this.arcList.size(); i++) {
            array[i] = this.arcList.get(i).toString();
        }
        return str + String.join("\n", array);
    }
}