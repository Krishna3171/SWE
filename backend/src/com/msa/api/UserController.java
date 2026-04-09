package com.msa.api;

import com.msa.model.AppUser;
import com.msa.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
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

}
