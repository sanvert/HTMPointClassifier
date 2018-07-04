Performance Suggestions
- Add -XX:+UseCompressedOops to the spark-env.sh
- Submit master task command
sudo /opt/spark/spark-2.3.0-bin-hadoop2.7/bin/spark-submit --class edu.spark.htm.redirect.KafkaUnionPCMultiKeyedStream --master spark://nl1lxl-108916.ttg.global:7077 --num-executors 3 --executor-memory 2512m --total-executor-cores 6 --name "point-stream" ~/Projects/m/HTMPointClassifier/pointStreamClassifier/target/point.stream.classifier-1.0.jar app=localhost:2181;spark://nl1lxl-108916.ttg.global:7077

-Producer topic
sudo /opt/kafka/kafka_2.11-1.1.0/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 9 --topic p-1
-Consumer topic
sudo /opt/kafka_2.11-1.1.0/bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic c-1 
-Alter retention time of topic

sudo /opt/kafka_2.11-1.1.0/bin/kafka-configs.sh \
  --zookeeper localhost:2181 \
  --alter \
  --entity-type topics \
  --entity-name p-1 \
  --add-config retention.ms=5000