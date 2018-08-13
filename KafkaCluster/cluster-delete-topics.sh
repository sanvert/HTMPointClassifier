#!/usr/bin/env bash
sudo /opt/kafka_2.11-1.1.0/bin/kafka-topics.sh --zookeeper $1 --delete --topic p-1
sudo /opt/kafka_2.11-1.1.0/bin/kafka-topics.sh --zookeeper $1 --delete --topic c-1