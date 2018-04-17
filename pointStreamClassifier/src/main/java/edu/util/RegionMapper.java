package edu.util;

import com.google.gson.Gson;
import edu.spark.model.Region;

import java.io.InputStreamReader;
import java.util.List;

public class RegionMapper {
    private static final Gson gson = new Gson();

    public static List<Region> convert(String filePath) {
        return gson.fromJson(new InputStreamReader(RegionMapper.class.getClassLoader().getResourceAsStream(filePath)),
                List.class);
    }

    public static void main(String[] args) {
        List<Region> l = convert("regionsHTM.json");
        System.out.println(l.get(0));
    }
}
