package edu.amazon.dynamodb;

import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

import java.util.Date;

import edu.amazon.dynamodb.model.OperationResult;
import edu.util.PropertyMapper;

public class DynamoDBClient {

    private static volatile AmazonDynamoDB client;

    static {
        try {
            PropertyMapper propertyMapper = new PropertyMapper("SYS");
            client = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(Regions.EU_WEST_1)
                    .withCredentials(new SystemPropertiesCredentialsProvider())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AmazonDynamoDB getInstance() {
        return client;
    }

    public static void main(String[] args) {
        AmazonDynamoDB client = DynamoDBClient.getInstance();
        DynamoDBMapper mapper = new DynamoDBMapper(client);

        OperationResult result = new OperationResult();
        result.setResult("trial");
        result.setTimestamp(new Date());

        mapper.save(result);
    }
}
