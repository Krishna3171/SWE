package com.msa.api;

import com.msa.dao.BatchDAO;
import com.msa.dao.InventoryDAO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatchController extends BaseController implements HttpHandler {

    private final BatchDAO batchDAO;
    private final MedicineDAO medicineDAO;
    private final VendorDAO vendorDAO;
    private final InventoryDAO inventoryDAO;

    public BatchController() {
        this.batchDAO = new BatchDAO();
        this.medicineDAO = new MedicineDAO();
        this.vendorDAO = new VendorDAO();
        this.inventoryDAO = new InventoryDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, DELETE");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method)) {
            if (path.endsWith("/expired")) {
                handleGetExpiredBatches(exchange);
            } else {
                writeJson(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            Matcher m = Pattern.compile("/batches/(\\d+)$").matcher(path);
            if (m.find()) {
                handleDropBatch(exchange, Integer.parseInt(m.group(1)));
            } else {
                writeJson(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } else {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleGetExpiredBatches(HttpExchange exchange) throws IOException {
        if (!requireRole(exchange, null, "admin")) {
            return;
        }
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
                        .append("\"batchId\":").append(b.getBatchId()).append(",")
                        .append("\"medicineId\":").append(b.getMedicineId()).append(",")
                        .append("\"code\":\"").append(escapeJson(mCode)).append("\",")
                        .append("\"name\":\"").append(escapeJson(mName)).append("\",")
                        .append("\"date\":\"").append(b.getExpiryDate().toString()).append("\",")
                        .append("\"batch\":\"").append(escapeJson(b.getBatchNumber())).append("\",")
                        .append("\"qty\":").append(b.getQuantity()).append(",")
                        .append("\"vendor\":\"").append(escapeJson(vName)).append("\"")
                        .append("}");
                if (i < expired.size() - 1)
                    sb.append(",");
            }
            sb.append("]");

            writeJson(exchange, 200, sb.toString());
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleDropBatch(HttpExchange exchange, int batchId) throws IOException {
        if (!requireRole(exchange, null, "admin")) {
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            // Find the batch so we can reduce inventory after deletion
            List<Batch> allExpired = batchDAO.getExpiredBatches(conn);
            Batch target = allExpired.stream()
                    .filter(b -> b.getBatchId() == batchId)
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                writeJson(exchange, 404, "{\"error\":\"Batch not found or not expired\"}");
                return;
            }

            conn.setAutoCommit(false);
            try {
                boolean deleted = batchDAO.deleteBatch(conn, batchId);
                if (!deleted) {
                    conn.rollback();
                    writeJson(exchange, 500, "{\"error\":\"Failed to delete batch\"}");
                    return;
                }
                // Reduce inventory to reflect discarded units
                inventoryDAO.reduceQuantity(conn, target.getMedicineId(), target.getQuantity());
                conn.commit();
                writeJson(exchange, 200, "{\"message\":\"Batch dropped and inventory updated\"}");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
}
