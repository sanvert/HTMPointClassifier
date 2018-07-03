package edu.util;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ArgumentUtils {

    public static String[] readArgumentsSilently(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.contains("app"))
                .map(arg -> arg.replaceFirst("app=", "").split(";"))
                .findFirst()
                .orElse(new String[]{});
    }

    public static String readArgumentOverriding(String[] arr, int idx, String name) {
        try {
            PropertyMapper.readDefaultProps().putIfAbsent(name, arr[idx]);
            return arr[idx];
        } catch(ArrayIndexOutOfBoundsException e) {
        }
        return StringUtils.EMPTY;
    }

    public static String readArgumentSilently(String[] arr, int idx, String def) {
        try {
            return arr[idx];
        } catch(ArrayIndexOutOfBoundsException e) {
        }
        return def;
    }

    public static int readIntegerArgumentSilently(String[] arr, int idx) {
        return Integer.parseInt(readArgumentSilently(arr, idx, "0"));
    }
}
