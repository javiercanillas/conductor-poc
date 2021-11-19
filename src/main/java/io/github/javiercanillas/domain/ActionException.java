package io.github.javiercanillas.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ActionException extends RuntimeException {

    private final String code;
    private Map<String, Object> outputData = new HashMap<>();

    public ActionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ActionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ActionException withOutputData(String key, Object value) {
        this.outputData.put(key, value);
        return this;
    }
}
