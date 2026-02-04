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

public class SalesService {

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final SalesDAO salesDAO = new SalesDAO();
    private final SalesDetailsDAO salesDetailsDAO = new SalesDetailsDAO();

    // ==========================
    // PUBLIC ENTRY POINT
    // ==========================
    public SaleResponse makeSale(SaleRequest request) {

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 🔴 TRANSACTION START

            List<SaleLinePlan> salePlan = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            // ==========================
            // PHASE 1 — VALIDATE & PLAN (FEFO)
            // ==========================
            for (SaleItemRequest item : request.getItems()) {

                Medicine medicine =
                        medicineDAO.getMedicineByCode(
                                conn,
                                item.getMedicineCode()
                        );

                if (medicine == null) {
                    throw new RuntimeException(
                            "Medicine not found: " + item.getMedicineCode()
                    );
                }

                int medicineId = medicine.getMedicineId();

                List<Batch> allBatches =
                        batchDAO.getBatchesByMedicineId(conn, medicineId);

                List<BatchAllocationPlan> allocations =
                        planBatchAllocation(allBatches, item.getQuantity());

                BigDecimal unitPrice = medicine.getUnitSellingPrice();
                BigDecimal lineTotal =
                        unitPrice.multiply(
                                BigDecimal.valueOf(item.getQuantity())
                        );

                totalAmount = totalAmount.add(lineTotal);

                salePlan.add(
                        new SaleLinePlan(
                                medicineId,
                                item.getMedicineCode(),
                                unitPrice,
                                item.getQuantity(),
                                lineTotal,
                                allocations
                        )
                );
            }

            // ==========================
            // PHASE 2 — WRITE (ALL OR NOTHING)
            // ==========================
            int saleId =
                    salesDAO.insertSale(conn, totalAmount);

            for (SaleLinePlan line : salePlan) {

                SalesDetails detail =
                        new SalesDetails(
                                saleId,
                                line.getMedicineId(),
                                line.getQuantity(),
                                line.getUnitPrice()
                        );

                salesDetailsDAO.insertSalesDetail(conn, detail);

                for (BatchAllocationPlan alloc : line.getBatchAllocationPlans()) {

                    boolean updated =
                            batchDAO.reduceBatchQuantity(
                                    conn,
                                    alloc.getBatchId(),
                                    alloc.getQuantityToConsume()
                            );

                    if (!updated) {
                        throw new RuntimeException(
                                "Batch update failed for batch ID "
                                        + alloc.getBatchId()
                        );
                    }

                    // delete empty batch (safe inside txn)
                    boolean deleted = batchDAO.deleteBatch(conn, alloc.getBatchId());
                    if (!deleted) {
                        System.err.println("Warning: Failed to delete empty batch ID " + alloc.getBatchId());
                    }
                }

                // update aggregate inventory
                inventoryDAO.reduceQuantity(
                        conn,
                        line.getMedicineId(),
                        line.getQuantity()
                );
            }

            conn.commit(); // 🟢 COMMIT

            return new SaleResponse(
                    saleId,
                    totalAmount,
                    "Sale completed successfully"
            );

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
                    e
            );

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
            int requiredQuantity
    ) {

        List<BatchAllocationPlan> allocation = new ArrayList<>();
        int remaining = requiredQuantity;
        LocalDate today = LocalDate.now();

        List<Batch> validBatches =
                batches.stream()
                        .filter(b -> b.getQuantity() > 0)
                        .filter(b -> !b.getExpiryDate().isBefore(today))
                        .sorted(
                                Comparator.comparing(Batch::getExpiryDate)
                        )
                        .toList();

        for (Batch batch : validBatches) {

            if (remaining == 0) break;

            int usable =
                    Math.min(batch.getQuantity(), remaining);

            allocation.add(
                    new BatchAllocationPlan(
                            batch.getBatchId(),
                            usable
                    )
            );

            remaining -= usable;
        }

        if (remaining > 0) {
            throw new RuntimeException(
                    "Insufficient batch stock. Short by " + remaining
            );
        }

        return allocation;
    }
}
