package com.msa.service;

import com.msa.dao.*;
import com.msa.db.DBConnection;
import com.msa.dto.*;
import com.msa.model.Batch;
import com.msa.model.Medicine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExpiredBatchDiscardService {

    private final BatchDAO batchDAO = new BatchDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    public ExpiredBatchReport discardExpiredBatches() {

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            List<Batch> expiredBatches =
                    batchDAO.getExpiredBatches(conn);

            List<ExpiredBatchReportItem> reportItems = new ArrayList<>();

            int totalBatches = 0;
            int totalUnits = 0;

            for (Batch batch : expiredBatches) {

                Medicine medicine =
                        medicineDAO.getMedicineById(
                                conn,
                                batch.getMedicineId()
                        );

                // 1️⃣ Update inventory
                inventoryDAO.reduceQuantity(
                        conn,
                        batch.getMedicineId(),
                        batch.getQuantity()
                );

                // 2️⃣ Delete batch
                batchDAO.deleteBatch(conn, batch.getBatchId());

                // 3️⃣ Add report entry
                reportItems.add(
                        new ExpiredBatchReportItem(
                                medicine.getMedicineCode(),
                                batch.getBatchNumber(),
                                batch.getExpiryDate(),
                                batch.getQuantity(),
                                batch.getVendorId()
                        )
                );

                totalBatches++;
                totalUnits += batch.getQuantity();
            }

            conn.commit();

            return new ExpiredBatchReport(
                    reportItems,
                    totalBatches,
                    totalUnits
            );

        } catch (Exception e) {

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            throw new RuntimeException(
                    "Expired batch discard failed",
                    e
            );

        } finally {
            if (conn != null) {
                try { conn.close(); }
                catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
