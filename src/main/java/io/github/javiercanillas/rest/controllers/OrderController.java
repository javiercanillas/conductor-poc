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

    public static final String ORDER_WORKER_POC = "Order-Workers-POC";
    public static final String ORDER_EVENT_POC = "Order-Event-POC";
    public static final int VERSION = 1;

    private final WorkflowClient workflowClient;

    @Autowired
    public OrderController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @PostMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> orderData) {
        var startWorkflowRequest = new StartWorkflowRequest();
        startWorkflowRequest.setName(orderData.getOrDefault("workflowDefinition", ORDER_WORKER_POC).toString());
        startWorkflowRequest.setVersion(Integer.valueOf(orderData.getOrDefault("version", VERSION).toString()));
        final var orderId = orderData.getOrDefault("orderId", UUID.randomUUID()).toString();
        startWorkflowRequest.setInput(Map.of("orderId", orderId));
        final var workflowId = this.workflowClient.startWorkflow(startWorkflowRequest);
        return Map.of(
                "orderId", orderId,
                "workflowId", workflowId,
                "workflowDefinition", Map.of(
                        "name", startWorkflowRequest.getName(),
                        "version", startWorkflowRequest.getVersion()
                )
        );
    }
}
