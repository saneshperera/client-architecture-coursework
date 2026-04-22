# Smart Campus — Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures  
**Student:** [Your Name]  
**Student ID:** [Your Student ID]  
**Year:** 2025/26

---

## Overview

For this coursework I built a REST API for a Smart Campus system using JAX-RS (Jersey 3.1.3) in Java, running on Apache Tomcat 11. The API lets you manage university rooms and the sensors inside them. You can create rooms, register sensors, record readings, and the API handles errors with proper HTTP status codes. All data is stored in memory using HashMaps no database is used.

---

## Project Structure

```
src/main/java/com/smartcampus/
- ApplicationConfig.java          (API entry point)
- model/
  - Room.java
  - Sensor.java
  - SensorReading.java
- store/
  - DataStore.java                (stores all data in memory)
- resource/
  - DiscoveryResource.java        (GET /api/v1)
  - RoomResource.java             (/api/v1/rooms)
  - SensorResource.java           (/api/v1/sensors)
  - SensorReadingResource.java    (/api/v1/sensors/{id}/readings)
- exception/
  - RoomNotEmptyException.java
  - RoomNotEmptyExceptionMapper.java
  - LinkedResourceNotFoundException.java
  - LinkedResourceNotFoundExceptionMapper.java
  - SensorUnavailableException.java
  - SensorUnavailableExceptionMapper.java
  - GlobalExceptionMapper.java
- filter/
  - LoggingFilter.java
```

---

## How to Build and Run

### Requirements
- Java JDK 17
- Apache Maven 3.6+
- NetBeans 18 with Apache Tomcat 11

### Steps

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
```

2. Open NetBeans - File - Open Project - select the cloned folder

3. Right-click the project - Clean and Build

4. Right-click the project - Run

5. The API will be live at:
```
http://localhost:8080/smart-campus-api/api/v1/
```

The app starts with pre-loaded data so you can test straight away:
- Rooms: LIB-301 (Library), LAB-101 (Computer Lab)
- Sensors: TEMP-001 (Active), CO2-001 (Active), OCC-001 (Maintenance)

---

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/
```
Expected: 200 OK

### 2. Get all rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```
Expected: 200 OK

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LAB-202","name":"AI Research Lab","capacity":20}'
```
Expected: 201 Created

### 4. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
```
Expected: 200 OK with only CO2 sensors

### 5. Post a sensor reading
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```
Expected: 201 Created

### 6. Delete a room that still has sensors
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```
Expected: 409 Conflict

### 7. Post a reading to a MAINTENANCE sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15.0}'
```
Expected: 403 Forbidden

---

## Report: Answers to Coursework Questions

---

### Part 1 — Service Architecture & Setup

#### Question 1.1 JAX-RS Resource Lifecycle and Thread Safety

By default JAX-RS creates a brand new copy of each resource class for every request that comes in, and throws it away once the response is sent. This means you cannot store data inside a resource class because it would disappear after every request.

To solve this I created a DataStore singleton a single object that gets created once when the server starts and stays alive the whole time. All rooms and sensors are stored inside it so the data persists between requests.

I used ConcurrentHashMap instead of a regular HashMap because the server handles multiple requests at the same time on different threads. A regular HashMap is not safe for this and can corrupt data if two requests write to it simultaneously. ConcurrentHashMap handles this safely without any extra code.

---

#### Question 1.2 HATEOAS and Hypermedia

HATEOAS means the API includes links in its responses telling the client where it can go next similar to how a website has clickable links instead of making you memorise every URL.

My discovery endpoint returns a links section showing the URLs for rooms and sensors. This means a developer can explore the whole API just by following links from the starting point, without needing to read separate documentation. If the server ever changes a URL, the client just follows the new link rather than breaking.

---

### Part 2 Room Management

#### Question 2.1 Full Objects vs IDs Only

If the API only returned a list of room IDs, the client would have to make a separate request for every single ID to get the actual details. With 100 rooms that is 101 requests very slow.

Returning the full room objects in one response means the client gets everything it needs in a single call. I chose this approach because room data is small and a facilities dashboard would need all the details straight away anyway.

---

#### Question 2.2 Is DELETE Idempotent?

Yes. Idempotent means doing the same thing multiple times gives the same result as doing it once.

If you delete a room it gets removed and returns 204. If you try to delete the same room again it is already gone so it returns 404. Either way the end result is the same the room does not exist. No extra damage is done by accidentally sending the request twice.

---

### Part 3 Sensor Operations & Linking

#### Question 3.1 Wrong Content-Type with @Consumes

The @Consumes(APPLICATION_JSON) annotation tells Jersey to only accept requests with Content-Type: application/json. If a client sends the wrong format like text/plain, Jersey automatically rejects it and returns 415 Unsupported Media Type before my code even runs. This keeps the resource method clean and gives the client a clear error explaining what went wrong.

---

#### Question 3.2 @QueryParam vs Path for Filtering

A URL path is meant to point to a specific resource. Putting a filter in the path like /sensors/type/CO2 looks like there is a resource called "type/CO2" which is confusing and wrong.

Query parameters like ?type=CO2 are the standard way to filter or search a collection everyone understands what they mean. They are also easy to combine, for example ?type=CO2&status=ACTIVE, whereas doing that with path segments would get messy quickly.

---

### Part 4 Deep Nesting with Sub-Resources

#### Question 4.1 Sub-Resource Locator Pattern

Instead of putting all the readings logic inside SensorResource, I created a separate SensorReadingResource class just for readings. When a request comes in for /sensors/{id}/readings, SensorResource simply hands it off to that dedicated class.

This keeps each class focused on one job and makes the code much easier to navigate. If everything was in one class it would become hundreds of lines long and hard to maintain. It also makes testing easier because each class can be tested on its own.

---

### Part 5 Error Handling, Exception Mapping & Logging

#### Question 5.2 Why 422 Instead of 404

404 means the URL was not found. But when someone posts a sensor with a roomId that does not exist, the URL itself is perfectly fine the problem is inside the request body.

422 means the request was understood but the data inside it does not make sense. This tells the client exactly what to fix not the URL, but the roomId value in their JSON body.

---

#### Question 5.4 Security Risks of Exposing Stack Traces

A Java stack trace reveals a lot of sensitive information the internal structure of the code, which libraries are being used and their versions, and file paths on the server. Attackers can use this to find known vulnerabilities in specific library versions or map out the codebase to plan an attack.

My GlobalExceptionMapper catches every error and returns a simple generic 500 message to the client. The full details are only logged on the server side where only developers can see them.

---

#### Question 5.5 Filters vs Manual Logging

If I added a logging line manually inside every resource method I would have to remember to do it every time I create a new endpoint. If I forgot one, that endpoint would have no logging at all.

A JAX-RS filter is written once and automatically applies to every single request and response across the whole API. One place to write it, one place to change it, and it can never be accidentally missed on a new endpoint.
