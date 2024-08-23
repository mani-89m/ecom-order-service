package com.orderservice.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.dynamodb.model.Order;
import com.dynamodb.model.OrderItem;
import com.dynamodb.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderservice.repository.OrderInfoRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class OrderInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OrderInfoService.class);

    @Value("${product.service.endpoint}")
    private String productServiceEndpoint;

    @Autowired
    private OrderInfoRepository orderInfoRepository;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SQSService sqsService;

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
	    false);

    public List<Order> getOrders() {
	Iterable<Order> orders = orderInfoRepository.findAll();
	List<Order> orderList = new ArrayList<>();
	orders.forEach(orderList::add);
	return orderList;
    }

    @CircuitBreaker(name = "ORDER_SERVICE", fallbackMethod = "createOrder_Fallback")
    public String createOrder(final Order order) {
	List<String> differences = validateProductsAndReturnDifferences(order);
	if (!differences.isEmpty()) {
	    String invalidProductIds = differences.stream().collect(Collectors.joining(","));
	    return "The order has some invalid products(" + invalidProductIds + ")";
	}

	String orderId = UUID.randomUUID().toString();
	Date date = new Date();
	order.setOrderId(orderId);
	order.setOrderPlacedTimestamp(date);
	List<Order> orders = new ArrayList<>();
	for (OrderItem oi : order.getItems()) {
	    addOrders(order, orderId, date, orders, oi);
	}
	orderInfoRepository.saveAll(orders);
	sqsService.sendMessage(prepareOrderMessage(order));
	return "SUCCESS";
    }

    private String prepareOrderMessage(final Order order) {
	try {
	    return mapper.writeValueAsString(order);
	} catch (JsonProcessingException e) {
	    logger.error("exception while forming SQS message {}", e);
	}
	return null;
    }

    private void addOrders(final Order order, String orderId, Date date, List<Order> orders, OrderItem oi) {
	Order o = new Order();
	o.setDescription(order.getDescription());
	o.setOrderId(orderId);
	o.setProductId(oi.getProductId());
	o.setProductQuantity(oi.getQuantity());
	o.setOrderPlacedTimestamp(date);
	o.setOrderUpdatedTimestamp(date);
	orders.add(o);
    }

    private List<String> validateProductsAndReturnDifferences(final Order order) {
	List<String> inputProductIds = order.getItems().stream().map(o -> o.getProductId())
		.collect(Collectors.toList());
	HttpEntity<List<String>> requestEntity = new HttpEntity<>(inputProductIds);
	ResponseEntity<List<Product>> responseEntity = restTemplate.exchange(productServiceEndpoint + "/products",
		HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<Product>>() {
		});

	List<Product> productList = responseEntity.getBody();
	List<String> productIdsInResponse = productList.stream().map(p -> p.getProductId())
		.collect(Collectors.toList());
	List<String> differences = new ArrayList<>(inputProductIds);
	differences.removeAll(productIdsInResponse);
	return differences;
    }

    public List<Order> getOrderById(final String orderId) {
	List<Order> orderList = new ArrayList<>();

	DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
	Table table = dynamoDB.getTable("OrderInfo");
	QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("orderId = :o_id")
		.withValueMap(new ValueMap().withString(":o_id", orderId));

	try {
	    ItemCollection<QueryOutcome> results = table.query(querySpec);
	    Iterator<Item> iterator = results.iterator();
	    Item item = null;
	    while (iterator.hasNext()) {
		item = iterator.next();
		orderList.add(mapper.readValue(item.toJSON(), Order.class));
	    }
	} catch (Exception e) {
	    logger.error("exception while fetching order by id {}", e);
	}
	return orderList;
    }

    public String createOrder_Fallback(Exception e) {
	logger.error("excpetion while making product-service call {}", e);
	String message = "CIRCUIT BREAKER ENABLED!!! No Response from Product Service at this moment. Service will be back shortly";
	logger.warn(message);
	return message;
    }

}
