package com.msa.service;

import com.msa.dao.BatchDAO;
import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Batch;
import com.msa.model.Medicine;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public class MedicineService {

    private final MedicineDAO medicineDAO;
    private final InventoryDAO inventoryDAO;
    private final BatchDAO batchDAO;

    public MedicineService() {
        this.medicineDAO = new MedicineDAO();
        this.inventoryDAO = new InventoryDAO();
        this.batchDAO = new BatchDAO();
    }

    public List<Medicine> getAllMedicines() {
        try (Connection conn = DBConnection.getConnection()) {
            return medicineDAO.getAllMedicines(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get medicines", e);
        }
    }

    /**
     * Adds a new medicine to catalog, creates inventory record,
     * and if initial stock > 0, also creates a proper Batch with expiry date
     * so FEFO logic works from day one.
     */
    public boolean addMedicine(Medicine medicine, int initialQuantity,
                               int reorderThreshold, String expiryDate, int vendorId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean inserted = medicineDAO.insertMedicine(conn, medicine);
                if (inserted) {
                    inventoryDAO.createInventoryForMedicine(
                            conn, medicine.getMedicineId(), initialQuantity, reorderThreshold);

                    // Create a real Batch so FEFO can function on these units
                    if (initialQuantity > 0 && expiryDate != null && !expiryDate.isEmpty()) {
                        Batch batch = new Batch();
                        batch.setMedicineId(medicine.getMedicineId());
                        batch.setBatchNumber("INIT-" + medicine.getMedicineCode());
                        batch.setExpiryDate(LocalDate.parse(expiryDate));
                        batch.setQuantity(initialQuantity);
                        batch.setVendorId(vendorId > 0 ? vendorId : 0);
                        batchDAO.insertBatch(conn, batch);
                    }
                }
                conn.commit();
                return inserted;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add medicine", e);
        }
    }
}
