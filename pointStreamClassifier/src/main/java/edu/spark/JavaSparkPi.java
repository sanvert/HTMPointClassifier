package edu.spark;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.util.ArrayList;
import java.util.List;

public final class JavaSparkPi {

    public static void main(String[] args) {
        SparkSession spark = SparkSession
                .builder()
                .appName("JavaSparkPi")
                .getOrCreate();

        try(JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext())) {
            int slices = (args.length == 1) ? Integer.parseInt(args[0]) : 2;
            int n = 100000 * slices;
            List<Integer> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                l.add(i);
            }

            System.out.println("Spark Pi calculation");

            JavaRDD<Integer> dataSet = jsc.parallelize(l, slices);

            int count = dataSet.map(integer -> {
                double x = Math.random() * 2 - 1;
                double y = Math.random() * 2 - 1;
                int result = (x * x + y * y <= 1) ? 1 : 0;
                System.out.println(result);
                return result;
            }).reduce((integer, integer2) -> integer + integer2);

            System.out.println("Pi is roughly " + 4.0 * count / n);

            spark.stop();
        }
    }
}