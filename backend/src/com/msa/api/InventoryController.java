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
import java.util.List;

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

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < medicines.size(); i++) {
                Medicine m = medicines.get(i);
                Inventory inv = inventoryDAO.getInventoryByMedicineId(conn, m.getMedicineId());
                int qty = inv != null ? inv.getQuantityAvailable() : 0;
                int staticThreshold = inv != null ? inv.getReorderThreshold() : 0;

                Integer avgSales = salesDetailsDAO.getAverageDailySalesLast7Days(conn, m.getMedicineId());
                int dynamicThreshold = (avgSales != null && avgSales > 0) ? avgSales : staticThreshold;

                sb.append("{")
                        .append("\"code\":\"").append(escapeJson(m.getMedicineCode())).append("\",")
                        .append("\"tradeName\":\"").append(escapeJson(m.getTradeName())).append("\",")
                        .append("\"currentStock\":").append(qty).append(",")
                        .append("\"threshold\":").append(staticThreshold).append(",")
                        .append("\"dynamicThreshold\":").append(dynamicThreshold).append(",")
                        .append("\"rack\":\"A-10\"")
                        .append("}");
                if (i < medicines.size() - 1)
                    sb.append(",");
            }
            sb.append("]");

            writeJson(exchange, 200, sb.toString());
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
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for (int i = 0; i < lowStockList.size(); i++) {
                Inventory inv = lowStockList.get(i);
                Medicine m = medicineDAO.getMedicineById(conn, inv.getMedicineId());

                List<Integer> vIds = vendorMedicineDAO.getVendorsForMedicine(conn, m.getMedicineId());
                String vendorStr = "Unknown Vendor";
                if (!vIds.isEmpty()) {
                    vendorStr = "VND-" + vIds.get(0); // Simplification, get first
                }

                sb.append("{")
                        .append("\"code\":\"").append(escapeJson(m.getMedicineCode())).append("\",")
                        .append("\"name\":\"").append(escapeJson(m.getTradeName())).append("\",")
                        .append("\"current\":").append(inv.getQuantityAvailable()).append(",")
                        .append("\"threshold\":").append(inv.getReorderThreshold()).append(",")
                        .append("\"toOrder\":")
                        .append(Math.max(10, inv.getReorderThreshold() - inv.getQuantityAvailable() + 20)).append(",")
                        .append("\"vendor\":\"").append(vendorStr).append("\"")
                        .append("}");
                if (i < lowStockList.size() - 1)
                    sb.append(",");
            }
            sb.append("]");

            writeJson(exchange, 200, sb.toString());
        } catch (SQLException e) {
            writeJson(exchange, 500, "{\"error\":\"Database error\"}");
        }
    }
}
