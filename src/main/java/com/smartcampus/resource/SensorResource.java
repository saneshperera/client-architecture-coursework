package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sensor Resource — manages /api/v1/sensors
 *
 * Supports:
 *   GET    /api/v1/sensors           — list all (with optional ?type= filter)
 *   POST   /api/v1/sensors           — register a new sensor
 *   GET    /api/v1/sensors/{id}      — fetch one sensor
 *   DELETE /api/v1/sensors/{id}      — remove a sensor
 *   ANY    /api/v1/sensors/{id}/readings  — sub-resource locator
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /api/v1/sensors  (optional ?type=CO2) ────────────────────────────────
    /**
     * Returns all sensors, optionally filtered by type via a query parameter.
     *
     * Using @QueryParam is preferred over embedding type in the path
     * (e.g. /sensors/type/CO2) because query parameters are semantically
     * meant for filtering/searching collections rather than identifying resources.
     * It also allows combining multiple filters in future (e.g. ?type=CO2&status=ACTIVE).
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    // ── POST /api/v1/sensors ─────────────────────────────────────────────────────
    /**
     * Registers a new sensor.
     *
     * Validates that the referenced roomId actually exists.
     * If it does not, a LinkedResourceNotFoundException is thrown → 422.
     *
     * The @Consumes(APPLICATION_JSON) annotation means JAX-RS will reject
     * requests with a Content-Type other than application/json with 415 Unsupported Media Type.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor ID must not be blank."))
                    .build();
        }
        if (store.getSensor(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }
        // Validate the referenced room exists
        if (sensor.getRoomId() == null || store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException(
                "Room '" + sensor.getRoomId() + "' does not exist. " +
                "Sensor cannot be assigned to a non-existent room."
            );
        }
        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.putSensor(sensor);
        // Link sensor back to its room
        store.getRoom(sensor.getRoomId()).addSensorId(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ── GET /api/v1/sensors/{sensorId} ───────────────────────────────────────────
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ── DELETE /api/v1/sensors/{sensorId} ────────────────────────────────────────
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        // Remove sensor from its parent room's list
        if (sensor.getRoomId() != null && store.getRoom(sensor.getRoomId()) != null) {
            store.getRoom(sensor.getRoomId()).removeSensorId(sensorId);
        }
        store.deleteSensor(sensorId);
        return Response.noContent().build();
    }

    // ── Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings ────────────────
    /**
     * Sub-resource locator pattern.
     * JAX-RS delegates all requests under /{sensorId}/readings to SensorReadingResource.
     * This method is NOT annotated with @GET/@POST — JAX-RS uses it purely for routing.
     *
     * Benefits: keeps each class focused, supports dependency injection per sub-resource,
     * and avoids a single bloated controller handling every nested path.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
    }

    // ── Helper ───────────────────────────────────────────────────────────────────
    private Map<String, String> errorBody(String message) {
        return java.util.Collections.singletonMap("error", message);
    }
}