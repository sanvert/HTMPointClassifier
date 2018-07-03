package edu.util;

import java.util.concurrent.TimeUnit;

public class GenericUtils {
    public static long passedTimeInMsec(long nsStart, long nsEnd) {
        return TimeUnit.NANOSECONDS.toMillis(nsEnd) - TimeUnit.NANOSECONDS.toMillis(nsStart);
    }
}
