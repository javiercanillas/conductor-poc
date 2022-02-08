package io.github.javiercanillas.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class FraudOrderChecker implements Action {

    private final Random rnd;

    public FraudOrderChecker() {
        this.rnd = new Random();
    }

    @Override
    public Map<String, Object> execute(String id, Map<String, Object> input) {
        log.trace("Running fraud checks for {}", id);
        Utils.sleepSilently(100L + rnd.nextInt(2000));

        if (rnd.nextBoolean()) {
            log.trace("Nothing wrong found on fraud checks for {}", id);
            return Map.of("score", Integer.toString(1 + rnd.nextInt(99)));
        } else {
            log.trace("Ups! Something wrong found on fraud checks for {}", id);
            throw new ActionException("0001", "Didn't pass fraud checks!")
                    .withOutputData("score", "0");
        }
    }
}
