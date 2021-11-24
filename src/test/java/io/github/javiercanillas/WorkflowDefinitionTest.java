package io.github.javiercanillas;

import com.google.protobuf.Any;
import com.google.protobuf.AnyProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.EventClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.events.EventHandler;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.WorkflowSummary;
import io.github.javiercanillas.workers.FraudOrderEvaluatorWorker;
import io.github.javiercanillas.workers.OrderStatusUpdaterWorker;
import io.github.javiercanillas.workers.PaymentOrderAuthorizerWorker;
import io.github.javiercanillas.workers.PaymentOrderCancellerWorker;
import io.github.javiercanillas.workers.PaymentOrderCapturerWorker;
import io.github.javiercanillas.workers.ProductOrderDeliveryWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.javiercanillas.rest.controllers.OrderController.ORDER_EVENT_POC;
import static io.github.javiercanillas.rest.controllers.OrderController.ORDER_WORKER_POC;
import static io.github.javiercanillas.rest.controllers.OrderController.VERSION;

/**
 * This is not a test itself, instead is a wait to automate the registration of a workflow with conductor.
 */
@Slf4j
class WorkflowDefinitionTest {

    private static final String CONDUCTOR_ROOT_URI = "http://localhost:5000/api/";
    public static final String ORDER_EVENT_FIRER_POC = "Order-Event-Firer-POC";

    private static MetadataClient metadataClient;
    private static EventClient eventClient;
    private static WorkflowClient workflowClient;

    @BeforeAll
    static void setup() {
        metadataClient = new MetadataClient();
        metadataClient.setRootURI(CONDUCTOR_ROOT_URI);

        eventClient = new EventClient();
        eventClient.setRootURI(CONDUCTOR_ROOT_URI);

        workflowClient = new WorkflowClient();
        workflowClient.setRootURI(CONDUCTOR_ROOT_URI);
    }

    @Test
    void registerWorkersWorkflow() {

        registerWorkerTasks();

        final var workflowDefinition = new WorkflowDef();
        workflowDefinition.setDescription(ORDER_WORKER_POC);
        workflowDefinition.setName(ORDER_WORKER_POC);
        workflowDefinition.setVersion(VERSION);
        workflowDefinition.setInputParameters(List.of("orderId"));
        var tasks = new ArrayList<WorkflowTask>();

        var fraudOrderEvaluatorTask = new WorkflowTask();
        fraudOrderEvaluatorTask.setDescription("Run Fraud check analysis");
        fraudOrderEvaluatorTask.setName(FraudOrderEvaluatorWorker.TASK_DEF_NAME);
        fraudOrderEvaluatorTask.setTaskReferenceName("fraudOrderEvaluator");
        fraudOrderEvaluatorTask.setType(TaskType.SIMPLE.name());
        fraudOrderEvaluatorTask.setRetryCount(3);
        fraudOrderEvaluatorTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}"));
        tasks.add(fraudOrderEvaluatorTask);

        var fraudResultDecisionTask = new WorkflowTask();
        fraudResultDecisionTask.setDescription("Fraud Result decision");
        fraudResultDecisionTask.setName("fraud-result-decision");
        fraudResultDecisionTask.setTaskReferenceName("fraudResultDecision");
        fraudResultDecisionTask.setType(TaskType.SWITCH.name());
        fraudResultDecisionTask.setEvaluatorType("value-param");
        fraudResultDecisionTask.setInputParameters(Map.of("value", "${fraudOrderEvaluator.output.result}"));
        fraudResultDecisionTask.setExpression("value");

