package com.msa.api;

import com.msa.dao.VendorMedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.AppUser;
import com.msa.model.VendorMedicine;
import com.msa.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorMedicineController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 4096;

    private final VendorMedicineDAO vendorMedicineDAO;
    private final AuthService authService;

    public VendorMedicineController() {
        this.vendorMedicineDAO = new VendorMedicineDAO();
        this.authService = new AuthService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST, DELETE");

        if (isPreflight(exchange)) {
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/vendor-medicine".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleGetAll(exchange);
                return;
            }

            if ("/api/vendor-medicine".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleLink(exchange, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            if ("/api/vendor-medicine".equals(path) && "DELETE".equalsIgnoreCase(method)) {
                handleUnlink(exchange, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown vendor-medicine endpoint\"}");
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try (var conn = DBConnection.getConnection()) {
            List<VendorMedicine> mappings = vendorMedicineDAO.getAllMappings(conn);
            writeJson(exchange, 200, toMappingsJson(mappings));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleLink(HttpExchange exchange, String body) throws IOException {
        AppUser admin = authenticateAdmin(body);
        if (admin == null) {
            writeJson(exchange, 401, "{\"error\":\"Invalid admin credentials\"}");
            return;
        }

        Integer vendorId = parseIntValue(body, "vendorId");
        Integer medicineId = parseIntValue(body, "medicineId");
        if (vendorId == null || medicineId == null) {
            writeJson(exchange, 400, "{\"error\":\"vendorId and medicineId are required\"}");
            return;
        }

        try (var conn = DBConnection.getConnection()) {
            boolean linked = vendorMedicineDAO.linkVendorToMedicine(conn, vendorId, medicineId);
            if (!linked) {
                writeJson(exchange, 409, "{\"error\":\"Mapping already exists or could not be created\"}");
                return;
            }
            writeJson(exchange, 201, "{\"message\":\"Vendor-medicine mapping created successfully\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleUnlink(HttpExchange exchange, String body) throws IOException {
        AppUser admin = authenticateAdmin(body);
        if (admin == null) {
            writeJson(exchange, 401, "{\"error\":\"Invalid admin credentials\"}");
            return;
        }

        Integer vendorId = parseIntValue(body, "vendorId");
        Integer medicineId = parseIntValue(body, "medicineId");
        if (vendorId == null || medicineId == null) {
            writeJson(exchange, 400, "{\"error\":\"vendorId and medicineId are required\"}");
            return;
        }

        try (var conn = DBConnection.getConnection()) {
            boolean unlinked = vendorMedicineDAO.unlinkVendorFromMedicine(conn, vendorId, medicineId);
            if (!unlinked) {
                writeJson(exchange, 404, "{\"error\":\"Mapping not found\"}");
                return;
            }
            writeJson(exchange, 200, "{\"message\":\"Vendor-medicine mapping removed successfully\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AppUser authenticateAdmin(String body) {
        String adminUsername = extractJsonValue(body, "adminUsername");
        String adminPassword = extractJsonValue(body, "adminPassword");
        if (isBlank(adminUsername) || isBlank(adminPassword)) {
            return null;
        }
        return authService.login(adminUsername.trim().toLowerCase(), adminPassword, "admin");
    }

    private static Integer parseIntValue(String body, String key) {
        String raw = extractJsonValue(body, key);
        if (isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toMappingsJson(List<VendorMedicine> mappings) {
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
