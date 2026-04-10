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
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class SalesService {

        private final MedicineDAO medicineDAO;
        private final BatchDAO batchDAO;
        private final InventoryDAO inventoryDAO;
        private final SalesDAO salesDAO;
        private final SalesDetailsDAO salesDetailsDAO;
        private final Supplier<Connection> connectionProvider;

        public SalesService() {
                this(new MedicineDAO(), new BatchDAO(), new InventoryDAO(), new SalesDAO(), new SalesDetailsDAO(), () -> {
                        try {
                                return DBConnection.getConnection();
                        } catch (Exception e) {
                                throw new RuntimeException("Failed to get database connection", e);
                        }
                });
        }

        public SalesService(MedicineDAO medicineDAO, BatchDAO batchDAO, InventoryDAO inventoryDAO,
                        SalesDAO salesDAO, SalesDetailsDAO salesDetailsDAO, Supplier<Connection> connectionProvider) {
                this.medicineDAO = medicineDAO;
                this.batchDAO = batchDAO;
                this.inventoryDAO = inventoryDAO;
                this.salesDAO = salesDAO;
                this.salesDetailsDAO = salesDetailsDAO;
                this.connectionProvider = connectionProvider;
        }

        // ==========================
        // PUBLIC ENTRY POINT
        // ==========================
        public SaleResponse makeSale(SaleRequest request) {

                Connection conn = null;

                try {
                        conn = connectionProvider.get();
                        conn.setAutoCommit(false); // 🔴 TRANSACTION START

                        List<SaleLinePlan> salePlan = new ArrayList<>();
                        BigDecimal totalAmount = BigDecimal.ZERO;

                        // ==========================
                        // PHASE 1 — VALIDATE & PLAN (FEFO)
                        // ==========================
                        for (SaleItemRequest item : request.getItems()) {

                                Medicine medicine = medicineDAO.getMedicineByCode(
                                                conn,
                                                item.getMedicineCode());

                                if (medicine == null) {
                                        throw new RuntimeException(
                                                        "Medicine not found: " + item.getMedicineCode());
                                }

                                int medicineId = medicine.getMedicineId();

                                List<Batch> allBatches = batchDAO.getBatchesByMedicineId(conn, medicineId);

                                List<BatchAllocationPlan> allocations = planBatchAllocation(allBatches,
                                                item.getQuantity());

                                BigDecimal unitPrice = medicine.getUnitSellingPrice();
                                BigDecimal lineTotal = unitPrice.multiply(
                                                BigDecimal.valueOf(item.getQuantity()));

                                totalAmount = totalAmount.add(lineTotal);

                                salePlan.add(
                                                new SaleLinePlan(
                                                                medicineId,
                                                                item.getMedicineCode(),
                                                                unitPrice,
                                                                item.getQuantity(),
                                                                lineTotal,
                                                                allocations));
                        }

                        // ==========================
                        // PHASE 2 — WRITE (ALL OR NOTHING)
                        // ==========================
                        LocalDate saleDate = LocalDate.now();
                        int saleId = salesDAO.insertSale(conn, saleDate, totalAmount);

                        for (SaleLinePlan line : salePlan) {

                                SalesDetails detail = new SalesDetails(
                                                saleId,
                                                line.getMedicineId(),
                                                line.getQuantity(),
                                                line.getUnitPrice(),
                                                saleDate);

                                salesDetailsDAO.insertSalesDetail(conn, detail);

                                for (BatchAllocationPlan alloc : line.getBatchAllocationPlans()) {

                                        boolean updated = batchDAO.reduceBatchQuantity(
                                                        conn,
                                                        alloc.getBatchId(),
                                                        alloc.getQuantityToConsume());

                                        if (!updated) {
                                                throw new RuntimeException(
                                                                "Batch update failed for batch ID "
                                                                                + alloc.getBatchId());
                                        }

                                        // Delete only if the reduction left quantity at zero.
                                        // No warning is needed when rows are not deleted because stock remains.
                                        batchDAO.deleteBatchIfEmpty(conn, alloc.getBatchId());
                                }

                                // update aggregate inventory
                                inventoryDAO.reduceQuantity(
                                                conn,
                                                line.getMedicineId(),
                                                line.getQuantity());
                        }

                        conn.commit(); // 🟢 COMMIT

                        return new SaleResponse(
                                        saleId,
                                        totalAmount,
                                        "Sale completed successfully");

                } catch (Exception e) {

                        if (conn != null) {
                                try {
                                        conn.rollback(); // 🔴 FULL ROLLBACK
                                } catch (SQLException ex) {
                                        ex.printStackTrace();
                                }
                        }

                        throw new RuntimeException(
                                        "Sale failed. Transaction rolled back.",
                                        e);

                } finally {
                        if (conn != null) {
                                try {
                                        conn.setAutoCommit(true); // Reset to default
                                        conn.close();
                                } catch (SQLException e) {
                                        System.err.println("Error closing connection: " + e.getMessage());
                                        e.printStackTrace();
                                }
                        }
                }
        }

        // ==========================
        // FEFO ALLOCATION (PURE LOGIC)
        // ==========================
        private List<BatchAllocationPlan> planBatchAllocation(
                        List<Batch> batches,
                        int requiredQuantity) {

                List<BatchAllocationPlan> allocation = new ArrayList<>();
                int remaining = requiredQuantity;
                LocalDate today = LocalDate.now();

                List<Batch> validBatches = batches.stream()
                                .filter(b -> b.getQuantity() > 0)
                                .filter(b -> !b.getExpiryDate().isBefore(today))
                                .sorted(
                                                Comparator.comparing(Batch::getExpiryDate))
                                .toList();

                for (Batch batch : validBatches) {

                        if (remaining == 0)
                                break;

                        int usable = Math.min(batch.getQuantity(), remaining);

                        allocation.add(
                                        new BatchAllocationPlan(
                                                        batch.getBatchId(),
                                                        usable));

                        remaining -= usable;
                }

                if (remaining > 0) {
                        throw new RuntimeException(
                                        "Insufficient batch stock. Short by " + remaining);
                }

                return allocation;
        }
}