        var paymentOrderAuthorizerTask = new WorkflowTask();
        paymentOrderAuthorizerTask.setDescription("Collect all payment methods");
        paymentOrderAuthorizerTask.setName(PaymentOrderAuthorizerWorker.TASK_DEF_NAME);
        paymentOrderAuthorizerTask.setTaskReferenceName("paymentOrderCollector");
        paymentOrderAuthorizerTask.setType(TaskType.SIMPLE.name());
        paymentOrderAuthorizerTask.setRetryCount(3);
        paymentOrderAuthorizerTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}"));

        var authorizationResultDecisionTask = new WorkflowTask();
        authorizationResultDecisionTask.setDescription("Collection Result decision");
        authorizationResultDecisionTask.setName("collection-result-decision");
        authorizationResultDecisionTask.setTaskReferenceName("collectionResultDecision");
        authorizationResultDecisionTask.setType(TaskType.SWITCH.name());
        authorizationResultDecisionTask.setEvaluatorType("value-param");
        authorizationResultDecisionTask.setInputParameters(Map.of("value", "${paymentOrderCollector.output.result}"));
        authorizationResultDecisionTask.setExpression("value");

        var productOrderDeliveryTask = new WorkflowTask();
        productOrderDeliveryTask.setDescription("Deliver all products");
        productOrderDeliveryTask.setName(ProductOrderDeliveryWorker.TASK_DEF_NAME);
        productOrderDeliveryTask.setTaskReferenceName("productOrderDelivery");
        productOrderDeliveryTask.setType(TaskType.SIMPLE.name());
        productOrderDeliveryTask.setRetryCount(3);
        productOrderDeliveryTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}"));

        var deliveryResultDecisionTask = new WorkflowTask();
        deliveryResultDecisionTask.setDescription("Delivery Result decision");
        deliveryResultDecisionTask.setName("delivery-result-decision");
        deliveryResultDecisionTask.setTaskReferenceName("deliveryResultDecision");
        deliveryResultDecisionTask.setType(TaskType.SWITCH.name());
        deliveryResultDecisionTask.setEvaluatorType("value-param");
        deliveryResultDecisionTask.setInputParameters(Map.of("value", "${productOrderDelivery.output.result}"));
        deliveryResultDecisionTask.setExpression("value");

        var deliveryEventTask = new WorkflowTask();
        deliveryEventTask.setName("all-done-event");
        deliveryEventTask.setDescription("All done!");
        deliveryEventTask.setTaskReferenceName("allDoneEvent");
        deliveryEventTask.setType(TaskType.EVENT.name());
        deliveryEventTask.setSink("conductor");

        var paymentOrderCapturerTask = new WorkflowTask();
        paymentOrderCapturerTask.setDescription("Capture all payment authorizations");
        paymentOrderCapturerTask.setName(PaymentOrderCapturerWorker.TASK_DEF_NAME);
        paymentOrderCapturerTask.setTaskReferenceName("paymentOrderCapturer");
        paymentOrderCapturerTask.setType(TaskType.SIMPLE.name());
        paymentOrderCapturerTask.setRetryCount(5);
        paymentOrderCapturerTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}",
                "payment-authorizationId", "${paymentOrderCollector.output.payment-authorizationId"));

        var paymentOrderCancellerTask = new WorkflowTask();
        paymentOrderCancellerTask.setDescription("Cancel all payment authorizations");
        paymentOrderCancellerTask.setName(PaymentOrderCancellerWorker.TASK_DEF_NAME);
        paymentOrderCancellerTask.setTaskReferenceName("paymentOrderCanceller");
        paymentOrderCancellerTask.setType(TaskType.SIMPLE.name());
        paymentOrderCancellerTask.setRetryCount(5);
        paymentOrderCancellerTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}",
                "payment-authorizationId", "${paymentOrderCollector.output.payment-authorizationId"));

        deliveryResultDecisionTask.setDecisionCases(Map.of(
                "CONTINUE", List.of(createOrderStatusChange("order-approved", "APPROVED"),
                        deliveryEventTask,
                        paymentOrderCapturerTask),
                "BREAK", List.of(
                        createOrderStatusChange("order-in-declined-delivery", "DECLINED"),
                        paymentOrderCancellerTask)
                )
        );

        authorizationResultDecisionTask.setDecisionCases(Map.of(
                "CONTINUE", List.of(productOrderDeliveryTask, deliveryResultDecisionTask),
                "BREAK", List.of(
                        createOrderStatusChange("order-in-ccerror", "CC_ERROR"))
                )
        );
        //authorizationResultDecisionTask.setDefaultCase(List.of(productOrderDeliveryTask, deliveryResultDecisionTask));

        fraudResultDecisionTask.setDecisionCases(Map.of(
                "CONTINUE", List.of(paymentOrderAuthorizerTask, authorizationResultDecisionTask),
                "BREAK", List.of(
                        createOrderStatusChange("order-in-declined-fraud", "DECLINED"))
                )
        );
        //fraudOrderEvaluatorTask.setDefaultCase(List.of(paymentOrderAuthorizerTask, authorizationResultDecisionTask));

        tasks.add(fraudResultDecisionTask);

        workflowDefinition.setTasks(tasks);
        workflowDefinition.setOwnerEmail("example@email.com");
        workflowDefinition.setTimeoutSeconds(0);

        registerWorkflow(workflowDefinition);
    }

    private void registerWorkflow(WorkflowDef aWorkflowDefinition) {
        WorkflowDef oldWorkflowDefinition = null;
        try {
            oldWorkflowDefinition = metadataClient.getWorkflowDef(aWorkflowDefinition.getName(), aWorkflowDefinition.getVersion());
        } catch (ConductorClientException ex) {
            // check it really doesn't exists.
            Assertions.assertEquals(404, ex.getStatus());
        }

        if (oldWorkflowDefinition != null) {
            metadataClient.updateWorkflowDefs(List.of(aWorkflowDefinition));
        } else {
            metadataClient.registerWorkflowDef(aWorkflowDefinition);
        }
    }

    private WorkflowTask createOrderStatusChange(String taskReference, String status) {
        var task = new WorkflowTask();
        task.setType(TaskType.SIMPLE.name());
        task.setName(OrderStatusUpdaterWorker.TASK_DEF_NAME);
        task.setTaskReferenceName(taskReference);
        task.setDescription("Change Order status to " + status);
        task.setInputParameters(Map.of("orderId", "${workflow.input.orderId}", "newStatus", status));
        return task;
    }

    void registerWorkerTasks() {
        var taskDefinition = new TaskDef();
        taskDefinition.setDescription("Run Fraud check analysis");
        taskDefinition.setName(FraudOrderEvaluatorWorker.TASK_DEF_NAME);
        taskDefinition.setOwnerEmail("example@email.com");
        taskDefinition.setInputKeys(List.of("orderId"));
        taskDefinition.setOutputKeys(List.of("result"));
        taskDefinition.setConcurrentExecLimit(1000);
        registerTask(taskDefinition);

        taskDefinition = new TaskDef();
        taskDefinition.setDescription("Collect payments from order");
        taskDefinition.setName(PaymentOrderAuthorizerWorker.TASK_DEF_NAME);
        taskDefinition.setOwnerEmail("example@email.com");
        taskDefinition.setInputKeys(List.of("orderId"));
        taskDefinition.setOutputKeys(List.of("payment-authorizationId", "payment-transactionId"));
        taskDefinition.setConcurrentExecLimit(1000);
        registerTask(taskDefinition);

        taskDefinition = new TaskDef();
        taskDefinition.setDescription("Collect payments from order");
        taskDefinition.setName(ProductOrderDeliveryWorker.TASK_DEF_NAME);
        taskDefinition.setOwnerEmail("example@email.com");
        taskDefinition.setInputKeys(List.of("orderId"));
        taskDefinition.setOutputKeys(List.of("product-nsu"));
        taskDefinition.setConcurrentExecLimit(1000);
        registerTask(taskDefinition);

        taskDefinition = new TaskDef();
        taskDefinition.setDescription("Change status of order");
        taskDefinition.setName(OrderStatusUpdaterWorker.TASK_DEF_NAME);
        taskDefinition.setOwnerEmail("example@email.com");
        taskDefinition.setInputKeys(List.of("orderId"));
        taskDefinition.setOutputKeys(List.of());
        taskDefinition.setConcurrentExecLimit(1000);
        registerTask(taskDefinition);

        taskDefinition = new TaskDef();
        taskDefinition.setDescription("Cancel order payment authorizations");
        taskDefinition.setName(PaymentOrderCancellerWorker.TASK_DEF_NAME);
        taskDefinition.setOwnerEmail("example@email.com");
        taskDefinition.setInputKeys(List.of("orderId"));
        taskDefinition.setOutputKeys(List.of());
        taskDefinition.setConcurrentExecLimit(1000);
        registerTask(taskDefinition);

        taskDefinition = new TaskDef();
        taskDefinition.setDescription("Capture order payment authorizations");
        taskDefinition.setName(PaymentOrderCapturerWorker.TASK_DEF_NAME);
        taskDefinition.setOwnerEmail("example@email.com");
        taskDefinition.setInputKeys(List.of("orderId"));
        taskDefinition.setOutputKeys(List.of());
        taskDefinition.setConcurrentExecLimit(1000);
        registerTask(taskDefinition);
    }

    void registerTask(TaskDef aTaskDefinition) {
        TaskDef oldTaskDefinition = null;
        try {
            oldTaskDefinition = metadataClient.getTaskDef(aTaskDefinition.getName());
        } catch (ConductorClientException ex) {
            // check it really doesn't exists.
            Assertions.assertEquals(404, ex.getStatus());
        }

        if (oldTaskDefinition != null) {
            metadataClient.updateTaskDef(aTaskDefinition);
        } else {
            metadataClient.registerTaskDefs(List.of(aTaskDefinition));
        }
    }

    @Test
    void cleanupWorkflowExecutions() {
        var remaining = -1L;
        do {
            final var workflowSearchResult = workflowClient.search(0, 10, "startTime:DESC", "*", "");
            remaining = workflowSearchResult.getTotalHits() - workflowSearchResult.getResults().size();
            final var workflowIds = workflowSearchResult.getResults()
                    .stream()
                    .map(WorkflowSummary::getWorkflowId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.info("Terminating {}, remains {}", workflowIds.size(), remaining);
            if (workflowIds.size() > 0) {
                workflowClient.terminateWorkflows(workflowIds, "They will be erased");
                workflowIds.forEach(id -> workflowClient.deleteWorkflow(id, false));
            }
        } while (remaining > 0);
    }

    void registerEventHandlers() {
        eventClient.getEventHandlers("conductor:" + ORDER_EVENT_POC + ":fraud-check-result-wait", false)
                .stream().findAny().ifPresent(eh -> eventClient.unregisterEventHandler(eh.getName()));

        var eventHandler = new EventHandler();
        eventHandler.setName("fraud-check-event");
        // conductor:{workflow_name}:{taskReferenceName}
        // sqs:{my_sqs_queue_name}
        eventHandler.setEvent("conductor:" + ORDER_EVENT_FIRER_POC + ":eventFirer");
        eventHandler.setActive(true);
        var action = new EventHandler.Action();
        action.setAction(EventHandler.Action.Type.complete_task);
        var taskDetails = new EventHandler.TaskDetails();
        taskDetails.setWorkflowId("${sourceWorkflowId}");
        taskDetails.setTaskRefName("fraudCheckResultWait");
        /* TODO: For some reason I couldn't make the eventhandler update output parameters of the awaken task (wait task).
        So this workflow is unfinished.
        */
        var map = Struct.newBuilder()
                .putFields("score", Value.newBuilder()
                        .setStringValue("-99")
                        .build())
                .putFields("result", Value.newBuilder()
                        .setStringValue("ABC")
                        .build())
                .build();
        taskDetails.setOutputMessage(Any.pack(map));
        taskDetails.setOutput(Map.of(
                "score", "${workflow.input.score}",
                "result", "${workflow.input.result}"));
        action.setComplete_task(taskDetails);
        eventHandler.setActions(List.of(action));
        registerEventHandler(eventHandler);
    }

    void registerEventHandler(EventHandler anEventHandler) {
        final var eventHandlers = eventClient.getEventHandlers(anEventHandler.getEvent(), false);
        eventHandlers.stream().filter(eh -> anEventHandler.getName().equals(eh.getName()))
                .findAny()
                .ifPresentOrElse(eh -> eventClient.updateEventHandler(anEventHandler),
                        () -> eventClient.registerEventHandler(anEventHandler));
    }

    @Test
    void registerEventWorkflow() {
        registerEventHandlers();

        // Workflow to fire an event
        var workflowDefinition = new WorkflowDef();
        workflowDefinition.setDescription(ORDER_EVENT_FIRER_POC);
        workflowDefinition.setName(ORDER_EVENT_FIRER_POC);
        workflowDefinition.setVersion(VERSION);
        workflowDefinition.setInputParameters(List.of("result", "score", "sourceWorkflowId"));

        var eventFireTask = new WorkflowTask();
        eventFireTask.setName("eventFirer");
        eventFireTask.setTaskReferenceName("eventFirer");
        eventFireTask.setInputParameters(Map.of(
                "sourceWorkflowId", "${workflow.input.sourceWorkflowId}",
                "score", "${workflow.input.score}",
                "result", "${workflow.input.result}"
                )
        );
        eventFireTask.setType(TaskType.EVENT.name());
        eventFireTask.setSink("conductor");
        workflowDefinition.setTasks(List.of(eventFireTask));
        workflowDefinition.setOwnerEmail("example@email.com");
        workflowDefinition.setTimeoutSeconds(0);
        registerWorkflow(workflowDefinition);

        // Principal workflow
        workflowDefinition = new WorkflowDef();
        workflowDefinition.setDescription(ORDER_EVENT_POC);
        workflowDefinition.setName(ORDER_EVENT_POC);
        workflowDefinition.setVersion(VERSION);
        workflowDefinition.setInputParameters(List.of("orderId"));
        var tasks = new ArrayList<WorkflowTask>();

        var fraudOrderEvaluatorTask = new WorkflowTask();
        fraudOrderEvaluatorTask.setDescription("Fire Fraud check analysis");
        fraudOrderEvaluatorTask.setName("fraud-check-async");
        fraudOrderEvaluatorTask.setTaskReferenceName("fraudOrderEvaluator");
        fraudOrderEvaluatorTask.setType(TaskType.HTTP.name());
        fraudOrderEvaluatorTask.setRetryCount(3);
        fraudOrderEvaluatorTask.setInputParameters(Map.of(
                "http_request", Map.of(
                        "uri", "http://host.docker.internal:8081/fraud/order",
                        "method", "POST",
                        "accept", "application/json",
                        "contentType", "application/json",
                        "body", Map.of(
                                "orderId", "${workflow.input.orderId}",
                                "workflowId", "${workflow.workflowId}",
                                "callback", Map.of(
                                        "workflow", Map.of("name", ORDER_EVENT_FIRER_POC,
                                                "version", 1)
                                )
                        )
                ),
                "asyncComplete", "false",
                "connectionTimeOut", "100",
                "readTimeOut", "1000"
                )
        );
        tasks.add(fraudOrderEvaluatorTask);

        var fraudOrderEvaluatorResultWaitTask = new WorkflowTask();
        fraudOrderEvaluatorResultWaitTask.setDescription("Wait Fraud check results");
        fraudOrderEvaluatorResultWaitTask.setName("fraud-check-result-wait");
        fraudOrderEvaluatorResultWaitTask.setTaskReferenceName("fraudCheckResultWait");
        fraudOrderEvaluatorResultWaitTask.setType(TaskType.WAIT.name());
        tasks.add(fraudOrderEvaluatorResultWaitTask);

        workflowDefinition.setTasks(tasks);
        workflowDefinition.setOwnerEmail("example@email.com");
        workflowDefinition.setTimeoutSeconds(0);

        registerWorkflow(workflowDefinition);
    }

}
