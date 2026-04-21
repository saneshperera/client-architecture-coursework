package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Observability Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter to log
 * every incoming request and every outgoing response in a single class.
 *
 * WHY FILTERS OVER MANUAL LOGGING:
 * Using filters for cross-cutting concerns (logging, auth, CORS) is preferable because:
 *   1. DRY principle — one place to change logging format, not dozens of resource methods.
 *   2. No risk of forgetting to add a log statement in a new resource.
 *   3. Separation of concerns — resource methods focus on business logic only.
 *   4. Reusable — the same filter applies to all endpoints automatically.
 *   5. Consistent format — all log lines follow the same pattern across the entire API.
 *
 * The @Provider annotation registers this filter with the JAX-RS runtime automatically.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Intercepts every incoming HTTP request.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
            "[REQUEST]  %s %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }

    /**
     * Intercepts every outgoing HTTP response.
     * Logs the final HTTP status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
            "[RESPONSE] %s %s → %d %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri(),
            responseContext.getStatus(),
            responseContext.getStatusInfo().getReasonPhrase()
        ));
    }
}