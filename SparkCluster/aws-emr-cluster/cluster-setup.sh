aws emr create-cluster \
--applications Name=Ganglia Name=Spark Name=Zeppelin \
--ec2-attributes '{"KeyName":"xxxx","InstanceProfile":"EMR_EC2_DefaultRole","SubnetId":"subnet-xxxx"}' \
--service-role EMR_DefaultRole \
--enable-debugging \
--release-label emr-5.13.0 --log-uri 's3n://xxxx/elasticmapreduce/' \
--name 'xxxx' \
--instance-groups '[{"InstanceCount":2,"InstanceGroupType":"CORE","InstanceType":"m3.xlarge","Name":"Core Instance Group"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"m3.xlarge","Name":"Master Instance Group"}]' \
--configurations '[{"Classification":"spark","Properties":{"maximizeResourceAllocation":"true"},"Configurations":[]}]' \
--scale-down-behavior TERMINATE_AT_TASK_COMPLETION --region eu-west-1