package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for sensor readings.
 * Handles paths like /api/v1/sensors/{sensorId}/readings
 *
 * This class is NOT annotated with @Path at the class level —
 * it is instantiated by SensorResource's sub-resource locator.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ── GET /api/v1/sensors/{sensorId}/readings ──────────────────────────────────
    /**
     * Returns the full historical log of readings for this sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> history = store.getReadings(sensorId);
        return Response.ok(history).build();
    }

    // ── POST /api/v1/sensors/{sensorId}/readings ─────────────────────────────────
    /**
     * Records a new reading for this sensor.
     *
     * STATE CONSTRAINT: Sensors in MAINTENANCE status cannot accept new readings —
     * they are physically disconnected. A SensorUnavailableException (→ 403) is thrown.
     *
     * SIDE EFFECT: A successful POST updates the parent sensor's currentValue field
     * to keep data consistent across the API.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // Block if sensor is in MAINTENANCE
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot accept readings."
            );
        }
        // Also block OFFLINE sensors
        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is OFFLINE and cannot accept readings."
            );
        }

        // Auto-generate ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.addReading(sensorId, reading);

        // ── Side effect: update parent sensor's currentValue ──────────────────
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}