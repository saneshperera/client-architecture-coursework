package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global catch-all exception mapper.
 *
 * Intercepts any uncaught Throwable and returns a generic HTTP 500.
 * The raw stack trace is NEVER exposed in the response — only a safe, generic message.
 *
 * CYBERSECURITY RATIONALE:
 * Exposing Java stack traces reveals:
 *   1. Internal class/package structure — helps attackers map the codebase.
 *   2. Library versions (e.g. Jersey 2.x, Jackson) — enabling version-specific exploit targeting.
 *   3. Server file paths — useful for path traversal or file inclusion attacks.
 *   4. Business logic flow — attackers learn exactly where an error occurred.
 * A generic 500 response denies all of this intelligence while still signalling failure.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full details server-side for debugging — never send to client
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", exception);

        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact support if this persists.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}