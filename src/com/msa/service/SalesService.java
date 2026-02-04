package com.msa.service;

import com.msa.dao.*;
import com.msa.db.DBConnection;
import com.msa.dto.*;
import com.msa.model.Medicine;
import com.msa.model.Inventory;
import com.msa.model.SalesDetails;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SalesService {

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final SalesDAO salesDAO = new SalesDAO();
    private final SalesDetailsDAO salesDetailsDAO = new SalesDetailsDAO();

    public SaleResponse makeSale(SaleRequest request) {

        Connection connection = null;

        try {
            // 1️⃣ START TRANSACTION
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            List<SaleLinePlan> salePlan = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            // 2️⃣ PHASE 1 — VALIDATE EVERYTHING
            for (SaleItemRequest item : request.getItems()) {

                // a) resolve medicine (business → technical)
                Medicine medicine = medicineDAO.getMedicineByCode(
                        connection,
                        item.getMedicineCode()
                );

                if (medicine == null) {
                    throw new RuntimeException(
                        "Medicine not found: " + item.getMedicineCode()
                    );
                }

                int medicineId = medicine.getMedicineId();

                // b) check inventory
                Inventory inventory = inventoryDAO.getInventoryByMedicineId(
                        connection,
                        medicineId
                );

                if (inventory.getQuantityAvailable() < item.getQuantity()) {
                    throw new RuntimeException(
                        "Insufficient stock for " + item.getMedicineCode()
                    );
                }

                // c) calculate price
                BigDecimal unitPrice = medicine.getUnitSellingPrice();
                BigDecimal lineTotal = unitPrice.multiply(
                        BigDecimal.valueOf(item.getQuantity())
                );

                totalAmount = totalAmount.add(lineTotal);

                // d) freeze validated decision
                salePlan.add(new SaleLinePlan(
                        medicineId,
                        item.getMedicineCode(),
                        unitPrice,
                        item.getQuantity(),
                        lineTotal
                ));
            }

            // 3️⃣ PHASE 2 — WRITE (ALL OR NOTHING)

            // a) insert sales header
            int saleId = salesDAO.insertSale(
                    connection,
                    totalAmount
            );

            // b) insert details + update inventory
            for (SaleLinePlan line : salePlan) {

                SalesDetails detail=new SalesDetails(saleId,
                        line.getMedicineId(),
                        line.getQuantity(),
                        line.getUnitPrice());
                salesDetailsDAO.insertSalesDetail(
                        connection,
                        detail
                );

                inventoryDAO.reduceQuantity(
                        connection,
                        line.getMedicineId(),
                        line.getQuantity()
                );
            }

            // 4️⃣ COMMIT
            connection.commit();

            return new SaleResponse(
                    saleId,
                    totalAmount,
                    "Sale completed successfully"
            );

        } catch (Exception e) {

            // 5️⃣ ROLLBACK ON ANY FAILURE
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            throw new RuntimeException("Sale failed. Rolled back.", e);

        } finally {

            // 6️⃣ CLEANUP
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
