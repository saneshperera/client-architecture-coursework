package com.smartcampus.exception;

/**
 * Thrown when a client attempts to delete a Room that still has Sensors assigned.
 * Maps to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}