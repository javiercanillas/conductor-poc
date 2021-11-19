package io.github.javiercanillas.domain;

import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.github.javiercanillas.workers.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class ProductOrderDelivery implements Action {
    
    private final Random rnd;

    public ProductOrderDelivery() {
        this.rnd = new Random();
    }
    
    @Override
    public Map<String, Object> execute(String id, Map<String, Object> input) {
        log.trace("Running product order delivery for {}", id);
        Utils.sleepSilently(100L + rnd.nextInt(2000));
        if (rnd.nextBoolean()) {
            log.trace("Successful product delivery for {}", id);
            return Map.of("product-nsu", UUID.randomUUID().toString());
        } else {
            log.trace("Ups! Product delivery failure for {}", id);
            throw new ActionException("0001", "This product cannot be delivered");
        }
    }
}
