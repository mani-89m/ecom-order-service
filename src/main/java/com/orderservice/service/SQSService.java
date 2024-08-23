package com.orderservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;

@Service
public class SQSService {

    @Autowired
    private AmazonSQS sqsClient;

    @Value("${config.aws.sqs.queue-name}")
    private String queueName;

    public void sendMessage(String msg) {
	SendMessageRequest send_msg_request = new SendMessageRequest()
		.withQueueUrl(queueName)
		.withMessageBody(msg)
		.withDelaySeconds(5);
	sqsClient.sendMessage(send_msg_request);
    }
}
