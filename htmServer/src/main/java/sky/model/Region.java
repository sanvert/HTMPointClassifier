package sky.model;

import spherical.util.Pair;

import java.util.List;

public class Region {
    private String name;
    private Integer id;
    private List<List<Pair<Long, Long>>> pairs;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public List<List<Pair<Long, Long>>> getPairs() {
        return pairs;
    }

    public void setPairs(final List<List<Pair<Long, Long>>> pairs) {
        this.pairs = pairs;
    }
}
