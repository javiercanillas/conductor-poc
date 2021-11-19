package io.github.javiercanillas.rest.controllers;

import io.github.javiercanillas.domain.FraudOrderChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FraudController {

    private final FraudOrderChecker checker;

    @Autowired
    FraudController(FraudOrderChecker checker) {
        this.checker = checker;
    }

    @PostMapping(value = "/fraud/order/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object>  check(@PathVariable("orderId") String orderId, @RequestBody Map<String,Object> inputData) {
        return this.checker.execute(orderId, inputData);
    }
}
