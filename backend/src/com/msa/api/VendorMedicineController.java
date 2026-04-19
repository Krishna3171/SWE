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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                if (!requireRole(exchange, null, "admin")) return;
                handleGetLinks(exchange);
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange, 4096);
                if (!requireRole(exchange, body, "admin")) return;
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

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            // vendors array
            sb.append("\"vendors\":[");
            for (int i = 0; i < vendors.size(); i++) {
                Vendor v = vendors.get(i);
                sb.append("{")
                  .append("\"vendorId\":").append(v.getVendorId()).append(",")
                  .append("\"vendorName\":\"").append(escapeJson(v.getVendorName())).append("\"")
                  .append("}");
                if (i < vendors.size() - 1) sb.append(",");
            }
            sb.append("],");

            // medicines array
            sb.append("\"medicines\":[");
            for (int i = 0; i < medicines.size(); i++) {
                Medicine m = medicines.get(i);
                sb.append("{")
                  .append("\"medicineId\":").append(m.getMedicineId()).append(",")
                  .append("\"medicineCode\":\"").append(escapeJson(m.getMedicineCode())).append("\",")
                  .append("\"tradeName\":\"").append(escapeJson(m.getTradeName())).append("\"")
                  .append("}");
                if (i < medicines.size() - 1) sb.append(",");
            }
            sb.append("],");

            // existing links
            sb.append("\"links\":[");
            boolean firstLink = true;
            for (Medicine m : medicines) {
                List<Integer> vIds = vendorMedicineDAO.getVendorsForMedicine(conn, m.getMedicineId());
                for (int vId : vIds) {
                    if (!firstLink) sb.append(",");
                    sb.append("{")
                      .append("\"vendorId\":").append(vId).append(",")
                      .append("\"medicineId\":").append(m.getMedicineId())
                      .append("}");
                    firstLink = false;
                }
            }
            sb.append("]");

            sb.append("}");
            writeJson(exchange, 200, sb.toString());
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error\"}");
        }
    }

    private void handleCreateLink(HttpExchange exchange, String body) throws IOException {
        int vendorId = extractInt(body, "vendorId");
        int medicineId = extractInt(body, "medicineId");

        if (vendorId == -1 || medicineId == -1) {
            writeJson(exchange, 400, "{\"error\":\"vendorId and medicineId are required\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Check if link already exists
            boolean exists = vendorMedicineDAO.existsMapping(conn, vendorId, medicineId);
            if (exists) {
                writeJson(exchange, 409, "{\"error\":\"Link already exists\"}");
                return;
            }

            boolean created = vendorMedicineDAO.linkVendorToMedicine(conn, vendorId, medicineId);
            if (created) {
                writeJson(exchange, 201, "{\"message\":\"Vendor linked to medicine successfully\",\"vendorId\":" + vendorId + ",\"medicineId\":" + medicineId + "}");
            } else {
                writeJson(exchange, 500, "{\"error\":\"Failed to create link\"}");
            }
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error\"}");
        }
    }

    private int extractInt(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        return -1;
    }
}