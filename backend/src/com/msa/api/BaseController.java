package com.msa.api;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseController {

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
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("Request body too large");
        }
        return new String(bytes, StandardCharsets.UTF_8);
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
        String patternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9.]+)|true|false)";
        Pattern pattern = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
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
