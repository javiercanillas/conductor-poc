package io.github.javiercanillas.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.github.javiercanillas.domain.ActionException;
import io.github.javiercanillas.domain.FraudOrderChecker;
import io.github.javiercanillas.domain.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FraudOrderEvaluatorWorker implements Worker {

    public static final String TASK_DEF_NAME = "fraud-order-evaluator";
    private final FraudOrderChecker checker;

    public FraudOrderEvaluatorWorker(FraudOrderChecker checker) {
        this.checker = checker;
    }

    @Override
    public String getTaskDefName() {
        return TASK_DEF_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult taskResult;
        try {
            final var result = this.checker.execute(task.getTaskId(), task.getInputData());
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
