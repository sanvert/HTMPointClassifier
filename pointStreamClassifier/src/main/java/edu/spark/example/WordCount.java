package main.java.edu.spark.example;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.SparkSession;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import scala.Tuple2;

public class WordCount {
    public static void main(String[] args) {
        SparkSession spark = SparkSession
                .builder()
                .appName("Word count test")
                .getOrCreate();

        try (JavaSparkContext sc = new JavaSparkContext(spark.sparkContext())) {
            // load data
            JavaRDD<String> textFile = sc.textFile("file:///Users/sanver/Downloads/big.txt");
            /*
             * User is able to handle different operations on the obtained DStream,
             * first we split the data, then use Map and ReduceByKey to calculation.
             */
            JavaRDD<String> words = textFile.flatMap(new FlatMapFunction<String, String>() {
                @Override
                public Iterator<String> call(String s) {
                    return Arrays.asList(Pattern.compile(" ").split(s)).iterator();
                }
            });

            JavaPairRDD<String, Integer> ones = words.mapToPair(new PairFunction<String, String, Integer>() {
                @Override
                public Tuple2<String, Integer> call(String s) {
                    return new Tuple2<>(s, 1);
                }
            });

            JavaPairRDD<String, Integer> counts = ones.reduceByKey(new Function2<Integer, Integer, Integer>() {
                @Override
                public Integer call(Integer i1, Integer i2) {
                    return i1 + i2;
                }
            });

            List<Tuple2<String, Integer>> output = counts.collect();
            for (Tuple2<?, ?> tuple : output) {
                System.out.println(tuple._1() + ": " + tuple._2());
            }

            sc.stop();
        }
    }
}