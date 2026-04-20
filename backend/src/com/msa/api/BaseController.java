package com.msa.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected static void addCorsHeaders(HttpExchange exchange, String allowedMethods) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", allowedMethods + ", OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-User-Role");
    }

    protected static boolean isPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    protected static String readRequestBody(HttpExchange exchange, int maxBytes) throws IOException {
        try (InputStream in = exchange.getRequestBody();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("Request body too large");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    protected static void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    protected static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    protected static String extractJsonValueFromBody(String body, String key) {
        if (body == null || body.isBlank() || key == null || key.isBlank()) {
            return null;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(body);
            JsonNode valueNode = node.get(key);
            if (valueNode == null || valueNode.isNull()) {
                return null;
            }
            if (valueNode.isTextual()) {
                return valueNode.asText();
            }
            if (valueNode.isNumber() || valueNode.isBoolean()) {
                return valueNode.asText();
            }
        } catch (IOException ignored) {
            // Role extraction should fail closed; callers handle missing values.
        }

        return null;
    }

    protected static JsonNode parseJson(String body) throws IOException {
        return OBJECT_MAPPER.readTree(body);
    }

    protected static <T> T parseJson(String body, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(body, clazz);
    }

    protected static void writeJsonObject(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        try {
            writeJson(exchange, statusCode, OBJECT_MAPPER.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            writeJson(exchange, 500, "{\"error\":\"Failed to serialize response\"}");
        }
    }

    protected static String getRole(HttpExchange exchange, String requestBody) {
        String roleHeader = exchange.getRequestHeaders().getFirst("X-User-Role");
        if (roleHeader != null && !roleHeader.trim().isEmpty()) {
            return roleHeader.trim().toLowerCase();
        }
        if (requestBody == null || requestBody.isBlank()) {
            return null;
        }
        String role = extractJsonValueFromBody(requestBody, "role");
        return role == null ? null : role.trim().toLowerCase();
    }

    protected static boolean hasRole(HttpExchange exchange, String requestBody, String role) {
        String actual = getRole(exchange, requestBody);
        return actual != null && actual.equalsIgnoreCase(role);
    }

    protected static boolean hasAnyRole(HttpExchange exchange, String requestBody, String... roles) {
        String actual = getRole(exchange, requestBody);
        if (actual == null) {
            return false;
        }
        for (String role : roles) {
            if (actual.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean requireRole(HttpExchange exchange, String requestBody, String role) throws IOException {
        if (hasRole(exchange, requestBody, role)) {
            return true;
        }
        writeJson(exchange, 403, "{\"error\":\"Forbidden: " + escapeJson(role) + " role required\"}");
        return false;
    }

    protected static boolean requireAnyRole(HttpExchange exchange, String requestBody, String... roles)
            throws IOException {
        if (hasAnyRole(exchange, requestBody, roles)) {
            return true;
        }
        writeJson(exchange, 403, "{\"error\":\"Forbidden: insufficient permissions\"}");
        return false;
    }
}
