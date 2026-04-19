package com.msa.api;

import com.msa.db.DBConnection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;

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
            writeJson(exchange, 200, "{\"status\":\"UP\",\"db\":\"UP\"}");
        } catch (Exception e) {
            writeJson(exchange, 503, "{\"status\":\"DOWN\",\"db\":\"DOWN\"}");
        }
    }
}
