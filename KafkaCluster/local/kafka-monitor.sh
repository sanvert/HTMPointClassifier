#!/usr/bin/env bash

#List all topics.
sudo /opt/kafka_2.11-1.1.0/bin/kafka-topics.sh --list --zookeeper localhost:2181

#Partition info for topic
sudo /opt/kafka_2.11-1.1.0/bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic c-1