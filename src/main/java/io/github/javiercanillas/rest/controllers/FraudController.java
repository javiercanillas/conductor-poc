package io.github.javiercanillas.rest.controllers;

import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import io.github.javiercanillas.domain.ActionException;
import io.github.javiercanillas.domain.FraudOrderChecker;
import io.github.javiercanillas.domain.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class FraudController {

    private final FraudOrderChecker checker;
    private final ExecutorService backgroundExecutor;
    private final WorkflowClient workflowClient;

    @Autowired
    FraudController(FraudOrderChecker checker, ExecutorService backgroundExecutor,
                    WorkflowClient workflowClient) {
        this.checker = checker;
        this.backgroundExecutor = backgroundExecutor;
        this.workflowClient = workflowClient;
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/fraud/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> check(@RequestBody final Map<String,Object> inputData) {
        log.info("about to check {} for fraud", inputData);
        final var orderId = inputData.get("orderId").toString();

        var callbackInformation = inputData.entrySet().stream()
                .filter(entry -> "callback".equals(entry.getKey()))
                .map(entry -> (Map<String,Object>) entry.getValue())
                .flatMap(entry -> entry.entrySet().stream())
                .findAny();

        var workflowId = inputData.entrySet().stream().filter(entry -> "workflowId".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(String.class::cast)
                .findAny();

        var workflowInformation = callbackInformation.stream()
                .filter(entry -> "workflow".equals(entry.getKey()))
                .map(entry -> (Map<String,Object>) entry.getValue())
                .flatMap(entry -> entry.entrySet().stream())
                .collect(Collectors.toSet());

        var callbackWorkflowName = workflowInformation.stream().filter(entry -> "name".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(String.class::cast)
                .findAny();

        var callbackWorkflowVersion = workflowInformation.stream().filter(entry -> "version".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(Integer.class::cast)
                .findAny();

        if (callbackWorkflowName.isEmpty() || callbackWorkflowVersion.isEmpty() || workflowId.isEmpty()) {
            log.error("Missing workflow definition information");
            return ResponseEntity.badRequest().build();
        }

        log.info("submitting work for {}: {}", orderId, inputData);
        this.backgroundExecutor.submit( () -> {
            final Map<String,Object> result = new HashMap<>();
            result.put("sourceWorkflowId", workflowId.get());
            try {
                this.checker.execute(orderId, inputData).forEach(result::put);
                result.put("result", "CONTINUE");
            } catch (ActionException e) {
                e.getOutputData().forEach(result::put);
                result.put("result", "BREAK");
            } catch (RuntimeException e) {
                result.put("exception", Utils.printException(e));
            }
            var startWorkflowRequest = new StartWorkflowRequest();
            startWorkflowRequest.setInput(result);
            startWorkflowRequest.setCorrelationId(workflowId.get());
            startWorkflowRequest.setName(callbackWorkflowName.get());
            startWorkflowRequest.setVersion(callbackWorkflowVersion.get());
            log.info("leeeerooy {}", startWorkflowRequest);
            this.workflowClient.startWorkflow(startWorkflowRequest);
        });

        return ResponseEntity.noContent().build();
    }
}
