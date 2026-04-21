package com.smartcampus.exception;

/**
 * Thrown when a referenced resource (e.g. roomId inside a Sensor payload) does not exist.
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * 422 is more semantically accurate than 404 here because:
 *   - 404 signals that the *requested* URL resource was not found.
 *   - 422 signals that the request was syntactically valid JSON but contains a
 *     logically invalid reference — the roomId inside the body points to nothing.
 * This distinction helps clients understand whether to fix the URL or the payload body.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}