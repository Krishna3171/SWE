package com.msa.api;

import com.msa.dao.MedicineDAO;
import com.msa.dao.SalesDAO;
import com.msa.dao.SalesDetailsDAO;
import com.msa.db.DBConnection;
import com.msa.dto.SaleItemRequest;
import com.msa.dto.SaleRequest;
import com.msa.dto.SaleResponse;
import com.msa.model.Medicine;
import com.msa.model.Sales;
import com.msa.model.SalesDetails;
import com.msa.service.SalesService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalesController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 16384;
    private final SalesService salesService;
    private final SalesDAO salesDAO;
    private final SalesDetailsDAO salesDetailsDAO;
    private final MedicineDAO medicineDAO;

    public SalesController() {
        this.salesService = new SalesService();
        this.salesDAO = new SalesDAO();
        this.salesDetailsDAO = new SalesDetailsDAO();
        this.medicineDAO = new MedicineDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            handleGetRecentSales(exchange);
        } else if ("POST".equals(method)) {
            handleMakeSale(exchange);
        } else {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleGetRecentSales(HttpExchange exchange) throws IOException {
        if (!requireAnyRole(exchange, null, "cashier", "admin")) {
            return;
        }
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            List<Sales> recentSales = salesDAO.getRecentSales(conn, 20);

            List<Map<String, Object>> responseRows = new ArrayList<>();

            for (Sales sale : recentSales) {

                // Get line-item details for this sale
                List<SalesDetails> details = salesDetailsDAO.getSalesDetailsBySaleId(conn, sale.getSaleId());

                List<Map<String, Object>> items = new ArrayList<>();
                for (SalesDetails d : details) {
                    Medicine med = medicineDAO.getMedicineById(conn, d.getMedicineId());
                    String medName = med != null ? med.getMedicineName() : "Unknown";
                    String medCode = med != null ? med.getMedicineCode() : "N/A";

                    Map<String, Object> item = new HashMap<>();
                    item.put("medicineId", d.getMedicineId());
                    item.put("medicineName", medName);
                    item.put("medicineCode", medCode);
                    item.put("quantity", d.getQuantitySold());
                    item.put("unitPrice", d.getPrice());
                    items.add(item);
                }

                Map<String, Object> row = new HashMap<>();
                row.put("saleId", sale.getSaleId());
                row.put("saleDate", String.valueOf(sale.getSaleDate()));
                row.put("totalAmount", sale.getTotalAmount());
                row.put("items", items);
                responseRows.add(row);
            }

            writeJsonObject(exchange, 200, responseRows);

        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void handleMakeSale(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
            if (!requireAnyRole(exchange, body, "cashier", "admin")) {
                return;
            }

            List<SaleItemRequest> items = new ArrayList<>();

            var node = parseJson(body);
            var itemsNode = node.path("items");
            if (itemsNode.isArray()) {
                for (var itemNode : itemsNode) {
                    String code = itemNode.path("medicineCode").asText(null);
                    int qty = itemNode.path("quantity").asInt(-1);
                    if (code != null && qty > 0) {
                        items.add(new SaleItemRequest(code, qty));
                    }
                }
            }

            if (items.isEmpty()) {
                writeJsonObject(exchange, 400, Map.of("error", "No items in sale request"));
                return;
            }

            SaleRequest req = new SaleRequest(items);
            SaleResponse resp = salesService.makeSale(req);

            writeJsonObject(exchange, 201, Map.of(
                    "saleId", resp.getSaleId(),
                    "totalAmount", resp.getTotalAmount(),
                    "message", resp.getMessage()));

        } catch (IllegalArgumentException e) {
            writeJsonObject(exchange, 413, Map.of("error", "Request body too large"));
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonObject(exchange, 500, Map.of("error", e.getMessage()));
        }
    }
}
