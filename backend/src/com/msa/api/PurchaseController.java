package com.msa.api;

import com.msa.dto.PurchaseItemRequest;
import com.msa.dto.PurchaseRequest;
import com.msa.dto.PurchaseResponse;
import com.msa.dao.BatchDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dao.PurchaseDAO;
import com.msa.dao.PurchaseDetailsDAO;
import com.msa.dao.VendorDAO;
import com.msa.db.DBConnection;
import com.msa.model.Batch;
import com.msa.model.Medicine;
import com.msa.model.Purchase;
import com.msa.model.PurchaseDetails;
import com.msa.model.Vendor;
import com.msa.service.PurchaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PurchaseController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 10240;

    private final PurchaseService purchaseService;
    private final PurchaseDAO purchaseDAO;
    private final PurchaseDetailsDAO purchaseDetailsDAO;
    private final VendorDAO vendorDAO;
    private final MedicineDAO medicineDAO;
    private final BatchDAO batchDAO;

    public PurchaseController() {
        this.purchaseService = new PurchaseService();
        this.purchaseDAO = new PurchaseDAO();
        this.purchaseDetailsDAO = new PurchaseDetailsDAO();
        this.vendorDAO = new VendorDAO();
        this.medicineDAO = new MedicineDAO();
        this.batchDAO = new BatchDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();

            if ("/api/purchases".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleListPurchases(exchange);
                return;
            }

            if (path.startsWith("/api/purchases/") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGetPurchase(exchange, path);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);

            if ("/api/purchases/create".equals(path)) {
                handleCreatePurchase(exchange, body);
                return;
            }

            if ("/api/purchases/receive".equals(path)) {
                handleReceivePurchase(exchange, body);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown purchase endpoint\"}");
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleCreatePurchase(HttpExchange exchange, String body) throws IOException {
        String vendorIdRaw = extractJsonValue(body, "vendorId");
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

        List<PurchaseItemRequest> items = parsePurchaseItems(body);
        if (items.isEmpty()) {
            writeJson(exchange, 400, "{\"error\":\"items are required\"}");
            return;
        }

        PurchaseRequest request = new PurchaseRequest(vendorId, items);
        PurchaseResponse response = purchaseService.makePurchase(request);

        String json = "{"
                + "\"purchaseId\":" + response.getPurchaseId() + ","
                + "\"totalAmount\":" + response.getTotalAmount() + ","
                + "\"message\":\"" + escapeJson(response.getMessage()) + "\""
                + "}";

        writeJson(exchange, 201, json);
    }

    private void handleReceivePurchase(HttpExchange exchange, String body) throws IOException {
        String purchaseIdRaw = extractJsonValue(body, "purchaseId");

        if (isBlank(purchaseIdRaw)) {
            writeJson(exchange, 400, "{\"error\":\"purchaseId is required\"}");
            return;
        }

        int purchaseId;
        try {
            purchaseId = Integer.parseInt(purchaseIdRaw.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"purchaseId must be a valid number\"}");
            return;
        }

        try {
            int receivedLines = purchaseService.receivePurchase(purchaseId);
            String json = "{"
                    + "\"purchaseId\":" + purchaseId + ","
                    + "\"receivedLines\":" + receivedLines + ","
                    + "\"message\":\"Purchase received successfully\""
                    + "}";
            writeJson(exchange, 200, json);
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "Failed to receive purchase" : e.getMessage();
            if (message.contains("already fully received")) {
                writeJson(exchange, 409, "{\"error\":\"" + escapeJson(message) + "\"}");
                return;
            }
            if (message.contains("not found")) {
                writeJson(exchange, 404, "{\"error\":\"" + escapeJson(message) + "\"}");
                return;
            }
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(message) + "\"}");
        }
    }

    private void handleListPurchases(HttpExchange exchange) throws IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Purchase> purchases = purchaseDAO.getAllPurchases(conn);
            writeJson(exchange, 200, toPurchasesJson(conn, purchases));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGetPurchase(HttpExchange exchange, String path) throws IOException {
        String idPart = extractPathValue(path, "/api/purchases/");
        if (isBlank(idPart)) {
            writeJson(exchange, 400, "{\"error\":\"purchaseId is required\"}");
            return;
        }

        int purchaseId;
        try {
            purchaseId = Integer.parseInt(idPart.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"purchaseId must be a valid number\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            Purchase purchase = purchaseDAO.getPurchaseById(conn, purchaseId);
            if (purchase == null) {
                writeJson(exchange, 404, "{\"error\":\"Purchase not found\"}");
                return;
            }

            writeJson(exchange, 200, toPurchaseDetailJson(conn, purchase));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<PurchaseItemRequest> parsePurchaseItems(String body) {
        List<PurchaseItemRequest> items = new ArrayList<>();

        String itemsArray = extractJsonArray(body, "items");
        if (itemsArray == null) {
            return items;
        }

        Matcher matcher = Pattern.compile("\\{[^{}]*\\}").matcher(itemsArray);
        while (matcher.find()) {
            String itemJson = matcher.group();

            String medicineCode = extractJsonValue(itemJson, "medicineCode");
            String batchNumber = extractJsonValue(itemJson, "batchNumber");
            String expiryDate = extractJsonValue(itemJson, "expiryDate");
            String quantityRaw = extractJsonValue(itemJson, "quantity");
            String unitPurchasePriceRaw = extractJsonValue(itemJson, "unitPurchasePrice");

            if (isBlank(medicineCode)
                    || isBlank(batchNumber)
                    || isBlank(expiryDate)
                    || isBlank(quantityRaw)
                    || isBlank(unitPurchasePriceRaw)) {
                continue;
            }

            try {
                int quantity = Integer.parseInt(quantityRaw.trim());
                BigDecimal unitPurchasePrice = new BigDecimal(unitPurchasePriceRaw.trim());

                items.add(new PurchaseItemRequest(
                        medicineCode.trim(),
                        batchNumber.trim(),
                        expiryDate.trim(),
                        quantity,
                        unitPurchasePrice));
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

    private static String extractPathValue(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }

        String value = path.substring(prefix.length());
        int slashIndex = value.indexOf('/');
        if (slashIndex >= 0) {
            value = value.substring(0, slashIndex);
        }
        return value;
    }

    private String toPurchasesJson(Connection conn, List<Purchase> purchases) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < purchases.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(toPurchaseSummaryJson(conn, purchases.get(i)));
        }
        return "[" + builder + "]";
    }

    private String toPurchaseSummaryJson(Connection conn, Purchase purchase) throws Exception {
        Vendor vendor = vendorDAO.getVendorById(conn, purchase.getVendorId());
        int totalLines = purchaseDetailsDAO.getTotalQuantityByPurchaseId(conn, purchase.getPurchaseId());
        int receivedLines = purchaseDetailsDAO.getReceivedQuantityByPurchaseId(conn, purchase.getPurchaseId());

        String status = receivedLines == 0 ? "PENDING" : (receivedLines >= totalLines ? "RECEIVED" : "PARTIAL");

        return "{"
                + "\"purchaseId\":" + purchase.getPurchaseId() + ","
                + "\"purchaseDate\":\"" + purchase.getPurchaseDate() + "\","
                + "\"vendorId\":" + purchase.getVendorId() + ","
                + "\"vendorName\":\"" + escapeJson(vendor != null ? vendor.getVendorName() : "UNKNOWN") + "\","
                + "\"totalAmount\":" + purchase.getTotalAmount() + ","
                + "\"totalQuantity\":" + totalLines + ","
                + "\"receivedQuantity\":" + receivedLines + ","
                + "\"status\":\"" + status + "\""
                + "}";
    }

    private String toPurchaseDetailJson(Connection conn, Purchase purchase) throws Exception {
        Vendor vendor = vendorDAO.getVendorById(conn, purchase.getVendorId());
        List<PurchaseDetails> details = purchaseDetailsDAO.getPurchaseDetailsByPurchaseId(conn,
                purchase.getPurchaseId());

        StringBuilder itemsBuilder = new StringBuilder();
        for (int i = 0; i < details.size(); i++) {
            if (i > 0) {
                itemsBuilder.append(',');
            }
            PurchaseDetails detail = details.get(i);
            Medicine medicine = medicineDAO.getMedicineById(conn, detail.getMedicineId());
            Batch batch = null;
            if (detail.getBatchId() > 0) {
                // batch number is optional in response; batch id is enough for the flow
            }
            itemsBuilder.append('{')
                    .append("\"medicineId\":").append(detail.getMedicineId()).append(',')
                    .append("\"medicineCode\":\"")
                    .append(escapeJson(medicine != null ? medicine.getMedicineCode() : "UNKNOWN")).append("\",")
                    .append("\"batchId\":").append(detail.getBatchId()).append(',')
                    .append("\"quantity\":").append(detail.getQuantity()).append(',')
                    .append("\"unitPurchasePrice\":").append(detail.getUnitPurchasePrice()).append(',')
                    .append("\"received\":").append(detail.isReceived())
                    .append('}');
        }

        int totalQuantity = purchaseDetailsDAO.getTotalQuantityByPurchaseId(conn, purchase.getPurchaseId());
        int receivedQuantity = purchaseDetailsDAO.getReceivedQuantityByPurchaseId(conn, purchase.getPurchaseId());
        String status = receivedQuantity == 0 ? "PENDING"
                : (receivedQuantity >= totalQuantity ? "RECEIVED" : "PARTIAL");

        return "{"
                + "\"purchaseId\":" + purchase.getPurchaseId() + ","
                + "\"purchaseDate\":\"" + purchase.getPurchaseDate() + "\","
                + "\"vendorId\":" + purchase.getVendorId() + ","
                + "\"vendorName\":\"" + escapeJson(vendor != null ? vendor.getVendorName() : "UNKNOWN") + "\","
                + "\"totalAmount\":" + purchase.getTotalAmount() + ","
                + "\"totalQuantity\":" + totalQuantity + ","
                + "\"receivedQuantity\":" + receivedQuantity + ","
                + "\"status\":\"" + status + "\","
                + "\"items\":[" + itemsBuilder + "]"
                + "}";
    }
}
