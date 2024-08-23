package com.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

@Configuration
public class AwsSQSConfig {

    @Value("${config.aws.region}")
    private String region;

    @Value("${config.aws.sqs.url}")
    private String sqsEndpointUrl;

    @Value("${config.aws.sqs.access-key}")
    private String accessKey;

    @Value("${config.aws.sqs.secret-key}")
    private String secretKey;

    @Bean
    public AmazonSQS sqsClient() {
	BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
	return AmazonSQSClientBuilder.standard().withRegion(Regions.fromName(region))
		.withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials)).build();
    }

}
