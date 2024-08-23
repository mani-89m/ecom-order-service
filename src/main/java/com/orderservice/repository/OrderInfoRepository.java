package com.orderservice.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import com.dynamodb.model.Order;

@EnableScan
public interface OrderInfoRepository extends CrudRepository<Order, String> {

}
