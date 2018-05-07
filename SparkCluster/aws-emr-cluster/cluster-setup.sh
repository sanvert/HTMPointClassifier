aws emr create-cluster \
--applications Name=Ganglia Name=Spark Name=Zeppelin \
--ec2-attributes '{"KeyName":"AWS-Root","InstanceProfile":"EMR_EC2_DefaultRole","SubnetId":"subnet-ddf25995"}' \
--service-role EMR_DefaultRole \
--enable-debugging \
--release-label emr-5.13.0 --log-uri 's3n://aws-logs-414486974611-eu-west-1/elasticmapreduce/' \
--name 'My cluster' \
--instance-groups '[{"InstanceCount":2,"InstanceGroupType":"CORE","InstanceType":"m3.xlarge","Name":"Core Instance Group"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"m3.xlarge","Name":"Master Instance Group"}]' \
--configurations '[{"Classification":"spark","Properties":{"maximizeResourceAllocation":"true"},"Configurations":[]}]' \
--scale-down-behavior TERMINATE_AT_TASK_COMPLETION --region eu-west-1