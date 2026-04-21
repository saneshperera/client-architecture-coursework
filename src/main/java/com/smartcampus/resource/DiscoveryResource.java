package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Root discovery endpoint.
 * Returns API metadata and hypermedia links to primary resource collections (HATEOAS).
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> response = new HashMap<>();

        // API versioning info
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("status", "operational");

        // Administrative contact
        Map<String, String> contact = new HashMap<>();
        contact.put("team", "Campus Infrastructure Team");
        contact.put("email", "smartcampus@university.ac.uk");
        response.put("contact", contact);

        // HATEOAS-style links map — tells clients where resources live
        Map<String, String> links = new HashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        links.put("self",    "/api/v1");
        response.put("_links", links);

        return Response.ok(response).build();
    }
}