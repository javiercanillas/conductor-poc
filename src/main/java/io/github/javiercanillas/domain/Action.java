package io.github.javiercanillas.domain;

import java.util.Map;

public interface Action {

    Map<String,Object> execute(String id, Map<String,Object> input) throws ActionException;
}
