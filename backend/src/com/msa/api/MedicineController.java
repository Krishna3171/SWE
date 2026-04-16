package com.msa.api;

import com.msa.dao.MedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Medicine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicineController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 8192;

    private final MedicineDAO medicineDAO;

    public MedicineController() {
        this.medicineDAO = new MedicineDAO();
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

            if ("/api/medicines".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleGetAll(exchange);
                return;
            }

            if ("/api/medicines".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(exchange, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            if (path.startsWith("/api/medicines/") && "GET".equalsIgnoreCase(method)) {
                handleGetByCode(exchange, path);
                return;
            }

            if (path.startsWith("/api/medicines/") && "PUT".equalsIgnoreCase(method)) {
                handleUpdate(exchange, path, readRequestBody(exchange, MAX_REQUEST_BYTES));
                return;
            }

            if (path.startsWith("/api/medicines/") && "DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange, path);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown medicine endpoint\"}");

        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Medicine> medicines = medicineDAO.getAllMedicines(conn);
            writeJson(exchange, 200, toMedicinesJson(medicines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGetByCode(HttpExchange exchange, String path) throws IOException {
        String code = extractPathValue(path, "/api/medicines/");
        if (isBlank(code)) {
            writeJson(exchange, 400, "{\"error\":\"medicine code is required\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            Medicine medicine = medicineDAO.getMedicineByCode(conn, code);
            if (medicine == null) {
                writeJson(exchange, 404, "{\"error\":\"Medicine not found\"}");
                return;
            }
            writeJson(exchange, 200, toMedicineJson(medicine));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCreate(HttpExchange exchange, String body) throws IOException {
        Medicine medicine = parseMedicine(body);
        if (medicine == null) {
            writeJson(exchange, 400,
                    "{\"error\":\"tradeName, genericName, unitSellingPrice and unitPurchasePrice are required\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean created = medicineDAO.insertMedicine(conn, medicine);
            if (!created) {
                writeJson(exchange, 409, "{\"error\":\"Failed to create medicine\"}");
                return;
            }
            writeJson(exchange, 201, toMedicineJson(medicine));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleUpdate(HttpExchange exchange, String path, String body) throws IOException {
        String code = extractPathValue(path, "/api/medicines/");
        Medicine medicine = parseMedicine(body);
        if (isBlank(code) || medicine == null) {
            writeJson(exchange, 400, "{\"error\":\"medicine code and update fields are required\"}");
            return;
        }

        medicine.setMedicineCode(code);

        try (Connection conn = DBConnection.getConnection()) {
            boolean updated = medicineDAO.updateMedicine(conn, medicine);
            if (!updated) {
                writeJson(exchange, 404, "{\"error\":\"Medicine not found or not updated\"}");
                return;
            }
            writeJson(exchange, 200, toMedicineJson(medicine));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String code = extractPathValue(path, "/api/medicines/");
        if (isBlank(code)) {
            writeJson(exchange, 400, "{\"error\":\"medicine code is required\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean deleted = medicineDAO.deleteMedicine(conn, code);
            if (!deleted) {
                writeJson(exchange, 404, "{\"error\":\"Medicine not found\"}");
                return;
            }
            writeJson(exchange, 200, "{\"message\":\"Medicine deleted successfully\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Medicine parseMedicine(String body) {
        String tradeName = extractJsonValue(body, "tradeName");
        String genericName = extractJsonValue(body, "genericName");
        String unitSellingPriceRaw = extractJsonValue(body, "unitSellingPrice");
        String unitPurchasePriceRaw = extractJsonValue(body, "unitPurchasePrice");

        if (isBlank(tradeName) || isBlank(genericName) || isBlank(unitSellingPriceRaw)
                || isBlank(unitPurchasePriceRaw)) {
            return null;
        }

        try {
            Medicine medicine = new Medicine();
            medicine.setTradeName(tradeName.trim());
            medicine.setGenericName(genericName.trim());
            medicine.setUnitSellingPrice(new BigDecimal(unitSellingPriceRaw.trim()));
            medicine.setUnitPurchasePrice(new BigDecimal(unitPurchasePriceRaw.trim()));
            return medicine;
        } catch (Exception e) {
            return null;
        }
    }

    private static String toMedicinesJson(List<Medicine> medicines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < medicines.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(toMedicineJson(medicines.get(i)));
        }
        return "[" + builder + "]";
    }

    private static String toMedicineJson(Medicine medicine) {
        return "{"
                + "\"medicineId\":" + medicine.getMedicineId() + ","
                + "\"medicineCode\":\"" + escapeJson(medicine.getMedicineCode()) + "\","
                + "\"tradeName\":\"" + escapeJson(medicine.getTradeName()) + "\","
                + "\"genericName\":\"" + escapeJson(medicine.getGenericName()) + "\","
                + "\"unitSellingPrice\":" + toNumber(medicine.getUnitSellingPrice()) + ","
                + "\"unitPurchasePrice\":" + toNumber(medicine.getUnitPurchasePrice())
                + "}";
    }

    private static String extractPathValue(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        String value = path.substring(prefix.length());
        return value.isBlank() ? null : value;
    }

    private static String extractJsonValue(String body, String key) {
        String quotedPatternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
        Matcher quotedMatcher = Pattern.compile(quotedPatternText).matcher(body);
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1);
        }
        String numberPatternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)";
        Matcher numberMatcher = Pattern.compile(numberPatternText).matcher(body);
        if (numberMatcher.find()) {
            return numberMatcher.group(1);
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String toNumber(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
