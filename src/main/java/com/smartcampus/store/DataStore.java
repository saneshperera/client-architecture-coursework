package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 *
 * Uses ConcurrentHashMap to ensure thread-safety across concurrent HTTP requests.
 * JAX-RS by default creates a new resource class instance per request, so all
 * mutable state must live here — outside resource classes — to persist between calls.
 *
 * ConcurrentHashMap is used instead of plain HashMap to prevent race conditions
 * when multiple requests attempt to read/write simultaneously.
 */
public class DataStore {

    // ── Singleton setup ──────────────────────────────────────────────────────────
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ── Storage structures ───────────────────────────────────────────────────────
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Readings are keyed by sensorId -> list of readings for that sensor
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // ── Constructor with seed data ───────────────────────────────────────────────
    private DataStore() {
        seedData();
    }

    private void seedData() {
        // Seed rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab A", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Seed sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE", 412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 0.0, "LAB-101");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to rooms
        r1.addSensorId(s1.getId());
        r1.addSensorId(s2.getId());
        r2.addSensorId(s3.getId());

        // Seed initial readings
        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001",  new ArrayList<>());
        readings.put("OCC-001",  new ArrayList<>());

        readings.get("TEMP-001").add(new SensorReading("READ-001", System.currentTimeMillis() - 60000, 21.5));
        readings.get("CO2-001").add(new SensorReading("READ-002",  System.currentTimeMillis() - 30000, 412.0));
    }

    // ── Room accessors ───────────────────────────────────────────────────────────
    public Map<String, Room> getRooms() { return rooms; }
    public Room getRoom(String id) { return rooms.get(id); }
    public void putRoom(Room room) { rooms.put(room.getId(), room); }
    public boolean deleteRoom(String id) { return rooms.remove(id) != null; }

    // ── Sensor accessors ─────────────────────────────────────────────────────────
    public Map<String, Sensor> getSensors() { return sensors; }
    public Sensor getSensor(String id) { return sensors.get(id); }
    public void putSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }
    public boolean deleteSensor(String id) {
        readings.remove(id);
        return sensors.remove(id) != null;
    }

    // ── Reading accessors ────────────────────────────────────────────────────────
    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}