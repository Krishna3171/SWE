package com.msa.api;

import com.msa.model.Medicine;
import com.msa.service.MedicineService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicineController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 8192;
    private final MedicineService medicineService;

    public MedicineController() {
        this.medicineService = new MedicineService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
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
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Medicine m = list.get(i);
            json.append("{")
                    .append("\"medicineId\":").append(m.getMedicineId()).append(",")
                    .append("\"medicineCode\":\"").append(escapeJson(m.getMedicineCode())).append("\",")
                    .append("\"tradeName\":\"").append(escapeJson(m.getTradeName())).append("\",")
                    .append("\"genericName\":\"").append(escapeJson(m.getGenericName())).append("\",")
                    .append("\"unitSellingPrice\":").append(m.getUnitSellingPrice()).append(",")
                    .append("\"unitPurchasePrice\":").append(m.getUnitPurchasePrice())
                    .append("}");
            if (i < list.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        writeJson(exchange, 200, json.toString());
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
        if (!requireRole(exchange, body, "admin")) {
            return;
        }

        String tradeName = extractJsonValue(body, "tradeName");
        String genericName = extractJsonValue(body, "genericName");
        String spStr = extractJsonValue(body, "unitSellingPrice");
        String ppStr = extractJsonValue(body, "unitPurchasePrice");
        if (isBlank(tradeName) || isBlank(genericName) || isBlank(spStr) || isBlank(ppStr)) {
            writeJson(exchange, 400, "{\"error\":\"Missing required fields\"}");
            return;
        }

        Medicine m = new Medicine();
        m.setTradeName(tradeName);
        m.setGenericName(genericName);
        m.setUnitSellingPrice(new BigDecimal(spStr));
        m.setUnitPurchasePrice(new BigDecimal(ppStr));

        boolean added = medicineService.addMedicine(m);
        if (added) {
            writeJson(exchange, 201,
                    "{\"message\":\"Medicine added\",\"medicineCode\":\"" + escapeJson(m.getMedicineCode()) + "\"}");
        } else {
            writeJson(exchange, 400, "{\"error\":\"Failed to add medicine\"}");
        }
    }

    private static String extractJsonValue(String body, String key) {
        String patternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9.]+))";
        Pattern pattern = Pattern.compile(patternText);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
