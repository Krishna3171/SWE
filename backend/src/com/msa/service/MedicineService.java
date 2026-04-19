package com.msa.service;

import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Medicine;

import java.sql.Connection;
import java.util.List;
import java.util.function.Supplier;

public class MedicineService {

    private final MedicineDAO medicineDAO;
    private final InventoryDAO inventoryDAO;
    private final Supplier<Connection> connectionProvider;

    public MedicineService() {
        this(new MedicineDAO(), new InventoryDAO(), () -> {
            try {
                return DBConnection.getConnection();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get database connection", e);
            }
        });
    }

    public MedicineService(MedicineDAO medicineDAO, InventoryDAO inventoryDAO,
                           Supplier<Connection> connectionProvider) {
        this.medicineDAO = medicineDAO;
        this.inventoryDAO = inventoryDAO;
        this.connectionProvider = connectionProvider;
    }

    public List<Medicine> getAllMedicines() {
        try (Connection conn = connectionProvider.get()) {
            return medicineDAO.getAllMedicines(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get medicines", e);
        }
    }

    public boolean addMedicine(Medicine medicine) {
        try (Connection conn = connectionProvider.get()) {
            conn.setAutoCommit(false);
            try {
                boolean inserted = medicineDAO.insertMedicine(conn, medicine);
                if (inserted) {
                    inventoryDAO.createInventoryForMedicine(conn, medicine.getMedicineId(), 0, 10);
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
