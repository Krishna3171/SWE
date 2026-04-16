package com.msa.api;

import com.msa.dao.InventoryDAO;
import com.msa.db.DBConnection;
import com.msa.model.Inventory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InventoryController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 4096;

    private final InventoryDAO inventoryDAO;

    public InventoryController() {
        this.inventoryDAO = new InventoryDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, PUT");

        if (isPreflight(exchange)) {
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/inventory".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleGetAll(exchange);
                return;
            }

            if (path.startsWith("/api/inventory/") && path.endsWith("/threshold") && "PUT".equalsIgnoreCase(method)) {
                handleUpdateThreshold(exchange, path, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            if ("/api/inventory/low-stock".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleLowStock(exchange);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown inventory endpoint\"}");
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try (var conn = DBConnection.getConnection()) {
            List<Inventory> inventory = inventoryDAO.getAllInventory(conn);
            writeJson(exchange, 200, toInventoryJson(inventory));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleLowStock(HttpExchange exchange) throws IOException {
        try (var conn = DBConnection.getConnection()) {
            List<Inventory> inventory = inventoryDAO.getLowStockMedicines(conn);
            writeJson(exchange, 200, toInventoryJson(inventory));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleUpdateThreshold(HttpExchange exchange, String path, String body) throws IOException {
        String medicineIdRaw = extractPathValue(path, "/api/inventory/");
        if (medicineIdRaw == null || !medicineIdRaw.endsWith("/threshold")) {
            writeJson(exchange, 400, "{\"error\":\"medicineId is required\"}");
            return;
        }

        String idPart = medicineIdRaw.substring(0, medicineIdRaw.length() - "/threshold".length());
        String thresholdRaw = extractJsonValue(body, "reorderThreshold");

        if (isBlank(idPart) || isBlank(thresholdRaw)) {
            writeJson(exchange, 400, "{\"error\":\"medicineId and reorderThreshold are required\"}");
            return;
        }

        int medicineId;
        int threshold;
        try {
            medicineId = Integer.parseInt(idPart.trim());
            threshold = Integer.parseInt(thresholdRaw.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"medicineId and reorderThreshold must be valid numbers\"}");
            return;
        }

        try (var conn = DBConnection.getConnection()) {
            boolean updated = inventoryDAO.updateReorderThreshold(conn, medicineId, threshold);
            if (!updated) {
                writeJson(exchange, 404, "{\"error\":\"Inventory row not found\"}");
                return;
            }
            writeJson(exchange, 200, "{\"message\":\"Reorder threshold updated successfully\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toInventoryJson(List<Inventory> inventory) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < inventory.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            Inventory item = inventory.get(i);
            builder.append('{')
                    .append("\"medicineId\":").append(item.getMedicineId()).append(',')
                    .append("\"quantityAvailable\":").append(item.getQuantityAvailable()).append(',')
                    .append("\"reorderThreshold\":").append(item.getReorderThreshold())
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
