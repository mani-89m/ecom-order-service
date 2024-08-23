package com.orderservice.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dynamodb.model.Order;
import com.orderservice.service.OrderInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@Api(value = "Order API", description = "CRUD operations for Order Detail")
public class OrderInfoServiceController {

    @Autowired
    private OrderInfoService orderInfoService;

    @PostMapping("/order/create")
    @ApiOperation(value = "Create a new order", response = Order.class)
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
	String response = orderInfoService.createOrder(order);
	if (response.equalsIgnoreCase("SUCCESS")) {
	    return new ResponseEntity<>(response, HttpStatus.CREATED);
	} else {
	    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}
    }

    @GetMapping("/orders")
    @ApiOperation(value = "Retrieve all orders", response = List.class)
    public List<Order> getOrders() {
	return orderInfoService.getOrders();
    }

    @GetMapping("/order/{id}")
    @ApiOperation(value = "Retrieve orders based on ID", response = Order.class)
    public List<Order> getProductById(@PathVariable String id) {
	return orderInfoService.getOrderById(id);
    }

}
