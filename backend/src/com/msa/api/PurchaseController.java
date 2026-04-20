package com.msa.api;

import com.msa.dto.PurchaseItemRequest;
import com.msa.dto.PurchaseRequest;
import com.msa.dto.PurchaseResponse;
import com.msa.model.PurchaseDetails;
import com.msa.service.PurchaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 32768; // 32KB
    private final PurchaseService purchaseService;

    public PurchaseController() {
        this.purchaseService = new PurchaseService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.endsWith("/pending")) {
                    if (!requireAnyRole(exchange, null, "cashier", "admin")) {
                        return;
                    }
                    handleGetPending(exchange);
                    return;
                }
                writeJson(exchange, 404, "{\"error\":\"Not found\"}");
                return;
            }

            if (!"POST".equals(method)) {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);

            if (path.endsWith("/receive")) {
                if (!requireAnyRole(exchange, body, "cashier", "admin")) {
                    return;
                }
                handleReceive(exchange, body);
                return;
            }

            if (!path.endsWith("/purchases")) {
                writeJson(exchange, 404, "{\"error\":\"Not found\"}");
                return;
            }

            if (!requireAnyRole(exchange, body, "cashier", "admin")) {
                return;
            }

            var root = parseJson(body);
            int vendorId = root.path("vendorId").asInt(-1);
            if (vendorId == -1) {
                writeJsonObject(exchange, 400, Map.of("error", "Missing vendorId"));
                return;
            }

            List<PurchaseItemRequest> items = new ArrayList<>();
            var itemsNode = root.path("items");
            if (itemsNode.isArray()) {
                for (var itemNode : itemsNode) {
                    String code = itemNode.path("medicineCode").asText(null);
                    String batch = itemNode.path("batchNumber").asText(null);
                    String expiry = itemNode.path("expiryDate").asText(null);
                    int qty = itemNode.path("quantity").asInt(-1);
                    String priceRaw = itemNode.path("unitPurchasePrice").asText(null);

                    if (code == null || batch == null || expiry == null || qty == -1 || priceRaw == null) {
                        throw new IllegalArgumentException("Invalid item fields");
                    }

                    items.add(new PurchaseItemRequest(
                            code,
                            batch,
                            expiry,
                            qty,
                            new java.math.BigDecimal(priceRaw)));
                }
            }

            if (items.isEmpty()) {
                writeJsonObject(exchange, 400, Map.of("error", "No items provided in purchase"));
                return;
            }

            PurchaseRequest req = new PurchaseRequest(vendorId, items);
            PurchaseResponse resp = purchaseService.makePurchase(req);

            writeJsonObject(exchange, 201, Map.of(
                    "purchaseId", resp.getPurchaseId(),
                    "totalAmount", resp.getTotalAmount(),
                    "message", resp.getMessage()));

        } catch (IllegalArgumentException e) {
            writeJsonObject(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonObject(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleReceive(HttpExchange exchange, String body) throws IOException {
        var root = parseJson(body);
        int purchaseId = root.path("purchaseId").asInt(-1);
        int batchId = root.path("batchId").asInt(-1);

        if (purchaseId == -1 || batchId == -1) {
            writeJsonObject(exchange, 400, Map.of("error", "purchaseId and batchId are required"));
            return;
        }

        PurchaseDetails received = purchaseService.receivePurchaseLine(purchaseId, batchId);
        writeJsonObject(exchange, 200, Map.of(
                "purchaseId", received.getPurchaseId(),
                "batchId", received.getBatchId(),
                "medicineId", received.getMedicineId(),
                "quantity", received.getQuantity(),
                "received", received.isReceived(),
                "message", "Purchase line received successfully"));
    }

    private void handleGetPending(HttpExchange exchange) throws IOException {
        List<PurchaseDetails> pending = purchaseService.getPendingPurchaseDetails();
        List<Map<String, Object>> response = new ArrayList<>();
        for (PurchaseDetails detail : pending) {
            Map<String, Object> row = new HashMap<>();
            row.put("purchaseId", detail.getPurchaseId());
            row.put("medicineId", detail.getMedicineId());
            row.put("batchId", detail.getBatchId());
            row.put("quantity", detail.getQuantity());
            row.put("unitPrice", detail.getUnitPrice());
            row.put("purchaseDate", detail.getPurchaseDate());
            row.put("received", detail.isReceived());
            response.add(row);
        }
        writeJsonObject(exchange, 200, response);
    }
}
