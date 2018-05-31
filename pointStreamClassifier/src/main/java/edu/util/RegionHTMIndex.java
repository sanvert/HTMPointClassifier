package edu.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import skiplist.IntervalSkipList;
import skiplist.region.RegionAwareIntervalSkipList;
import skiplist.region.RegionAwareSkipList;
import sky.model.Region;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RegionHTMIndex implements Serializable {

    private static final Gson GSON = new Gson();

    public final Map<String, Integer> cityMap = new ConcurrentHashMap<>();

    private final String regionFile;
    private final RegionAwareSkipList regionAwareSkipList;

    public RegionHTMIndex(final String regionFile) {
        this.regionFile = regionFile;
        this.regionAwareSkipList = generateIntervalSkipList();
    }

    private List<Region> convert() {
        Type typeOfT = new TypeToken<List<Region>>(){}.getType();
        return GSON.fromJson(new InputStreamReader(RegionHTMIndex.class.getClassLoader()
                        .getResourceAsStream(regionFile)), typeOfT);
    }

    private RegionAwareSkipList generateIntervalSkipList() {
        final RegionAwareSkipList regionAwareIntervalSkipList = new RegionAwareIntervalSkipList();
        convert().stream()
                .forEach(region -> {
                        cityMap.put(region.getName().toLowerCase(), region.getId());
                        region.getPairs().stream()
                                .forEach(pairs -> pairs.forEach(pair -> regionAwareIntervalSkipList
                                        .addInterval(region.getId(), pair.getX(), pair.getY())));
                });
        return regionAwareIntervalSkipList;
    }

    private List<IntervalSkipList> convertIntoSkipLists() {
        List<Region> regionList = convert();
        return regionList.stream().map(region -> {
            final IntervalSkipList skipList = new IntervalSkipList(String.valueOf(region.getId()));
            region.getPairs().stream()
                    .forEach(polygonPairs -> polygonPairs
                                            .forEach(pair -> skipList.addInterval(pair.getX(), pair.getY())));
            return skipList;
        }).collect(Collectors.toList());
    }

    private List<Long> extractNumberOfHTMs(List<Region> regionList) {
        return regionList.stream()
                .map(region -> region.getPairs().stream()
                        .map(polygonPair -> polygonPair.stream()
                                .map(pair -> pair.getX() - pair.getY())
                                .reduce(0L, Long::sum))
                        .reduce(0L, Long::sum))
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getCityMap() {
        return cityMap;
    }

    public Set<Integer> getResidingRegionIdSet(long htmId) {
        return regionAwareSkipList.regionIdSet(htmId);
    }

    //Test
    public static void main(String[] args) {
        String regionFile = "regionsHTM.json";
        RegionHTMIndex regionHTMIndex = new RegionHTMIndex(regionFile);
        List<Region> regions = regionHTMIndex.convert();
        System.out.println(regions.get(0).getPairs().size());
        System.out.println(regionHTMIndex.extractNumberOfHTMs(regions));
        System.out.println(regions.get(0).toString());
        List<IntervalSkipList> regionsSL = regionHTMIndex.convertIntoSkipLists();
        System.out.println(regionsSL.get(0));

        RegionAwareSkipList regionAwareSkipList = regionHTMIndex.generateIntervalSkipList();
        System.out.println(regionAwareSkipList);

        regionHTMIndex.getCityMap().entrySet().stream()
                .forEach(entry -> System.out.println(entry.getKey() + " " + entry.getValue()));
    }
}
