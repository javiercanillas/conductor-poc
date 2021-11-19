package io.github.javiercanillas.rest.controllers;

import io.github.javiercanillas.domain.ProductOrderDelivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController()
public class ProductController {

    private final ProductOrderDelivery delivery;

    @Autowired
    ProductController(ProductOrderDelivery delivery) {
        this.delivery = delivery;
    }

    @PostMapping(value = "/product/order/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String,Object>  collect(@PathVariable("orderId") String orderId, @RequestBody Map<String,Object> inputData) {
        return this.delivery.execute(orderId, inputData);
    }
}
