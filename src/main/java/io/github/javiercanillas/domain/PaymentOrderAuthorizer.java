package io.github.javiercanillas.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class PaymentOrderAuthorizer implements Action {

    private final Random rnd;

    public PaymentOrderAuthorizer() {
        this.rnd = new Random();
    }

    @Override
    public Map<String, Object> execute(String id, Map<String, Object> input) {
        log.trace("Running payment order authorization for {}", id);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("All payment intents for {} are authorized", id);
            return Map.of("payment-authorizationId", UUID.randomUUID().toString());
        } else {
            log.trace("Ups! Payment declined for {}", id);
            throw new ActionException("0001", "Payment declined! Insufficient funds")
                    .withOutputData("payment-transactionId", UUID.randomUUID().toString());
        }
    }
}
