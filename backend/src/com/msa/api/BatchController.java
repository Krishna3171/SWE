package com.msa.api;

import com.msa.dao.BatchDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dao.VendorDAO;
import com.msa.db.DBConnection;
import com.msa.model.Batch;
import com.msa.model.Medicine;
import com.msa.model.Vendor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BatchController extends BaseController implements HttpHandler {

    private final BatchDAO batchDAO;
    private final MedicineDAO medicineDAO;
    private final VendorDAO vendorDAO;

    public BatchController() {
        this.batchDAO = new BatchDAO();
        this.medicineDAO = new MedicineDAO();
        this.vendorDAO = new VendorDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET");

        if (isPreflight(exchange)) {
            return;
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/expired")) {
                handleGetExpiredBatches(exchange);
            } else {
                writeJson(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } else {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleGetExpiredBatches(HttpExchange exchange) throws IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Batch> expired = batchDAO.getExpiredBatches(conn);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            
            for (int i = 0; i < expired.size(); i++) {
                Batch b = expired.get(i);
                Medicine m = medicineDAO.getMedicineById(conn, b.getMedicineId());
                Vendor v = vendorDAO.getVendorById(conn, b.getVendorId());
                
                String mCode = m != null ? m.getMedicineCode() : "N/A";
                String mName = m != null ? m.getTradeName() : "Unknown";
                String vName = v != null ? "VND-" + v.getVendorId() + " - " + v.getVendorName() : "Unknown";

                sb.append("{")
                  .append("\"code\":\"").append(escapeJson(mCode)).append("\",")
                  .append("\"name\":\"").append(escapeJson(mName)).append("\",")
                  .append("\"date\":\"").append(b.getExpiryDate().toString()).append("\",")
                  .append("\"batch\":\"").append(escapeJson(b.getBatchNumber())).append("\",")
                  .append("\"qty\":").append(b.getQuantity()).append(",")
                  .append("\"vendor\":\"").append(escapeJson(vName)).append("\"")
                  .append("}");
                if (i < expired.size() - 1) sb.append(",");
            }
            sb.append("]");
            
            writeJson(exchange, 200, sb.toString());
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
}
