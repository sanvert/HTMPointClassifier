#!/usr/bin/env bash
sudo /opt/kafka/kafka_2.11-1.1.0/bin/kafka-topics.sh --create --zookeeper $1 --replication-factor 1 --partitions 30 --topic p-1

sudo /opt/kafka_2.11-1.1.0/bin/kafka-configs.sh \
  --zookeeper $1 \
  --alter \
  --entity-type topics \
  --entity-name p-1 \
  --add-config retention.ms=5000

sudo /opt/kafka/kafka_2.11-1.1.0/bin/kafka-topics.sh --create --zookeeper $1 --replication-factor 1 --partitions 6 --topic c-1

sudo /opt/kafka_2.11-1.1.0/bin/kafka-configs.sh \
  --zookeeper $1 \
  --alter \
  --entity-type topics \
  --entity-name c-1 \
  --add-config retention.ms=10000

