package edu.kafka.reader;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.util.Tweet;
import spherical.util.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class TweetJsonSourceReader {
    private static final ExclusionStrategy EXCLUSION_STRATEGY = new TestExclusionStrategy();
    private static final Gson GSON = new GsonBuilder()
            .disableInnerClassSerialization()
            .disableHtmlEscaping()
            .excludeFieldsWithoutExposeAnnotation()
            .setExclusionStrategies(EXCLUSION_STRATEGY)
            .create();

    private String inputFolder;
    private Set<String> searchKeywords;
    private String extension;

    public TweetJsonSourceReader(final String inputFolder, final Set<String> searchKeywords, final String extension) {
        this.inputFolder = inputFolder;
        this.searchKeywords = searchKeywords;
        this.extension = extension;
    }

    public Map<String, Pair<String, String>> readWithRegionMapping() throws IOException {
        Map<String, Pair<String, String>> regionToQueryElement = new HashMap<>();
        Files.newDirectoryStream(Paths.get(inputFolder),
            path -> path.toString().endsWith(extension)
                    && searchKeywords.stream().anyMatch(keyword -> path.toString().toLowerCase().contains(keyword)))
            .forEach(path -> {
                String regionName = searchKeywords.stream()
                        .filter(keyword -> path.toString().toLowerCase().contains(keyword))
                        .findFirst().orElse("");
                System.out.println(regionName);
                try (Stream<String> stream = Files.lines(path, Charset.forName("UTF-8"))) {
                    stream.forEach(line -> {
                        Tweet t = GSON.fromJson(line, Tweet.class);
                        System.out.println(t);
                        String coordinatePair = t.getCoordinates().get(0) + ";" + t.getCoordinates().get(1);
                        regionToQueryElement.put(regionName, new Pair<>(t.getId(), coordinatePair));
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        return regionToQueryElement;
    }

    public Map<String, String> read() throws IOException {
        Map<String, String> idToCoordinates = new HashMap<>();
        Files.newDirectoryStream(Paths.get(inputFolder),
                path -> path.toString().endsWith(extension)
                        && searchKeywords.stream().anyMatch(keyword -> path.toString().toLowerCase().contains(keyword)))
                .forEach(path -> {
                    try (Stream<String> stream = Files.lines(path, Charset.forName("UTF-8"))) {
                        stream.forEach(line -> {
                            Tweet t = GSON.fromJson(line, Tweet.class);
                            String coordinatePair = t.getCoordinates().get(0) + ";" + t.getCoordinates().get(1);
                            idToCoordinates.put(t.getId(), coordinatePair);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        System.out.println(idToCoordinates.size());
        return idToCoordinates;
    }

    public void countLines() throws IOException {
        Files.newDirectoryStream(Paths.get(inputFolder),
                path -> path.toString().endsWith(extension)
                        && searchKeywords.stream()
                        .anyMatch(keyword -> path.toString().toLowerCase().contains(keyword)))
                .forEach(path -> countExec(path.toString()));
    }

    public void countExec(String fileName) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"wc", "-l", fileName});
            p.waitFor();
            try(final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int count(String fileName) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(fileName));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars;
            boolean endsWithoutNewLine = false;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n')
                        ++count;
                }
                endsWithoutNewLine = (c[readChars - 1] != '\n');
            }
            if(endsWithoutNewLine) {
                ++count;
            }
            return count;
        } finally {
            is.close();
        }
    }

    private static class TestExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(final FieldAttributes f) {
            return f.getName().contains("city") || f.getName().contains("text");
        }

        @Override
        public boolean shouldSkipClass(final Class<?> clazz) {
            return false;
        }
    }

    public static void main(String[] args) {
        String tempInputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data/temp";
        String processedInputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data/processed";
        String ongoingInputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data";
        Set<String> searchKeywords = new HashSet<>();
        searchKeywords.add("istanbul");
        searchKeywords.add("izmir");
        searchKeywords.add("ankara");
        searchKeywords.add("kocaeli");
        searchKeywords.add("eskisehir");
        String extension = ".json";

        try {
            TweetJsonSourceReader reader = new TweetJsonSourceReader(tempInputFolder, searchKeywords, extension);
            reader.read().entrySet().stream().limit(100).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
