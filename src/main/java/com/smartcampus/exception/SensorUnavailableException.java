package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a Sensor that is in MAINTENANCE or OFFLINE status.
 * Maps to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}