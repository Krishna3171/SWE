package com.msa.api;

import com.msa.dao.MedicineDAO;
import com.msa.dao.VendorDAO;
import com.msa.dao.VendorMedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Medicine;
import com.msa.model.Vendor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VendorMedicineController extends BaseController implements HttpHandler {

    private final VendorMedicineDAO vendorMedicineDAO;
    private final VendorDAO vendorDAO;
    private final MedicineDAO medicineDAO;

    public VendorMedicineController() {
        this.vendorMedicineDAO = new VendorMedicineDAO();
        this.vendorDAO = new VendorDAO();
        this.medicineDAO = new MedicineDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("GET".equals(method)) {
                if (!requireRole(exchange, null, "admin"))
                    return;
                handleGetLinks(exchange);
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange, 4096);
                if (!requireRole(exchange, body, "admin"))
                    return;
                handleCreateLink(exchange, body);
            } else {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleGetLinks(HttpExchange exchange) throws IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Vendor> vendors = vendorDAO.getAllVendors(conn);
            List<Medicine> medicines = medicineDAO.getAllMedicines(conn);

            List<Map<String, Object>> vendorRows = new java.util.ArrayList<>();
            for (Vendor v : vendors) {
                vendorRows.add(Map.of(
                        "vendorId", v.getVendorId(),
                        "vendorName", v.getVendorName()));
            }

            List<Map<String, Object>> medicineRows = new java.util.ArrayList<>();
            for (Medicine m : medicines) {
                medicineRows.add(Map.of(
                        "medicineId", m.getMedicineId(),
                        "medicineCode", m.getMedicineCode(),
                        "tradeName", m.getTradeName()));
            }

            List<Map<String, Object>> links = new java.util.ArrayList<>();
            for (Medicine m : medicines) {
                List<Integer> vIds = vendorMedicineDAO.getVendorsForMedicine(conn, m.getMedicineId());
                for (int vId : vIds) {
                    Map<String, Object> link = new HashMap<>();
                    link.put("vendorId", vId);
                    link.put("medicineId", m.getMedicineId());
                    links.add(link);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("vendors", vendorRows);
            response.put("medicines", medicineRows);
            response.put("links", links);

            writeJsonObject(exchange, 200, response);
        } catch (SQLException e) {
            writeJsonObject(exchange, 500, Map.of("error", "Database error"));
        }
    }

    private void handleCreateLink(HttpExchange exchange, String body) throws IOException {
        var node = parseJson(body);
        int vendorId = node.path("vendorId").asInt(-1);
        int medicineId = node.path("medicineId").asInt(-1);

        if (vendorId == -1 || medicineId == -1) {
            writeJsonObject(exchange, 400, Map.of("error", "vendorId and medicineId are required"));
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Check if link already exists
            boolean exists = vendorMedicineDAO.existsMapping(conn, vendorId, medicineId);
            if (exists) {
                writeJsonObject(exchange, 409, Map.of("error", "Link already exists"));
                return;
            }

            boolean created = vendorMedicineDAO.linkVendorToMedicine(conn, vendorId, medicineId);
            if (created) {
                writeJsonObject(exchange, 201, Map.of(
                        "message", "Vendor linked to medicine successfully",
                        "vendorId", vendorId,
                        "medicineId", medicineId));
            } else {
                writeJsonObject(exchange, 500, Map.of("error", "Failed to create link"));
            }
        } catch (SQLException e) {
            writeJsonObject(exchange, 500, Map.of("error", "Database error"));
        }
    }
}