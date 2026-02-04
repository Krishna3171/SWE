package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.PurchaseDetails;

public class PurchaseDetailsDAO {

    // INSERT purchase line item
    public boolean insertPurchaseDetail(Connection conn, PurchaseDetails detail) {

        String sql = """
            INSERT INTO Purchase_Details (
                purchase_id,
                medicine_id,
                quantity,
                unit_price,
                batch_id
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, detail.getPurchaseId());
            ps.setInt(2, detail.getMedicineId());
            ps.setInt(3, detail.getQuantity());
            ps.setBigDecimal(4, detail.getUnitPrice());
            ps.setInt(5, detail.getBatchId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET all line items for a purchase
    public List<PurchaseDetails> getPurchaseDetailsByPurchaseId(Connection conn, int purchaseId) {

        List<PurchaseDetails> details = new ArrayList<>();

        String sql = """
            SELECT * FROM Purchase_Details
            WHERE purchase_id = ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, purchaseId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PurchaseDetails detail = new PurchaseDetails();
                detail.setPurchaseId(rs.getInt("purchase_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantity(rs.getInt("quantity"));
                detail.setUnitPrice(rs.getBigDecimal("unit_price"));
                detail.setBatchId(rs.getInt("batch_id"));
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }
}
