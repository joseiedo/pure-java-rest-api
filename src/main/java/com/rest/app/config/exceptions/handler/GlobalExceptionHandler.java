package com.rest.app.config.exceptions.handler;

import com.rest.app.config.ObjectMapperWrapper;
import com.rest.app.config.exceptions.InvalidRequestException;
import com.rest.app.config.exceptions.MethodNotAllowedException;
import com.rest.app.config.exceptions.ResourceNotFoundException;
import com.rest.app.utils.Component;
import com.rest.app.utils.Constants;
import com.rest.app.utils.Inject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class GlobalExceptionHandler {

    @Inject
    public ObjectMapperWrapper objectMapperWrapper;

    public void handle(Throwable throwable, HttpExchange exchange) {
        try {
            throwable.printStackTrace();
            exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            ErrorResponse response = getErrorResponse(throwable, exchange);
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write(objectMapperWrapper.getObjectMapper().writeValueAsBytes(response));
            responseBody.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ErrorResponse getErrorResponse(Throwable throwable, HttpExchange exchange) throws IOException {
        ErrorResponse.ErrorResponseBuilder responseBuilder = ErrorResponse.builder();
        if (throwable instanceof InvalidRequestException) {
            InvalidRequestException exc = (InvalidRequestException) throwable;
            responseBuilder.message(exc.getMessage()).code(exc.getCode());
            exchange.sendResponseHeaders(400, 0);
        } else if (throwable instanceof ResourceNotFoundException) {
            ResourceNotFoundException exc = (ResourceNotFoundException) throwable;
            responseBuilder.message(exc.getMessage()).code(exc.getCode());
            exchange.sendResponseHeaders(404, 0);
        } else if (throwable instanceof MethodNotAllowedException) {
            MethodNotAllowedException exc = (MethodNotAllowedException) throwable;
            responseBuilder.message(exc.getMessage()).code(exc.getCode());
            exchange.sendResponseHeaders(405, 0);
        } else {
            responseBuilder.code(500).message(throwable.getMessage());
            exchange.sendResponseHeaders(500, 0);
        }
        return responseBuilder.build();
    }
}
