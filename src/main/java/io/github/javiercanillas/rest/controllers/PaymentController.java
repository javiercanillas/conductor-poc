package io.github.javiercanillas.rest.controllers;

import io.github.javiercanillas.domain.PaymentOrderAuthorizer;
import io.github.javiercanillas.domain.PaymentOrderCanceller;
import io.github.javiercanillas.domain.PaymentOrderCapturer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController()
public class PaymentController {

    private final PaymentOrderAuthorizer authorizer;
    private final PaymentOrderCapturer capturer;
    private final PaymentOrderCanceller canceller;

    @Autowired
    PaymentController(PaymentOrderAuthorizer authorizer, PaymentOrderCapturer capturer, PaymentOrderCanceller canceller) {
        this.authorizer = authorizer;
        this.capturer = capturer;
        this.canceller = canceller;
    }

    @PostMapping(value = "/payment/order/{orderId}/authorize", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String,Object>  authorize(@PathVariable("orderId") String orderId, @RequestBody Map<String,Object> inputData) {
        return this.authorizer.execute(orderId, inputData);
    }

    @PostMapping(value = "/payment/order/{orderId}/capture", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String,Object>  capture(@PathVariable("orderId") String orderId, @RequestBody Map<String,Object> inputData) {
        return this.capturer.execute(orderId, inputData);
    }

    @PostMapping(value = "/payment/order/{orderId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String,Object>  cancel(@PathVariable("orderId") String orderId, @RequestBody Map<String,Object> inputData) {
        return this.capturer.execute(orderId, inputData);
    }
}
