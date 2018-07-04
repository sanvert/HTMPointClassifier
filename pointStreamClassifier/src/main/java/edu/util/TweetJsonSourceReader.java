package edu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TweetJsonSourceReader {
    private static final Gson GSON = new GsonBuilder()
            .disableInnerClassSerialization()
            .disableHtmlEscaping()
            .excludeFieldsWithoutExposeAnnotation().create();

    private String inputFolder;
    private Set<String> searchKeywords;
    private String extension;

    public TweetJsonSourceReader(final String inputFolder, final Set<String> searchKeywords, final String extension) {
        this.inputFolder = inputFolder;
        this.searchKeywords = searchKeywords;
        this.extension = extension;
    }

    public void read() throws IOException {
        Files.newDirectoryStream(Paths.get(inputFolder),
                path -> path.toString().endsWith(extension)
                        && searchKeywords.stream()
                            .anyMatch(keyword -> path.toString().toLowerCase().contains(keyword)))
                .forEach(path -> {
                    try (Stream<String> stream = Files.lines(path, Charset.forName("UTF-8"))) {
                        stream.forEach(line -> System.out.println(GSON.fromJson(line, Tweet.class)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
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

    public static void main(String[] args) {
        String inputFolder = "/home/tarmur/Projects/m/HTMPointClassifier/TweeterGeoStream/data/temp";
        Set<String> searchKeywords = new HashSet<>();
        searchKeywords.add("istanbul");
        searchKeywords.add("izmir");
        searchKeywords.add("ankara");
        String extension = ".json";

        try {
            TweetJsonSourceReader reader = new TweetJsonSourceReader(inputFolder, searchKeywords, extension);
            reader.countLines();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
