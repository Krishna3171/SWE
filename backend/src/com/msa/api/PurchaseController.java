package com.msa.api;

import com.msa.dto.PurchaseItemRequest;
import com.msa.dto.PurchaseRequest;
import com.msa.dto.PurchaseResponse;
import com.msa.service.PurchaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PurchaseController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 32768; // 32KB
    private final PurchaseService purchaseService;

    public PurchaseController() {
        this.purchaseService = new PurchaseService();
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
            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
            
            // Expected simple JSON: {"vendorId": 1, "items": [{"medicineCode":"MED1", "batchNumber": "B-1", "expiryDate": "2024-12-01", "quantity": 100, "unitPurchasePrice": 5.5}]}
            int vendorId = extractInt(body, "vendorId");
            if (vendorId == -1) {
                writeJson(exchange, 400, "{\"error\":\"Missing vendorId\"}");
                return;
            }

            List<PurchaseItemRequest> items = new ArrayList<>();
            Pattern itemPattern = Pattern.compile("\\{[^\\}]*\\\"medicineCode\\\"\\s*:\\s*\\\"([^\"]+)\\\"[^\\}]*\\}");
            Matcher matcher = itemPattern.matcher(body);

            while (matcher.find()) {
                String itemBlock = matcher.group(0);
                String code = extractString(itemBlock, "medicineCode");
                String batch = extractString(itemBlock, "batchNumber");
                String expiry = extractString(itemBlock, "expiryDate");
                int qty = extractInt(itemBlock, "quantity");
                double price = extractDouble(itemBlock, "unitPurchasePrice");

                if (code == null || batch == null || expiry == null || qty == -1 || price == -1) {
                    throw new IllegalArgumentException("Invalid item fields");
                }

                items.add(new PurchaseItemRequest(code, batch, expiry, qty, BigDecimal.valueOf(price)));
            }

            if (items.isEmpty()) {
                writeJson(exchange, 400, "{\"error\":\"No items provided in purchase\"}");
                return;
            }

            PurchaseRequest req = new PurchaseRequest(vendorId, items);
            PurchaseResponse resp = purchaseService.makePurchase(req);

            String jsonResp = "{"
                    + "\"purchaseId\":" + resp.getPurchaseId() + ","
                    + "\"totalAmount\":" + resp.getTotalAmount() + ","
                    + "\"message\":\"" + escapeJson(resp.getMessage()) + "\""
                    + "}";

            writeJson(exchange, 201, jsonResp);

        } catch (IllegalArgumentException e) {
            writeJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private int extractInt(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        return -1;
    }
    
    private double extractDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*([\\d\\.]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return Double.parseDouble(matcher.group(1));
        return -1;
    }

    private String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return matcher.group(1);
        return null;
    }
}
