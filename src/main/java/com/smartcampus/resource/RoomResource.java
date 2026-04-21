package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Room Resource — manages /api/v1/rooms
 *
 * Provides full CRUD operations for campus rooms with a safety constraint:
 * a room cannot be deleted while sensors are still assigned to it.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /api/v1/rooms ────────────────────────────────────────────────────────
    /**
     * Returns a list of all rooms in the system.
     * Full room objects are returned so clients have all metadata in one call,
     * avoiding a second round-trip to fetch details.
     */
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    // ── POST /api/v1/rooms ───────────────────────────────────────────────────────
    /**
     * Creates a new room.
     * Returns 201 Created with the new room in the body.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room ID must not be blank."))
                    .build();
        }
        if (store.getRoom(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        store.putRoom(room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // ── GET /api/v1/rooms/{roomId} ───────────────────────────────────────────────
    /**
     * Returns detailed metadata for a specific room by its ID.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /api/v1/rooms/{roomId} ────────────────────────────────────────────
    /**
     * Decommissions a room.
     *
     * SAFETY CONSTRAINT: If the room still has sensors assigned, deletion is blocked
     * and a RoomNotEmptyException is thrown (mapped to 409 Conflict).
     *
     * IDEMPOTENCY: DELETE is idempotent — calling it multiple times produces the same
     * final state (the room does not exist). However, only the first call returns 204;
     * subsequent calls return 404 since the resource is already gone.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        // Safety guard: block deletion if sensors are still present
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted — it still has " +
                room.getSensorIds().size() + " active sensor(s) assigned."
            );
        }
        store.deleteRoom(roomId);
        return Response.noContent().build(); // 204 No Content
    }

    // ── Helper ───────────────────────────────────────────────────────────────────
    private java.util.Map<String, String> errorBody(String message) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("error", message);
        return body;
    }
}