package io.github.javiercanillas.rest.controllers;

import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class OrderController {

    public static final String ORDER_POC = "OrderPOC";
    public static final int VERSION = 1;
    private final WorkflowClient workflowClient;

    @Autowired
    public OrderController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @PostMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> createOrder(@RequestBody Map<String,Object> orderData) {
        var startWorkflowRequest = new StartWorkflowRequest();
        startWorkflowRequest.setName(ORDER_POC);
        startWorkflowRequest.setVersion(VERSION);
        final var orderId = orderData.getOrDefault("orderId", UUID.randomUUID()).toString();
        startWorkflowRequest.setInput(Map.of("orderId", orderId));
        final var workflowId = this.workflowClient.startWorkflow(startWorkflowRequest);
        return Map.of("orderId", orderId, "workflowId", workflowId);
    }
}
