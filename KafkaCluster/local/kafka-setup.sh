#!/usr/bin/env bash
#MacOS Kafka installation commands
brew install kafka
#Linux Kafka installation commands

# default port is 2181 for zookeeper
zookeeper-server-start -daemon /usr/local/etc/kafka/zookeeper.properties

kafka-server-start -daemon /usr/local/etc/kafka/custom_config/server1.properties
kafka-server-start -daemon /usr/local/etc/kafka/custom_config/server2.properties

#Kafka cluster setup commands
kafka-topics --zookeeper localhost:2181 \
    --create --topic coordinate.stream --partitions 2 --replication-factor 2

# to check kafka instances
pgrep -f kafka