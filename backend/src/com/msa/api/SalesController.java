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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            List<Sales> recentSales = salesDAO.getRecentSales(conn, 20);

            StringBuilder json = new StringBuilder();
            json.append("[");

            for (int i = 0; i < recentSales.size(); i++) {
                Sales sale = recentSales.get(i);

                // Get line-item details for this sale
                List<SalesDetails> details = salesDetailsDAO.getSalesDetailsBySaleId(conn, sale.getSaleId());

                json.append("{");
                json.append("\"saleId\":").append(sale.getSaleId()).append(",");
                json.append("\"saleDate\":\"").append(sale.getSaleDate()).append("\",");
                json.append("\"totalAmount\":").append(sale.getTotalAmount()).append(",");

                // Build items array
                json.append("\"items\":[");
                for (int j = 0; j < details.size(); j++) {
                    SalesDetails d = details.get(j);
                    Medicine med = medicineDAO.getMedicineById(conn, d.getMedicineId());
                    String medName = med != null ? escapeJson(med.getMedicineName()) : "Unknown";
                    String medCode = med != null ? escapeJson(med.getMedicineCode()) : "N/A";

                    json.append("{");
                    json.append("\"medicineId\":").append(d.getMedicineId()).append(",");
                    json.append("\"medicineName\":\"").append(medName).append("\",");
                    json.append("\"medicineCode\":\"").append(medCode).append("\",");
                    json.append("\"quantity\":").append(d.getQuantitySold()).append(",");
                    json.append("\"unitPrice\":").append(d.getPrice());
                    json.append("}");
                    if (j < details.size() - 1) json.append(",");
                }
                json.append("]");

                json.append("}");
                if (i < recentSales.size() - 1) json.append(",");
            }

            json.append("]");

            writeJson(exchange, 200, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void handleMakeSale(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
            
            // Extract items logic: 
            // format: "medicineCode":"xyz", "quantity":5
            List<SaleItemRequest> items = new ArrayList<>();
            
            Pattern pattern = Pattern.compile("\\\"medicineCode\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"quantity\\\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(body);
            while (matcher.find()) {
                String code = matcher.group(1);
                int qty = Integer.parseInt(matcher.group(2));
                items.add(new SaleItemRequest(code, qty));
            }

            if (items.isEmpty()) {
                writeJson(exchange, 400, "{\"error\":\"No items in sale request\"}");
                return;
            }

            SaleRequest req = new SaleRequest(items);
            SaleResponse resp = salesService.makeSale(req);

            String json = "{"
                    + "\"saleId\":" + resp.getSaleId() + ","
                    + "\"totalAmount\":" + resp.getTotalAmount() + ","
                    + "\"message\":\"" + escapeJson(resp.getMessage()) + "\""
                    + "}";

            writeJson(exchange, 201, json);

        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
}
