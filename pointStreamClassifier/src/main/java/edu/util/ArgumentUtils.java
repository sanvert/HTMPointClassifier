package edu.util;

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

    public static String readFromArgumentListSilently(String[] arr, int idx, String def) {
        try {
            return arr[idx];
        } catch(ArrayIndexOutOfBoundsException e) {
        }
        return def;
    }

    public static int readIntegerArgumentSilently(String[] arr, int idx) {
        return Integer.parseInt(readFromArgumentListSilently(arr, idx, "0"));
    }

    public static long msecInPassedTime(long nsStart, long nsEnd) {
        return TimeUnit.NANOSECONDS.toMillis(nsEnd) - TimeUnit.NANOSECONDS.toMillis(nsStart);
    }
}
