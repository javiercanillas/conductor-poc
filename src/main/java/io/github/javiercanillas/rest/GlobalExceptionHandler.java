package io.github.javiercanillas.rest;

import io.github.javiercanillas.domain.ActionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ActionException.class)
    @ResponseBody
    public final Error handleException(final HttpServletRequest request,
                                       final HttpServletResponse response, final ActionException ex) {
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        return Error.builder()
                .message(ex.getMessage())
                .outputData(ex.getOutputData())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public final Error handleException(final HttpServletRequest request,
                                       final HttpServletResponse response, final Exception ex) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Error.builder()
                .message(ex.getMessage())
                .code("0")
                .build();
    }
}
