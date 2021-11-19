package io.github.javiercanillas.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.github.javiercanillas.domain.ActionException;
import io.github.javiercanillas.domain.PaymentOrderAuthorizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentOrderAuthorizerWorker implements Worker {

    public static final String TASK_DEF_NAME = "payment-order-authorizer";
    private final PaymentOrderAuthorizer collector;

    public PaymentOrderAuthorizerWorker(PaymentOrderAuthorizer collector) {
        this.collector = collector;
    }

    @Override
    public String getTaskDefName() {
        return TASK_DEF_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult taskResult;
        try {
            final var result = this.collector.execute(task.getTaskId(), task.getInputData());
            taskResult = TaskResult.complete();
            result.forEach(taskResult::addOutputData);
            taskResult.addOutputData("result", "CONTINUE");
        } catch (ActionException e) {
            taskResult = TaskResult.complete();
            e.getOutputData().forEach(taskResult::addOutputData);
            taskResult.addOutputData("result", "BREAK");
        } catch (RuntimeException e) {
            taskResult = TaskResult.failed().log(e.getMessage());
            taskResult.addOutputData("exception", Utils.printException(e));
        }
        return taskResult;
    }
}
