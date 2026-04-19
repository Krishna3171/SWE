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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorController extends BaseController implements HttpHandler {

    private final VendorDAO vendorDAO;

    public VendorController() {
        this.vendorDAO = new VendorDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
        } else {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Vendor> vendors = vendorDAO.getAllVendors(conn);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < vendors.size(); i++) {
                Vendor v = vendors.get(i);
                sb.append("{")
                        .append("\"vendorId\":").append(v.getVendorId()).append(",")
                        .append("\"vendorName\":\"").append(escapeJson(v.getVendorName())).append("\",")
                        .append("\"address\":\"").append(escapeJson(v.getAddress() != null ? v.getAddress() : ""))
                        .append("\",")
                        .append("\"contactNo\":\"").append(escapeJson(v.getContactNo() != null ? v.getContactNo() : ""))
                        .append("\"")
                        .append("}");
                if (i < vendors.size() - 1)
                    sb.append(",");
            }
            sb.append("]");

            writeJson(exchange, 200, sb.toString());
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange, 16384);
            if (!requireRole(exchange, body, "admin")) {
                return;
            }

            String name = extractString(body, "name");
            String address = extractString(body, "address");
            String contact = extractString(body, "contact");

            if (name == null) {
                writeJson(exchange, 400, "{\"error\":\"Missing fields\"}");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                Vendor vendor = new Vendor();
                vendor.setVendorName(name);
                vendor.setAddress(address);
                vendor.setContactNo(contact);

                boolean success = vendorDAO.insertVendor(conn, vendor);
                if (success) {
                    writeJson(exchange, 201,
                            "{\"message\":\"Vendor added\", \"vendorId\":" + vendor.getVendorId() + "}");
                } else {
                    writeJson(exchange, 500, "{\"error\":\"Failed to insert vendor into database\"}");
                }
            } catch (SQLException e) {
                writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        } catch (Exception e) {
            writeJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find())
            return matcher.group(1);
        return null;
    }
}
