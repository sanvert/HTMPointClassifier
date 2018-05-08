#!/usr/bin/env bash
tail -f /usr/local/share/spark-2.2.0-bin-hadoop2.7/logs/spark-sanver-org.apache.spark.deploy.worker.Worker-1-bb-system-0652.out

tail -f /usr/local/share/spark-2.2.0-bin-hadoop2.7/logs/spark-sanver-org.apache.spark.deploy.master.Master-1-bb-system-0652.out

#Web UI
http://<master-dns-name-or-ip>:8080/