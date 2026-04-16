package com.msa.api;

import com.msa.model.AppUser;
import com.msa.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 4096;

    private final AuthService authService;

    public UserController() {
        this.authService = new AuthService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/users/cashiers".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleListCashiers(exchange, exchange.getRequestURI().getQuery());
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);

            if ("/api/users/login".equals(path)) {
                handleLogin(exchange, body);
                return;
            }

            if ("/api/users/admin/change-credentials".equals(path)) {
                handleChangeAdminCredentials(exchange, body);
                return;
            }

            if ("/api/users/admin/add-cashier".equals(path)) {
                handleAddCashier(exchange, body);
                return;
            }

            if ("/api/users/admin/delete-cashier".equals(path)) {
                handleDeleteCashier(exchange, body);
                return;
            }

            if ("/api/users/admin/update-cashier".equals(path)) {
                handleUpdateCashier(exchange, body);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown user endpoint\"}");

        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private static String extractJsonValue(String body, String key) {
        String patternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
        Pattern pattern = Pattern.compile(patternText);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void handleLogin(HttpExchange exchange, String body) throws IOException {
        String username = extractJsonValue(body, "username");
        String password = extractJsonValue(body, "password");
        String role = extractJsonValue(body, "role");

        if (isBlank(username) || isBlank(password)) {
            writeJson(exchange, 400, "{\"error\":\"username and password are required\"}");
            return;
        }

        AppUser user = authService.login(username.trim().toLowerCase(), password, role);
        if (user == null) {
            writeJson(exchange, 401, "{\"error\":\"Invalid credentials for the selected role.\"}");
            return;
        }

        String json = "{"
                + "\"username\":\"" + escapeJson(user.getUsername()) + "\","
                + "\"role\":\"" + escapeJson(user.getRole()) + "\","
                + "\"displayName\":\"" + escapeJson(user.getUsername()) + "\""
                + "}";
        writeJson(exchange, 200, json);
    }

    private void handleChangeAdminCredentials(HttpExchange exchange, String body) throws IOException {
        String adminUsername = extractJsonValue(body, "adminUsername");
        String adminPassword = extractJsonValue(body, "adminPassword");
        String newUsername = extractJsonValue(body, "newUsername");
        String newPassword = extractJsonValue(body, "newPassword");

        if (isBlank(adminUsername) || isBlank(adminPassword) || isBlank(newUsername) || isBlank(newPassword)) {
            writeJson(exchange, 400,
                    "{\"error\":\"adminUsername, adminPassword, newUsername and newPassword are required\"}");
            return;
        }

        boolean updated = authService.updateAdminCredentials(
                adminUsername.trim().toLowerCase(),
                adminPassword,
                newUsername.trim().toLowerCase(),
                newPassword);

        if (!updated) {
            writeJson(exchange, 401, "{\"error\":\"Invalid admin credentials or the new username already exists\"}");
            return;
        }

        writeJson(exchange, 200, "{\"message\":\"Admin credentials updated successfully\"}");
    }

    private void handleAddCashier(HttpExchange exchange, String body) throws IOException {
        String adminUsername = extractJsonValue(body, "adminUsername");
        String adminPassword = extractJsonValue(body, "adminPassword");
        String cashierUsername = extractJsonValue(body, "cashierUsername");
        String cashierPassword = extractJsonValue(body, "cashierPassword");

        if (isBlank(adminUsername) || isBlank(adminPassword) || isBlank(cashierUsername) || isBlank(cashierPassword)) {
            writeJson(exchange, 400,
                    "{\"error\":\"adminUsername, adminPassword, cashierUsername and cashierPassword are required\"}");
            return;
        }

        boolean added = authService.addCashier(
                adminUsername.trim().toLowerCase(),
                adminPassword,
                cashierUsername.trim().toLowerCase(),
                cashierPassword);

        if (!added) {
            writeJson(exchange, 409, "{\"error\":\"Invalid admin credentials or cashier username already exists\"}");
            return;
        }

        writeJson(exchange, 201, "{\"message\":\"Cashier created successfully\"}");
    }

    private void handleDeleteCashier(HttpExchange exchange, String body) throws IOException {
        String adminUsername = extractJsonValue(body, "adminUsername");
        String adminPassword = extractJsonValue(body, "adminPassword");
        String cashierUsername = extractJsonValue(body, "cashierUsername");

        if (isBlank(adminUsername) || isBlank(adminPassword) || isBlank(cashierUsername)) {
            writeJson(exchange, 400, "{\"error\":\"adminUsername, adminPassword and cashierUsername are required\"}");
            return;
        }

        boolean deleted = authService.deleteCashier(
                adminUsername.trim().toLowerCase(),
                adminPassword,
                cashierUsername.trim().toLowerCase());

        if (!deleted) {
            writeJson(exchange, 404, "{\"error\":\"Invalid admin credentials or cashier not found\"}");
            return;
        }

        writeJson(exchange, 200, "{\"message\":\"Cashier deleted successfully\"}");
    }

    private void handleUpdateCashier(HttpExchange exchange, String body) throws IOException {
        String adminUsername = extractJsonValue(body, "adminUsername");
        String adminPassword = extractJsonValue(body, "adminPassword");
        String currentUsername = extractJsonValue(body, "currentUsername");
        String newUsername = extractJsonValue(body, "newUsername");
        String newPassword = extractJsonValue(body, "newPassword");

        if (isBlank(adminUsername) || isBlank(adminPassword) || isBlank(currentUsername)
                || isBlank(newUsername) || isBlank(newPassword)) {
            writeJson(exchange, 400,
                    "{\"error\":\"adminUsername, adminPassword, currentUsername, newUsername and newPassword are required\"}");
            return;
        }

        boolean updated = authService.updateCashierCredentials(
                adminUsername.trim().toLowerCase(),
                adminPassword,
                currentUsername.trim().toLowerCase(),
                newUsername.trim().toLowerCase(),
                newPassword);

        if (!updated) {
            writeJson(exchange, 404,
                    "{\"error\":\"Invalid admin credentials, cashier not found, or new username already exists\"}");
            return;
        }

        writeJson(exchange, 200, "{\"message\":\"Cashier credentials updated successfully\"}");
    }

    private void handleListCashiers(HttpExchange exchange, String query) throws IOException {
        String adminUsername = getQueryParam(query, "adminUsername");
        String adminPassword = getQueryParam(query, "adminPassword");

        if (isBlank(adminUsername) || isBlank(adminPassword)) {
            writeJson(exchange, 400, "{\"error\":\"adminUsername and adminPassword are required\"}");
            return;
        }

        List<AppUser> cashiers = authService.listCashiers(
                adminUsername.trim().toLowerCase(),
                adminPassword);

        if (cashiers == null) {
            writeJson(exchange, 401, "{\"error\":\"Invalid admin credentials\"}");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cashiers.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            AppUser cashier = cashiers.get(i);
            builder.append('{')
                    .append("\"userId\":").append(cashier.getUserId()).append(',')
                    .append("\"username\":\"").append(escapeJson(cashier.getUsername())).append("\",")
                    .append("\"role\":\"").append(escapeJson(cashier.getRole())).append("\"")
                    .append('}');
        }

        writeJson(exchange, 200, "[" + builder + "]");
    }

    private static String getQueryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }

        return null;
    }

}
