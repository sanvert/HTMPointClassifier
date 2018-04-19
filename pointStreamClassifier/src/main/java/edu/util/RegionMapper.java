package edu.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import skiplist.IntervalSkipList;
import sky.model.Region;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class RegionMapper {
    private static final Gson gson = new Gson();

    public static List<Region> convert(String fileName) {
        Type typeOfT = new TypeToken<List<Region>>(){}.getType();
        return gson.fromJson(new InputStreamReader(RegionMapper.class.getClassLoader().getResourceAsStream(fileName)),
                typeOfT);
    }

    public static List<IntervalSkipList> convertIntoSkipLists(String fileName) {
        List<Region> regionList = convert(fileName);
        return regionList.stream().map(region -> {
            final IntervalSkipList skipList = new IntervalSkipList();
            region.getPairs().stream().forEach(pair -> skipList.addInterval(pair.getX(), pair.getY()));
            return skipList;
        }).collect(Collectors.toList());
    }

    //Test
    public static void main(String[] args) {
        List<Region> l = convert("regionsHTM.json");
        System.out.println(l.get(0).toString());
        List<IntervalSkipList> sl = convertIntoSkipLists("regionsHTM.json");
        System.out.println(sl.get(0));
    }
}
