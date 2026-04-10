package com.msa.service;

import com.msa.dao.*;
import com.msa.db.DBConnection;
import com.msa.dto.*;
import com.msa.model.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ReorderService {

        private final InventoryDAO inventoryDAO;
        private final MedicineDAO medicineDAO;
        private final VendorMedicineDAO vendorMedicineDAO;
        private final Supplier<Connection> connectionProvider;

        public ReorderService() {
                this(new InventoryDAO(), new MedicineDAO(), new VendorMedicineDAO(), () -> {
                        try {
                                return DBConnection.getConnection();
                        } catch (Exception e) {
                                throw new RuntimeException("Failed to get database connection", e);
                        }
                });
        }

        public ReorderService(InventoryDAO inventoryDAO, MedicineDAO medicineDAO,
                        VendorMedicineDAO vendorMedicineDAO, Supplier<Connection> connectionProvider) {
                this.inventoryDAO = inventoryDAO;
                this.medicineDAO = medicineDAO;
                this.vendorMedicineDAO = vendorMedicineDAO;
                this.connectionProvider = connectionProvider;
        }

        public ReorderReport generateReorderReport() {

                try (Connection conn = connectionProvider.get()) {

                        // 1️⃣ Fetch low stock medicines
                        List<Inventory> lowStockList = inventoryDAO.getLowStockMedicines(conn);

                        List<ReorderItem> reorderItems = new ArrayList<>();

                        for (Inventory inventory : lowStockList) {

                                int medicineId = inventory.getMedicineId();

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
                                                - inventory.getQuantityAvailable();

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
