package io.github.javiercanillas;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import io.github.javiercanillas.workers.FraudOrderEvaluatorWorker;
import io.github.javiercanillas.workers.OrderStatusUpdaterWorker;
import io.github.javiercanillas.workers.PaymentOrderAuthorizerWorker;
import io.github.javiercanillas.workers.PaymentOrderCancellerWorker;
import io.github.javiercanillas.workers.PaymentOrderCapturerWorker;
import io.github.javiercanillas.workers.ProductOrderDeliveryWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.javiercanillas.rest.controllers.OrderController.ORDER_POC;
import static io.github.javiercanillas.rest.controllers.OrderController.VERSION;

/**
 * This is not a test itself, instead is a wait to automate the registration of a workflow with conductor.
 */
class WorkflowDefinitionTest {

    private static final String CONDUCTOR_ROOT_URI = "http://localhost:5000/api/";

    private static MetadataClient metadataClient;

    @BeforeAll
    static void setup() {
        metadataClient = new MetadataClient();
        metadataClient.setRootURI(CONDUCTOR_ROOT_URI);
    }

    @Test
    void registerWorkflow() {
        WorkflowDef workflowDefinition = null;
        try {
            workflowDefinition = metadataClient.getWorkflowDef(ORDER_POC, VERSION);
        } catch (ConductorClientException ex) {
            // check it really doesn't exists.
            Assertions.assertEquals(404, ex.getStatus());
        }

        if (workflowDefinition != null) {
            metadataClient.unregisterWorkflowDef(ORDER_POC, VERSION);
        }

        workflowDefinition = new WorkflowDef();
        workflowDefinition.setDescription(ORDER_POC);
        workflowDefinition.setName(ORDER_POC);
        workflowDefinition.setVersion(VERSION);
        workflowDefinition.setInputParameters(List.of("orderId"));
        var tasks = new ArrayList<WorkflowTask>();

        var fraudOrderEvaluatorTask = new WorkflowTask();
        fraudOrderEvaluatorTask.setDescription("Run Fraud check analysis");
        fraudOrderEvaluatorTask.setName(FraudOrderEvaluatorWorker.TASK_DEF_NAME);
        fraudOrderEvaluatorTask.setTaskReferenceName("fraudOrderEvaluator");
        fraudOrderEvaluatorTask.setType("SIMPLE");
        fraudOrderEvaluatorTask.setRetryCount(3);
        fraudOrderEvaluatorTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}"));
        tasks.add(fraudOrderEvaluatorTask);

        var fraudResultDecisionTask = new WorkflowTask();
        fraudResultDecisionTask.setDescription("Fraud Result decision");
        fraudResultDecisionTask.setName("fraud-result-decision");
        fraudResultDecisionTask.setTaskReferenceName("fraudResultDecision");
        fraudResultDecisionTask.setType("SWITCH");
        fraudResultDecisionTask.setEvaluatorType("value-param");
        fraudResultDecisionTask.setInputParameters(Map.of("value", "${fraudOrderEvaluator.output.result}"));
        fraudResultDecisionTask.setExpression("value");

