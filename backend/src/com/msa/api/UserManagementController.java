package com.msa.api;

import com.msa.model.AppUser;
import com.msa.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserManagementController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 8192;

    private final AuthService authService;

    public UserManagementController() {
        this.authService = new AuthService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST, PUT, DELETE");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equalsIgnoreCase(method) && isCollectionPath(path)) {
                handleGetAll(exchange);
            } else if ("POST".equalsIgnoreCase(method) && isCollectionPath(path)) {
                handleCreate(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                Integer userId = extractUserId(path);
                if (userId == null) {
                    writeJson(exchange, 400, "{\"error\":\"Missing or invalid user id\"}");
                    return;
                }
                handleUpdate(exchange, userId);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                Integer userId = extractUserId(path);
                if (userId == null) {
                    writeJson(exchange, 400, "{\"error\":\"Missing or invalid user id\"}");
                    return;
                }
                handleDelete(exchange, userId);
            } else {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (RuntimeException e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        if (!requireRole(exchange, "", "admin")) {
            return;
        }

        List<AppUser> users = authService.getAllUsers();
        List<Map<String, Object>> response = new java.util.ArrayList<>();
        for (AppUser user : users) {
            response.add(Map.of(
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "role", user.getRole()));
        }

        writeJsonObject(exchange, 200, response);
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        var node = parseJson(body);
        String username = node.path("username").asText(null);
        String password = node.path("password").asText(null);
        String role = node.path("role").asText(null);

        if (isBlank(username) || isBlank(password) || isBlank(role)) {
            writeJsonObject(exchange, 400, Map.of("error", "username, password, and role are required"));
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(password);
        user.setRole(role.trim().toLowerCase());

        boolean created = authService.createUser(user);
        if (!created) {
            writeJsonObject(exchange, 400, Map.of("error", "Failed to create user"));
            return;
        }

        writeJsonObject(exchange, 201, Map.of(
                "message", "User created",
                "userId", user.getUserId()));
    }

    private void handleUpdate(HttpExchange exchange, int userId) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        var node = parseJson(body);
        String username = node.path("username").asText(null);
        String password = node.path("password").asText(null);
        String role = node.path("role").asText(null);

        if (isBlank(username) || isBlank(password) || isBlank(role)) {
            writeJsonObject(exchange, 400, Map.of("error", "username, password, and role are required"));
            return;
        }

        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(password);
        user.setRole(role.trim().toLowerCase());

        boolean updated = authService.updateUser(user);
        if (!updated) {
            writeJsonObject(exchange, 404, Map.of("error", "User not found"));
            return;
        }

        writeJsonObject(exchange, 200, Map.of("message", "User updated"));
    }

    private void handleDelete(HttpExchange exchange, int userId) throws IOException {
        if (!requireRole(exchange, "", "admin")) {
            return;
        }

        boolean deleted = authService.deleteUser(userId);
        if (!deleted) {
            writeJsonObject(exchange, 404, Map.of("error", "User not found"));
            return;
        }

        writeJsonObject(exchange, 200, Map.of("message", "User deleted"));
    }

    private static boolean isCollectionPath(String path) {
        return "/api/users".equals(path) || "/api/users/".equals(path);
    }

    private static Integer extractUserId(String path) {
        Matcher matcher = Pattern.compile("^/api/users/([0-9]+)$").matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
