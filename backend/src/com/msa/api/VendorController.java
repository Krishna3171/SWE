package com.msa.api;

import com.msa.dao.VendorDAO;
import com.msa.db.DBConnection;
import com.msa.model.Vendor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class VendorController extends BaseController implements HttpHandler {

    private final VendorDAO vendorDAO;

    public VendorController() {
        this.vendorDAO = new VendorDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST, PUT");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
        } else if ("PUT".equalsIgnoreCase(method)) {
            handlePut(exchange);
        } else {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Vendor> vendors = vendorDAO.getAllVendors(conn);
            writeJsonObject(exchange, 200, vendors);
        } catch (SQLException e) {
            writeJsonObject(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange, 16384);
            if (!requireRole(exchange, body, "admin")) {
                return;
            }

            var node = parseJson(body);
            String name = node.path("name").asText(null);
            String address = node.path("address").isMissingNode() ? null : node.path("address").asText(null);
            String contact = node.path("contact").isMissingNode() ? null : node.path("contact").asText(null);

            if (name == null) {
                writeJsonObject(exchange, 400, Map.of("error", "Missing fields"));
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                Vendor vendor = new Vendor();
                vendor.setVendorName(name);
                vendor.setAddress(address);
                vendor.setContactNo(contact);

                boolean success = vendorDAO.insertVendor(conn, vendor);
                if (success) {
                    writeJsonObject(exchange, 201, Map.of(
                            "message", "Vendor added",
                            "vendorId", vendor.getVendorId()));
                } else {
                    writeJsonObject(exchange, 500, Map.of("error", "Failed to insert vendor into database"));
                }
            } catch (SQLException e) {
                writeJsonObject(exchange, 500, Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            writeJsonObject(exchange, 400, Map.of("error", e.getMessage()));
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange, 16384);
            if (!requireRole(exchange, body, "admin")) {
                return;
            }

            var node = parseJson(body);
            int vendorId = node.path("vendorId").asInt(-1);
            String name = node.path("name").asText(null);
            String address = node.path("address").isMissingNode() ? null : node.path("address").asText(null);
            String contact = node.path("contact").isMissingNode() ? null : node.path("contact").asText(null);

            if (vendorId == -1 || name == null) {
                writeJsonObject(exchange, 400, Map.of("error", "Missing required fields"));
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                Vendor vendor = new Vendor();
                vendor.setVendorId(vendorId);
                vendor.setVendorName(name);
                vendor.setAddress(address);
                vendor.setContactNo(contact);

                boolean updated = vendorDAO.updateVendor(conn, vendor);
                if (updated) {
                    writeJsonObject(exchange, 200, Map.of("message", "Vendor updated successfully"));
                } else {
                    writeJsonObject(exchange, 404, Map.of("error", "Vendor not found or update failed"));
                }
            } catch (SQLException e) {
                writeJsonObject(exchange, 500, Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            writeJsonObject(exchange, 400, Map.of("error", e.getMessage()));
        }
    }
}
