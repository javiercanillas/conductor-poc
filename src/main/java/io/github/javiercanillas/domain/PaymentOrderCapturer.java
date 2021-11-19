package io.github.javiercanillas.domain;

import io.github.javiercanillas.workers.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class PaymentOrderCapturer implements Action {

    private final Random rnd;

    public PaymentOrderCapturer() {
        this.rnd = new Random();
    }

    @Override
    public Map<String, Object> execute(String id, Map<String, Object> input) {
        log.trace("Running payment order capture for {}", id);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are captured", id);
            return Map.of("payment-authorizationId", UUID.randomUUID().toString());
        } else {
            log.trace("Ups! Payment capture error for {}", id);
            throw new ActionException("0001", "Payment capture error!")
                    .withOutputData("payment-transactionId", UUID.randomUUID().toString());
        }
    }
}
