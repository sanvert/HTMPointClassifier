# Apache Elastic Map Reduce Submit Step
# https://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-spark-submit-step.html

aws emr add-steps --cluster-id j-2HKF0UI49PFO9 --steps Type=Spark,Name="Spark application",ActionOnFailure=CONTINUE,\
Args="[--class,edu.spark.JavaSparkPi,--deploy-mode,cluster,--master,yarn,--conf, spark.executor.instances=3,--conf,spark.executor.cores=1,--conf,spark.executor.memory=1g,--conf,spark.yarn.submit.waitAppCompletion=false,s3://codelocation-emr-test/point.stream.classifier-1.0.jar]"

# Without overriding resource settings
aws emr add-steps --cluster-id j-2HKF0UI49PFO9 --steps Type=Spark,Name="Spark application",ActionOnFailure=CONTINUE,\
Args="[--class,edu.spark.JavaSparkPi,--deploy-mode,cluster,--master,yarn,--conf,spark.executor.memory=12g,--conf,spark.yarn.submit.waitAppCompletion=false,s3://codelocation-emr-test/point.stream.classifier-1.0.jar]"
