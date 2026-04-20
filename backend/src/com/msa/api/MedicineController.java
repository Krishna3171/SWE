package com.msa.api;

import com.msa.model.Medicine;
import com.msa.service.MedicineService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class MedicineController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 8192;
    private final MedicineService medicineService;

    public MedicineController() {
        this.medicineService = new MedicineService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST, PUT");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handlePut(exchange);
            } else {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<Medicine> list = medicineService.getAllMedicines();
        writeJsonObject(exchange, 200, list);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        var node = parseJson(body);
        String tradeName = node.path("tradeName").asText(null);
        String genericName = node.path("genericName").asText(null);
        String spStr = node.path("unitSellingPrice").asText(null);
        String ppStr = node.path("unitPurchasePrice").asText(null);
        if (isBlank(tradeName) || isBlank(genericName) || isBlank(spStr) || isBlank(ppStr)) {
            writeJsonObject(exchange, 400, Map.of("error", "Missing required fields"));
            return;
        }

        Medicine m = new Medicine();
        m.setTradeName(tradeName);
        m.setGenericName(genericName);
        m.setUnitSellingPrice(new BigDecimal(spStr));
        m.setUnitPurchasePrice(new BigDecimal(ppStr));

        boolean added = medicineService.addMedicine(m);
        if (added) {
            writeJsonObject(exchange, 201, Map.of(
                    "message", "Medicine added",
                    "medicineCode", m.getMedicineCode()));
        } else {
            writeJsonObject(exchange, 400, Map.of("error", "Failed to add medicine"));
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        var node = parseJson(body);
        String medicineCode = node.path("medicineCode").asText(null);
        String tradeName = node.path("tradeName").asText(null);
        String genericName = node.path("genericName").asText(null);
        String spStr = node.path("unitSellingPrice").asText(null);
        String ppStr = node.path("unitPurchasePrice").asText(null);

        if (isBlank(medicineCode) || isBlank(tradeName) || isBlank(genericName) || isBlank(spStr) || isBlank(ppStr)) {
            writeJsonObject(exchange, 400, Map.of("error", "Missing required fields"));
            return;
        }

        Medicine m = new Medicine();
        m.setMedicineCode(medicineCode);
        m.setTradeName(tradeName);
        m.setGenericName(genericName);
        m.setUnitSellingPrice(new BigDecimal(spStr));
        m.setUnitPurchasePrice(new BigDecimal(ppStr));

        boolean updated = medicineService.updateMedicine(m);
        if (updated) {
            writeJsonObject(exchange, 200, Map.of("message", "Medicine updated successfully"));
        } else {
            writeJsonObject(exchange, 404, Map.of("error", "Medicine not found or update failed"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
