package com.msa.api;

import com.msa.dao.VendorDAO;
import com.msa.dao.VendorMedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Vendor;
import com.msa.model.VendorMedicine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 8192;

    private final VendorDAO vendorDAO;
    private final VendorMedicineDAO vendorMedicineDAO;

    public VendorController() {
        this.vendorDAO = new VendorDAO();
        this.vendorMedicineDAO = new VendorMedicineDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST, PUT, DELETE");

        if (isPreflight(exchange)) {
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/vendors".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleGetAll(exchange);
                return;
            }

            if ("/api/vendors".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(exchange, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            if (path.startsWith("/api/vendors/") && path.endsWith("/medicines") && "GET".equalsIgnoreCase(method)) {
                handleGetMedicinesForVendor(exchange, path);
                return;
            }

            if (path.startsWith("/api/vendors/") && "PUT".equalsIgnoreCase(method)) {
                handleUpdate(exchange, path, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            if (path.startsWith("/api/vendors/") && "DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange, path);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown vendor endpoint\"}");

        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try (var conn = DBConnection.getConnection()) {
            List<Vendor> vendors = vendorDAO.getAllVendors(conn);
            writeJson(exchange, 200, toVendorsJson(vendors));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCreate(HttpExchange exchange, String body) throws IOException {
        Vendor vendor = parseVendor(body);
        if (vendor == null) {
            writeJson(exchange, 400, "{\"error\":\"vendorName, address and contactNo are required\"}");
            return;
        }

        try (var conn = DBConnection.getConnection()) {
            boolean created = vendorDAO.insertVendor(conn, vendor);
            if (!created) {
                writeJson(exchange, 409, "{\"error\":\"Failed to create vendor\"}");
                return;
            }
            writeJson(exchange, 201, toVendorJson(vendor));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleUpdate(HttpExchange exchange, String path, String body) throws IOException {
        String vendorIdRaw = extractPathValue(path, "/api/vendors/");
        Vendor vendor = parseVendor(body);
        if (isBlank(vendorIdRaw) || vendor == null) {
            writeJson(exchange, 400, "{\"error\":\"vendorId and update fields are required\"}");
            return;
        }

        int vendorId;
        try {
            vendorId = Integer.parseInt(vendorIdRaw.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"vendorId must be a valid number\"}");
            return;
        }

        vendor.setVendorId(vendorId);

        try (var conn = DBConnection.getConnection()) {
            boolean updated = vendorDAO.updateVendor(conn, vendor);
            if (!updated) {
                writeJson(exchange, 404, "{\"error\":\"Vendor not found or not updated\"}");
                return;
            }
            writeJson(exchange, 200, toVendorJson(vendor));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String vendorIdRaw = extractPathValue(path, "/api/vendors/");
        if (isBlank(vendorIdRaw)) {
            writeJson(exchange, 400, "{\"error\":\"vendorId is required\"}");
            return;
        }

        int vendorId;
        try {
            vendorId = Integer.parseInt(vendorIdRaw.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"vendorId must be a valid number\"}");
            return;
        }

        try (var conn = DBConnection.getConnection()) {
            boolean deleted = vendorDAO.deleteVendor(conn, vendorId);
            if (!deleted) {
                writeJson(exchange, 404, "{\"error\":\"Vendor not found\"}");
                return;
            }
            writeJson(exchange, 200, "{\"message\":\"Vendor deleted successfully\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGetMedicinesForVendor(HttpExchange exchange, String path) throws IOException {
        String vendorIdRaw = extractPathValue(path, "/api/vendors/");
        if (vendorIdRaw == null || !vendorIdRaw.endsWith("/medicines")) {
            writeJson(exchange, 400, "{\"error\":\"vendorId is required\"}");
            return;
        }

        String idPart = vendorIdRaw.substring(0, vendorIdRaw.length() - "/medicines".length());
        int vendorId;
        try {
            vendorId = Integer.parseInt(idPart.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"vendorId must be a valid number\"}");
            return;
        }

        try (var conn = DBConnection.getConnection()) {
            List<VendorMedicine> mappings = vendorMedicineDAO.getMedicinesForVendor(conn, vendorId);
            writeJson(exchange, 200, toVendorMedicinesJson(mappings));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Vendor parseVendor(String body) {
        String vendorName = extractJsonValue(body, "vendorName");
        String address = extractJsonValue(body, "address");
        String contactNo = extractJsonValue(body, "contactNo");

        if (isBlank(vendorName) || isBlank(address) || isBlank(contactNo)) {
            return null;
        }

        return new Vendor(vendorName.trim(), address.trim(), contactNo.trim());
    }

    private static String toVendorsJson(List<Vendor> vendors) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vendors.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(toVendorJson(vendors.get(i)));
        }
        return "[" + builder + "]";
    }

    private static String toVendorJson(Vendor vendor) {
        return "{"
                + "\"vendorId\":" + vendor.getVendorId() + ","
                + "\"vendorName\":\"" + escapeJson(vendor.getVendorName()) + "\","
                + "\"address\":\"" + escapeJson(vendor.getAddress()) + "\","
                + "\"contactNo\":\"" + escapeJson(vendor.getContactNo()) + "\""
                + "}";
    }

    private static String toVendorMedicinesJson(List<VendorMedicine> mappings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mappings.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            VendorMedicine mapping = mappings.get(i);
            builder.append('{')
                    .append("\"vendorId\":").append(mapping.getVendorId()).append(',')
                    .append("\"medicineId\":").append(mapping.getMedicineId())
                    .append('}');
        }
        return "[" + builder + "]";
    }

    private static String extractPathValue(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        return path.substring(prefix.length());
    }

    private static String extractJsonValue(String body, String key) {
        String quotedPatternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
        Matcher quotedMatcher = Pattern.compile(quotedPatternText).matcher(body);
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1);
        }
        String numberPatternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*([0-9]+)";
        Matcher numberMatcher = Pattern.compile(numberPatternText).matcher(body);
        if (numberMatcher.find()) {
            return numberMatcher.group(1);
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
