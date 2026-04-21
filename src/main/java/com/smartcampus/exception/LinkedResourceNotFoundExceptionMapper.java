package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException → HTTP 422 Unprocessable Entity.
 *
 * HTTP 422 is chosen over 404 because:
 * The request URL was valid — the payload JSON was structurally correct —
 * but a referenced entity inside the body (e.g. roomId) points to a resource
 * that doesn't exist. The problem is in the *content* of the request, not its structure.
 * 404 would imply the endpoint itself was not found, which is misleading.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("message", exception.getMessage());
        body.put("hint", "Ensure the roomId in your request body refers to an existing room.");

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}