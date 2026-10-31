package com.enset.incidents.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Format unique des erreurs : { "status": 404, "message": "Incident introuvable" } */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, Map<String, String> errors) {

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}
