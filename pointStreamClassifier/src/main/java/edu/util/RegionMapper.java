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
            final IntervalSkipList skipList = new IntervalSkipList(String.valueOf(region.getId()));
            region.getPairs().stream()
                    .forEach(polygonPairs -> polygonPairs
                                            .forEach(pair -> skipList.addInterval(pair.getX(), pair.getY())));
            return skipList;
        }).collect(Collectors.toList());
    }

    private static List<Long> extractNumberOfHTMs(List<Region> regionList) {
        return regionList.stream()
                .map(region -> region.getPairs().stream()
                        .map(polygonPair -> polygonPair.stream()
                                .map(pair -> pair.getX() - pair.getY())
                                .reduce(0L, Long::sum))
                        .reduce(0L, Long::sum))
                .collect(Collectors.toList());
    }

    //Test
    public static void main(String[] args) {
        List<Region> regions = convert("regionsHTM.json");
        System.out.println(regions.get(0).getPairs().size());
        System.out.println(extractNumberOfHTMs(regions));
        System.out.println(regions.get(0).toString());
        List<IntervalSkipList> regionsSL = convertIntoSkipLists("regionsHTM.json");
        System.out.println(regionsSL.get(0));
    }
}
