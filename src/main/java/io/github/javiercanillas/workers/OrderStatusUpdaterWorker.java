package io.github.javiercanillas.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class OrderStatusUpdaterWorker implements Worker {

    public static final String TASK_DEF_NAME = "order-status-updater";
    @Override
    public String getTaskDefName() {
        return TASK_DEF_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        final var inputData = task.getInputData();
        log.info("Changing order {} status to {}", inputData.get("orderId"),
                inputData.get("newStatus"));
        return TaskResult.complete();
    }
}
