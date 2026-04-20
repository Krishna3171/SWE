package com.msa.api;

import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dao.SalesDetailsDAO;
import com.msa.dao.VendorMedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Inventory;
import com.msa.model.Medicine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryController extends BaseController implements HttpHandler {

    private final InventoryDAO inventoryDAO;
    private final MedicineDAO medicineDAO;
    private final VendorMedicineDAO vendorMedicineDAO;
    private final SalesDetailsDAO salesDetailsDAO;

    public InventoryController() {
        this.inventoryDAO = new InventoryDAO();
        this.medicineDAO = new MedicineDAO();
        this.vendorMedicineDAO = new VendorMedicineDAO();
        this.salesDetailsDAO = new SalesDetailsDAO();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET");

        if (isPreflight(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {

            if (path.endsWith("/low-stock")) {
                handleGetLowStock(exchange);
            } else {
                handleGetAllInventory(exchange); // or just generic inventory mapping
            }

        } else {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleGetAllInventory(HttpExchange exchange) throws IOException {
        if (!requireAnyRole(exchange, null, "cashier", "admin")) {
            return;
        }
        // Since there is no getAllInventory in DAO directly, we'll manually fetch all
        // medicines + inventory.
        try (Connection conn = DBConnection.getConnection()) {
            List<Medicine> medicines = medicineDAO.getAllMedicines(conn);

            List<Map<String, Object>> rows = new java.util.ArrayList<>();
            for (Medicine m : medicines) {
                Inventory inv = inventoryDAO.getInventoryByMedicineId(conn, m.getMedicineId());
                int qty = inv != null ? inv.getQuantityAvailable() : 0;
                int staticThreshold = inv != null ? inv.getReorderThreshold() : 0;

                Integer avgSales = salesDetailsDAO.getAverageDailySalesLast7Days(conn, m.getMedicineId());
                int dynamicThreshold = (avgSales != null && avgSales > 0) ? avgSales : staticThreshold;

                Map<String, Object> row = new HashMap<>();
                row.put("code", m.getMedicineCode());
                row.put("tradeName", m.getTradeName());
                row.put("currentStock", qty);
                row.put("threshold", staticThreshold);
                row.put("dynamicThreshold", dynamicThreshold);
                row.put("rack", "A-10");
                rows.add(row);
            }

            writeJsonObject(exchange, 200, rows);
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error\"}");
        }
    }

    private void handleGetLowStock(HttpExchange exchange) throws IOException {
        if (!requireAnyRole(exchange, null, "admin", "cashier")) {
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            List<Inventory> lowStockList = inventoryDAO.getLowStockMedicines(conn);
            List<Map<String, Object>> rows = new java.util.ArrayList<>();

            for (Inventory inv : lowStockList) {
                Medicine m = medicineDAO.getMedicineById(conn, inv.getMedicineId());

                List<Integer> vIds = vendorMedicineDAO.getVendorsForMedicine(conn, m.getMedicineId());
                String vendorStr = "Unknown Vendor";
                if (!vIds.isEmpty()) {
                    vendorStr = "VND-" + vIds.get(0); // Simplification, get first
                }

                Map<String, Object> row = new HashMap<>();
                row.put("code", m.getMedicineCode());
                row.put("name", m.getTradeName());
                row.put("current", inv.getQuantityAvailable());
                row.put("threshold", inv.getReorderThreshold());
                row.put("toOrder", Math.max(10, inv.getReorderThreshold() - inv.getQuantityAvailable() + 20));
                row.put("vendor", vendorStr);
                rows.add(row);
            }

            writeJsonObject(exchange, 200, rows);
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error\"}");
        }
    }
}
