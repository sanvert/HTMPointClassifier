package edu.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyMapper {

    private static final Map<String, Map<String, String>> PROPERTY_MAP = new HashMap<>();

    public static final String APPLICATION_PROPERTIES = "application.properties";

    public PropertyMapper(String mode) {
        if(mode == "SYS") {
            readPropertyValues(APPLICATION_PROPERTIES)
                    .forEach((k, v) -> System.setProperty(k, v));
        }
    }

    public static Map<String, String> defaults() {
        return readPropertyValues(APPLICATION_PROPERTIES);
    }

    public static Map<String, String> readPropertyValues(String fileName) {
        return PROPERTY_MAP.computeIfAbsent(fileName, name -> {
            Properties properties = new Properties();

            InputStream propertyFileStream = PropertyMapper.class.getClassLoader().getResourceAsStream(fileName);

            try {
                properties.load(propertyFileStream);
            } catch (IOException e) {
                System.out.println(name + e);
            }

            return properties.stringPropertyNames().stream().collect(HashMap::new,
                    (map, prop) -> map.put(prop, properties.getProperty(prop)),
                    (acc, map) -> acc.putAll(map));
        });
    }
}
