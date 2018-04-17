package spherical.refs;

import spherical.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DynSymMatrix {
    //
    // Static Fields
    //
    public static final String Revision = "$Revision: 1.5 $";

    //
    // Fields
    //
    private List<List<Boolean>> data;

    //
    // Properties
    //
    public int getDim()

    {
        return this.data.size();
    }

    public void setDim(int value) {

        if (this.data.size() < value) {
            for (int i = this.data.size(); i < value; i++) {
                this.data.add(new ArrayList<>());
                for (int j = 0; j <= i; j++) {
                    this.data.get(i).add(false);
                }
            }
            return;
        }
        if (this.data.size() > value) {
            throw new IllegalArgumentException("Dim_set(): Count smaller than existing!");
        }
    }

    //
    // Indexer
    //
    public Boolean get(int i, int j) {
        if (i > j) {
            return this.data.get(i).get(j);
        }
        return this.data.get(j).get(i);
    }

    public void set(int i, int j, Boolean value) {
        if (i > j) {
            this.data.get(i).set(j, value);
        } else {
            this.data.get(j).set(i, value);
        }
    }

    //
    // Constructors
    //
    public DynSymMatrix(int dim) {
        this.data = new ArrayList<>(dim);
        for (int i = 0; i < dim; i++) {
            this.data.add(new ArrayList<>());
            for (int j = 0; j <= i; j++) {
                this.data.get(i).add(false);
            }
        }
    }

    //
    // Methods
    //
    public int CountValue(Boolean t) {
        int num = 0;
        for (int i = 0; i < this.data.size(); i++) {
            for (int j = 0; j <= i; j++) {
                Boolean t2 = this.data.get(i).get(j);
                if (t2.compareTo(t) == 0) {
                    if (j == i) {
                        num++;
                    } else {
                        num += 2;
                    }
                }
            }
        }
        return num;
    }

    public Pair<Boolean, Pair<Integer, Integer>> FindValue(Boolean t, int i, int j) {
        i = j = -1;
        for (j = 0; j < this.data.size(); j++) {
            for (i = 0; i <= j; i++) {
                Boolean t2 = this.data.get(j).get(i);
                if (t2.compareTo(t) == 0) {
                    return new Pair<>(true, new Pair<>(i, j));
                }
            }
        }
        return new Pair<>(false, new Pair<>(i, j));
    }

    //Problematic
    public Pair<Boolean, Pair<Integer, Integer>> FindValue(Boolean t, List<Integer> index) {
        int i = -1, j = -1;
        for (int k = 0; k < index.size(); k++) {
            i = index.get(k);
            for (int l = k; l < index.size(); l++) {
                j = index.get(l);
                Boolean t2 = this.data.get(i).get(j);
                if (t2.compareTo(t) == 0) {
                    return new Pair<>(true, new Pair<>(i, j));
                }
            }
        }
        return new Pair<>(false, new Pair<>(i, j));
    }

    public void removeAt(int index) {
        this.data.remove(index);
    }

    public List<Boolean> Row(int index) {
        List<Boolean> list = new ArrayList<>();
        for (int i = 0; i < this.data.size(); i++) {
            list.add(get(index, i));
        }
        return list;
    }

    @Override
    public String toString() {
        String text = "";
        for (int i = 0; i < this.data.size(); i++) {
            for (int j = 0; j < this.data.get(i).size(); j++) {
                text = text + " " + this.data.get(i).get(j);
            }
            text += "\n";
        }
        return text;
    }

    //Problematic
//    @Override
//    public int compareTo(final T o) {
//        return 0;
//    }
}