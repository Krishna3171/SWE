package com.msa.service;

import com.msa.dao.*;
import com.msa.db.DBConnection;
import com.msa.dto.*;
import com.msa.model.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ReorderService {

        private final InventoryDAO inventoryDAO = new InventoryDAO();
        private final MedicineDAO medicineDAO = new MedicineDAO();
        private final VendorMedicineDAO vendorMedicineDAO = new VendorMedicineDAO();
        private final PurchaseDetailsDAO purchaseDetailsDAO = new PurchaseDetailsDAO();

        public ReorderReport generateReorderReport() {

                try (Connection conn = DBConnection.getConnection()) {

                        // 1️⃣ Fetch low stock medicines
                        List<Inventory> lowStockList = inventoryDAO.getLowStockMedicines(conn);

                        List<ReorderItem> reorderItems = new ArrayList<>();

                        for (Inventory inventory : lowStockList) {

                                int medicineId = inventory.getMedicineId();
                                int pendingQuantity = purchaseDetailsDAO.getPendingQuantityByMedicineId(conn,
                                                medicineId);
                                int effectiveAvailable = inventory.getQuantityAvailable() + pendingQuantity;

                                if (effectiveAvailable >= inventory.getReorderThreshold()) {
                                        continue;
                                }

                                Medicine medicine = medicineDAO.getMedicineById(conn, medicineId);
                                String medicineCode = medicine != null
                                                ? medicine.getMedicineCode()
                                                : "UNKNOWN-" + medicineId;

                                // 2️⃣ Get vendor options
                                List<Integer> vendorIds = vendorMedicineDAO.getVendorsForMedicine(
                                                conn,
                                                medicineId);

                                // 3️⃣ Decide recommended quantity
                                int recommendedQty = inventory.getReorderThreshold()
                                                - effectiveAvailable;

                                if (recommendedQty <= 0) {
                                        continue;
                                }

                                reorderItems.add(
                                                new ReorderItem(
                                                                medicineCode,
                                                                inventory.getQuantityAvailable(),
                                                                inventory.getReorderThreshold(),
                                                                recommendedQty,
                                                                vendorIds));
                        }

                        return new ReorderReport(
                                        reorderItems,
                                        reorderItems.size());

                } catch (Exception e) {
                        throw new RuntimeException("Failed to generate reorder report", e);
                }
        }
}
