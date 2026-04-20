package com.msa.api;

import com.msa.model.AppUser;
import com.msa.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
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
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            AppUser user = users.get(i);
            json.append("{")
                    .append("\"userId\":").append(user.getUserId()).append(",")
                    .append("\"username\":\"").append(escapeJson(user.getUsername())).append("\",")
                    .append("\"role\":\"").append(escapeJson(user.getRole())).append("\"")
                    .append("}");
            if (i < users.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");

        writeJson(exchange, 200, json.toString());
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        String username = extractJsonString(body, "username");
        String password = extractJsonString(body, "password");
        String role = extractJsonString(body, "role");

        if (isBlank(username) || isBlank(password) || isBlank(role)) {
            writeJson(exchange, 400, "{\"error\":\"username, password, and role are required\"}");
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(password);
        user.setRole(role.trim().toLowerCase());

        boolean created = authService.createUser(user);
        if (!created) {
            writeJson(exchange, 400, "{\"error\":\"Failed to create user\"}");
            return;
        }

        writeJson(exchange, 201,
                "{\"message\":\"User created\",\"userId\":" + user.getUserId() + "}");
    }

    private void handleUpdate(HttpExchange exchange, int userId) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        String username = extractJsonString(body, "username");
        String password = extractJsonString(body, "password");
        String role = extractJsonString(body, "role");

        if (isBlank(username) || isBlank(password) || isBlank(role)) {
            writeJson(exchange, 400, "{\"error\":\"username, password, and role are required\"}");
            return;
        }

        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(password);
        user.setRole(role.trim().toLowerCase());

        boolean updated = authService.updateUser(user);
        if (!updated) {
            writeJson(exchange, 404, "{\"error\":\"User not found\"}");
            return;
        }

        writeJson(exchange, 200, "{\"message\":\"User updated\"}");
    }

    private void handleDelete(HttpExchange exchange, int userId) throws IOException {
        if (!requireRole(exchange, "", "admin")) {
            return;
        }

        boolean deleted = authService.deleteUser(userId);
        if (!deleted) {
            writeJson(exchange, 404, "{\"error\":\"User not found\"}");
            return;
        }

        writeJson(exchange, 200, "{\"message\":\"User deleted\"}");
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

    private static String extractJsonString(String body, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
