package com.msa.api;

import com.msa.db.DBConnection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;

public class HealthController extends BaseController implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET");

        if (isPreflight(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try (Connection ignored = DBConnection.getConnection()) {
            writeJsonObject(exchange, 200, Map.of("status", "UP", "db", "UP"));
        } catch (Exception e) {
            writeJsonObject(exchange, 503, Map.of("status", "DOWN", "db", "DOWN"));
        }
    }
}
