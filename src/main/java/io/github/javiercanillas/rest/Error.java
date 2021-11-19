package io.github.javiercanillas.rest;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class Error {
    private String code;
    private String message;
    private Map<String,Object> outputData;
}
