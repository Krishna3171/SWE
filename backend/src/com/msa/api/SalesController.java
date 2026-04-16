package com.msa.api;

import com.msa.dto.SaleItemRequest;
import com.msa.dto.SaleRequest;
import com.msa.dto.SaleResponse;
import com.msa.service.SalesService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SalesController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 8192;

    private final SalesService salesService;

    public SalesController() {
        this.salesService = new SalesService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "POST");

        if (isPreflight(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);

            if ("/api/sales/create".equals(path)) {
                handleCreateSale(exchange, body);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown sales endpoint\"}");
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleCreateSale(HttpExchange exchange, String body) throws IOException {
        List<SaleItemRequest> items = parseSaleItems(body);
        if (items.isEmpty()) {
            writeJson(exchange, 400, "{\"error\":\"items are required\"}");
            return;
        }

        SaleRequest request = new SaleRequest(items);
        SaleResponse response = salesService.makeSale(request);

        String json = "{"
                + "\"saleId\":" + response.getSaleId() + ","
                + "\"totalAmount\":" + response.getTotalAmount() + ","
                + "\"message\":\"" + escapeJson(response.getMessage()) + "\""
                + "}";

        writeJson(exchange, 201, json);
    }

    private static List<SaleItemRequest> parseSaleItems(String body) {
        List<SaleItemRequest> items = new ArrayList<>();

        String itemsArray = extractJsonArray(body, "items");
        if (itemsArray == null) {
            return items;
        }

        Matcher matcher = Pattern.compile("\\{[^{}]*\\}").matcher(itemsArray);
        while (matcher.find()) {
            String itemJson = matcher.group();

            String medicineCode = extractJsonValue(itemJson, "medicineCode");
            String quantityRaw = extractJsonValue(itemJson, "quantity");

            if (isBlank(medicineCode) || isBlank(quantityRaw)) {
                continue;
            }

            try {
                int quantity = Integer.parseInt(quantityRaw.trim());
                items.add(new SaleItemRequest(medicineCode.trim(), quantity));
            } catch (NumberFormatException ignored) {
                // Skip malformed items and let service validate remaining input.
            }
        }

        return items;
    }

    private static String extractJsonArray(String body, String key) {
        String patternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)]";
        Pattern pattern = Pattern.compile(patternText, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
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
