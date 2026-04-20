package com.msa.api;

import com.msa.model.AppUser;
import com.msa.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class UserController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 4096;

    private final AuthService authService;

    public UserController() {
        this.authService = new AuthService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "POST");

        if (isPreflight(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
            var node = parseJson(body);
            String username = node.path("username").asText(null);
            String password = node.path("password").asText(null);
            String role = node.path("role").asText(null);

            if (isBlank(username) || isBlank(password)) {
                writeJson(exchange, 400, "{\"error\":\"username and password are required\"}");
                return;
            }

            AppUser user = authService.login(username.trim().toLowerCase(), password, role);
            if (user == null) {
                writeJsonObject(exchange, 401, Map.of("error", "Invalid credentials for the selected role."));
                return;
            }

            writeJsonObject(exchange, 200, Map.of(
                    "username", user.getUsername(),
                    "role", user.getRole(),
                    "displayName", user.getUsername()));

        } catch (IllegalArgumentException e) {
            writeJsonObject(exchange, 413, Map.of("error", "Request body too large"));
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonObject(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
