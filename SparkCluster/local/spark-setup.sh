#!/usr/bin/env bash

#Local setup
sh /usr/local/share/spark-2.2.0-bin-hadoop2.7/sbin/start-master.sh
sh /usr/local/share/spark-2.2.0-bin-hadoop2.7/sbin/start-slave.sh <master-spark-connection-str>
#Shutdown
sh /usr/local/share/spark-2.2.0-bin-hadoop2.7/sbin/stop-slaves.sh
sh /usr/local/share/spark-2.2.0-bin-hadoop2.7/sbin/stop-master.sh

#Set performance related parameters into custom env script
cp /usr/local/share/spark-2.2.0-bin-hadoop2.7/conf/spark-env.sh.template /usr/local/share/spark-2.2.0-bin-hadoop2.7/conf/spark-env.sh

#Submit spark job
#only to trial
spark-submit --class edu.spark.example.KafkaStreamingProcess --master <master-spark-connection-str> --executor-memory 5G \
--total-executor-cores 8 /Users/sanver/Projects/master/sparkStreamQuery/target/processor-1.0-SNAPSHOT.jar \
--driver-java-options "-Dlog4j.debug=true -Dlog4j.configuration=log4j.properties" \
--conf "spark.executor.extraJavaOptions='-XX:+UseCompressedOops -Dlog4j.debug=true -Dlog4j.configuration=log4j.properties'"

#real trials
spark-submit --class edu.spark.htmLegacy.KafkaPointClassificationStreamProcess --master <master-spark-connection-str> --executor-memory 2G \
--conf "spark.executor.extraJavaOptions='-XX:+UseCompressedOops'" \
--total-executor-cores 2 /Users/sanver/Projects/master/sparkStreamQuery/target/processor-1.0-SNAPSHOT.jar \
"app=localhost:2181;4"