        var paymentOrderAuthorizerTask = new WorkflowTask();
        paymentOrderAuthorizerTask.setDescription("Collect all payment methods");
        paymentOrderAuthorizerTask.setName(PaymentOrderAuthorizerWorker.TASK_DEF_NAME);
        paymentOrderAuthorizerTask.setTaskReferenceName("paymentOrderCollector");
        paymentOrderAuthorizerTask.setType("SIMPLE");
        paymentOrderAuthorizerTask.setRetryCount(3);
        paymentOrderAuthorizerTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}"));

        var authorizationResultDecisionTask = new WorkflowTask();
        authorizationResultDecisionTask.setDescription("Collection Result decision");
        authorizationResultDecisionTask.setName("collection-result-decision");
        authorizationResultDecisionTask.setTaskReferenceName("collectionResultDecision");
        authorizationResultDecisionTask.setType("SWITCH");
        authorizationResultDecisionTask.setEvaluatorType("value-param");
        authorizationResultDecisionTask.setInputParameters(Map.of("value", "${paymentOrderCollector.output.result}"));
        authorizationResultDecisionTask.setExpression("value");

        var productOrderDeliveryTask = new WorkflowTask();
        productOrderDeliveryTask.setDescription("Deliver all products");
        productOrderDeliveryTask.setName(ProductOrderDeliveryWorker.TASK_DEF_NAME);
        productOrderDeliveryTask.setTaskReferenceName("productOrderDelivery");
        productOrderDeliveryTask.setType("SIMPLE");
        productOrderDeliveryTask.setRetryCount(3);
        productOrderDeliveryTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}"));

        var deliveryResultDecisionTask = new WorkflowTask();
        deliveryResultDecisionTask.setDescription("Delivery Result decision");
        deliveryResultDecisionTask.setName("delivery-result-decision");
        deliveryResultDecisionTask.setTaskReferenceName("deliveryResultDecision");
        deliveryResultDecisionTask.setType("SWITCH");
        deliveryResultDecisionTask.setEvaluatorType("value-param");
        deliveryResultDecisionTask.setInputParameters(Map.of("value", "${productOrderDelivery.output.result}"));
        deliveryResultDecisionTask.setExpression("value");

        var deliveryEventTask = new WorkflowTask();
        deliveryEventTask.setName("all-done-event");
        deliveryEventTask.setDescription("All done!");
        deliveryEventTask.setTaskReferenceName("allDoneEvent");
        deliveryEventTask.setType("EVENT");
        deliveryEventTask.setSink("conductor");

        var paymentOrderCapturerTask = new WorkflowTask();
        paymentOrderCapturerTask.setDescription("Capture all payment authorizations");
        paymentOrderCapturerTask.setName(PaymentOrderCapturerWorker.TASK_DEF_NAME);
        paymentOrderCapturerTask.setTaskReferenceName("paymentOrderCapturer");
        paymentOrderCapturerTask.setType("SIMPLE");
        paymentOrderCapturerTask.setRetryCount(5);
        paymentOrderCapturerTask.setInputParameters(Map.of("orderId", "${workflow.input.orderId}",
                "payment-authorizationId", "${paymentOrderCollector.output.payment-authorizationId"));

        var paymentOrderCancellerTask = new WorkflowTask();
        paymentOrderCancellerTask.setDescription("Cancel all payment authorizations");
        paymentOrderCancellerTask.setName(PaymentOrderCancellerWorker.TASK_DEF_NAME);
        paymentOrderCancellerTask.setTaskReferenceName("paymentOrderCanceller");
        paymentOrderCancellerTask.setType("SIMPLE");
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
        metadataClient.registerWorkflowDef(workflowDefinition);
    }

    private WorkflowTask createOrderStatusChange(String taskReference, String status) {
        var task = new WorkflowTask();
        task.setType("SIMPLE");
        task.setName(OrderStatusUpdaterWorker.TASK_DEF_NAME);
        task.setTaskReferenceName(taskReference);
        task.setDescription("Change Order status to " + status);
        task.setInputParameters(Map.of("orderId", "${workflow.input.orderId}", "newStatus", status));
        return task;
    }

    @Test
    void registerTasks() {
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

    void registerTask(TaskDef taskDefinition) {
        TaskDef taskDefinition2 = null;
        try {
            taskDefinition2 = metadataClient.getTaskDef(taskDefinition.getName());
        } catch (ConductorClientException ex) {
            // check it really doesn't exists.
            Assertions.assertEquals(404, ex.getStatus());
        }

        if (taskDefinition2 != null) {
            metadataClient.unregisterTaskDef(taskDefinition.getName());
        }

        metadataClient.registerTaskDefs(List.of(taskDefinition));
    }

}
