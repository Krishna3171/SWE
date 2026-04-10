package com.msa.service;

import com.msa.dao.*;
import com.msa.db.DBConnection;
import com.msa.dto.*;
import com.msa.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PurchaseService {

        private final VendorDAO vendorDAO;
        private final MedicineDAO medicineDAO;
        private final VendorMedicineDAO vendorMedicineDAO;
        private final PurchaseDAO purchaseDAO;
        private final PurchaseDetailsDAO purchaseDetailsDAO;
        private final BatchDAO batchDAO;
        private final InventoryDAO inventoryDAO;
        private final Supplier<Connection> connectionProvider;

        public PurchaseService() {
                this(new VendorDAO(), new MedicineDAO(), new VendorMedicineDAO(),
                                new PurchaseDAO(), new PurchaseDetailsDAO(), new BatchDAO(), new InventoryDAO(), () -> {
                                        try {
                                                return DBConnection.getConnection();
                                        } catch (Exception e) {
                                                throw new RuntimeException("Failed to get database connection", e);
                                        }
                                });
        }

        public PurchaseService(VendorDAO vendorDAO, MedicineDAO medicineDAO,
                        VendorMedicineDAO vendorMedicineDAO, PurchaseDAO purchaseDAO,
                        PurchaseDetailsDAO purchaseDetailsDAO, BatchDAO batchDAO,
                        InventoryDAO inventoryDAO, Supplier<Connection> connectionProvider) {
                this.vendorDAO = vendorDAO;
                this.medicineDAO = medicineDAO;
                this.vendorMedicineDAO = vendorMedicineDAO;
                this.purchaseDAO = purchaseDAO;
                this.purchaseDetailsDAO = purchaseDetailsDAO;
                this.batchDAO = batchDAO;
                this.inventoryDAO = inventoryDAO;
                this.connectionProvider = connectionProvider;
        }

        // ==========================
        // PUBLIC ENTRY POINT
        // ==========================
        public PurchaseResponse makePurchase(PurchaseRequest request) {

                Connection conn = null;

                try {
                        conn = connectionProvider.get();
                        conn.setAutoCommit(false); // 🔴 TRANSACTION START

                        // ==========================
                        // PHASE 1 — VALIDATION
                        // ==========================

                        // 1️⃣ Validate vendor
                        Vendor vendor = vendorDAO.getVendorById(
                                        conn,
                                        request.getVendorId());

                        if (vendor == null) {
                                throw new RuntimeException(
                                                "Vendor not found: " + request.getVendorId());
                        }

                        BigDecimal totalAmount = BigDecimal.ZERO;

                        // We store resolved data to avoid re-querying later
                        List<ResolvedPurchaseItem> resolvedItems = new ArrayList<>();

                        for (PurchaseItemRequest item : request.getItems()) {

                                // 2️⃣ Validate medicine
                                Medicine medicine = medicineDAO.getMedicineByCode(
                                                conn,
                                                item.getMedicineCode());

                                if (medicine == null) {
                                        throw new RuntimeException(
                                                        "Medicine not found: " + item.getMedicineCode());
                                }

                                int medicineId = medicine.getMedicineId();

                                // 3️⃣ Validate vendor supplies medicine
                                boolean supplied = vendorMedicineDAO.existsMapping(
                                                conn,
                                                request.getVendorId(),
                                                medicineId);

                                if (!supplied) {
                                        throw new RuntimeException(
                                                        "Vendor does not supply medicine: "
                                                                        + item.getMedicineCode());
                                }

                                // 4️⃣ Validate quantity
                                if (item.getQuantity() <= 0) {
                                        throw new RuntimeException(
                                                        "Invalid quantity for medicine: "
                                                                        + item.getMedicineCode());
                                }

                                // 5️⃣ Validate expiry
                                if (!LocalDate.parse(item.getExpiryDate()).isAfter(LocalDate.now())) {
                                        throw new RuntimeException(
                                                        "Invalid expiry date for medicine: "
                                                                        + item.getMedicineCode());
                                }

                                // 6️⃣ Validate price
                                if (item.getUnitPurchasePrice()
                                                .compareTo(BigDecimal.ZERO) <= 0) {
                                        throw new RuntimeException(
                                                        "Invalid purchase price for medicine: "
                                                                        + item.getMedicineCode());
                                }

                                BigDecimal lineTotal = item.getUnitPurchasePrice()
                                                .multiply(
                                                                BigDecimal.valueOf(item.getQuantity()));

                                totalAmount = totalAmount.add(lineTotal);

                                resolvedItems.add(
                                                new ResolvedPurchaseItem(
                                                                medicineId,
                                                                item));
                        }

                        // ==========================
                        // PHASE 2 — WRITE (ALL OR NOTHING)
                        // ==========================
                        LocalDate purchaseDate = LocalDate.now();
                        Purchase purchase = new Purchase(
                                        purchaseDate,
                                        request.getVendorId(),

                                        totalAmount);

                        // 1️⃣ Insert purchase header
                        int purchaseId = purchaseDAO.insertPurchase(
                                        conn,
                                        purchase);

                        // 2️⃣ Insert details, batches, inventory
                        for (ResolvedPurchaseItem resolved : resolvedItems) {

                                PurchaseItemRequest item = resolved.request;

                                // a) Batch
                                Batch batch = new Batch();
                                batch.setMedicineId(resolved.medicineId);
                                batch.setBatchNumber(item.getBatchNumber());
                                batch.setExpiryDate(LocalDate.parse(item.getExpiryDate()));
                                batch.setQuantity(item.getQuantity());
                                batch.setVendorId(request.getVendorId());

                                int batch_id = batchDAO.insertBatch(conn, batch);

                                PurchaseDetails detail = new PurchaseDetails(
                                                purchaseId,
                                                resolved.medicineId,
                                                item.getQuantity(),
                                                item.getUnitPurchasePrice(),
                                                batch_id, // will be set after batch insert
                                                purchaseDate);

                                // b) Purchase_Details
                                purchaseDetailsDAO.insertPurchaseDetail(
                                                conn,
                                                detail);

                                // c) Inventory update (aggregate)
                                boolean success = inventoryDAO.addQuantity(
                                                conn,
                                                resolved.medicineId,
                                                item.getQuantity());

                                if (!success) {
                                        // inventory record doesn't exist, create it reorder threshold 0
                                        inventoryDAO.createInventoryForMedicine(conn, resolved.medicineId,
                                                        item.getQuantity(), 10);
                                }
                        }

                        conn.commit(); // 🟢 COMMIT

                        return new PurchaseResponse(
                                        purchaseId,
                                        totalAmount,
                                        "Purchase completed successfully");

                } catch (Exception e) {

                        if (conn != null) {
                                try {
                                        conn.rollback(); // 🔴 FULL ROLLBACK
                                } catch (SQLException ex) {
                                        ex.printStackTrace();
                                }
                        }

                        throw new RuntimeException(
                                        "Purchase failed. Transaction rolled back.",
                                        e);

                } finally {
                        if (conn != null) {
                                try {
                                        conn.close();
                                } catch (SQLException e) {
                                        e.printStackTrace();
                                }
                        }
                }
        }

        // ==========================
        // INTERNAL HELPER (SERVICE ONLY)
        // ==========================
        private static class ResolvedPurchaseItem {

                private final int medicineId;
                private final PurchaseItemRequest request;

                ResolvedPurchaseItem(int medicineId,
                                PurchaseItemRequest request) {
                        this.medicineId = medicineId;
                        this.request = request;
                }
        }
}
