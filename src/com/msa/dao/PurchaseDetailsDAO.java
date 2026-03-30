package com.msa.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

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
                        batch_id,
                        purchase_date
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, detail.getPurchaseId());
            ps.setInt(2, detail.getMedicineId());
            ps.setInt(3, detail.getQuantity());
            ps.setBigDecimal(4, detail.getUnitPrice());
            ps.setInt(5, detail.getBatchId());
            ps.setDate(6, Date.valueOf(detail.getPurchaseDate()));

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
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PurchaseDetails detail = new PurchaseDetails();
                detail.setPurchaseId(rs.getInt("purchase_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantity(rs.getInt("quantity"));
                detail.setUnitPrice(rs.getBigDecimal("unit_price"));
                detail.setBatchId(rs.getInt("batch_id"));
                detail.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }

    // GET purchase details by medicine ID (for profit calculation)
    public List<PurchaseDetails> getPurchaseDetailsByMedicineId(Connection conn, int medicineId) {

        List<PurchaseDetails> details = new ArrayList<>();

        String sql = """
                    SELECT * FROM Purchase_Details
                    WHERE medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PurchaseDetails detail = new PurchaseDetails();
                detail.setPurchaseId(rs.getInt("purchase_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantity(rs.getInt("quantity"));
                detail.setUnitPrice(rs.getBigDecimal("unit_price"));
                detail.setBatchId(rs.getInt("batch_id"));
                detail.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }

    // GET average purchase price for a medicine (for profit margin calculation)
    public BigDecimal getAveragePurchasePriceForMedicine(Connection conn, int medicineId) {

        String sql = """
                    SELECT AVG(unit_price) as avg_price
                    FROM Purchase_Details
                    WHERE medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("avg_price");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // GET unit purchase price
    public BigDecimal getUnitPurchasePrice(Connection conn, int purchaseId, int medicineId) {

        String sql = """
                    SELECT unit_price FROM Purchase_Details
                    WHERE purchase_id = ? AND medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseId);
            ps.setInt(2, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("unit_price");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // GET quantity for a medicine
    public int getQuantity(Connection conn, int purchaseId, int medicineId) {

        String sql = """
                    SELECT quantity FROM Purchase_Details
                    WHERE purchase_id = ? AND medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseId);
            ps.setInt(2, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("quantity");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // GET purchase details by medicine ID and purchase date range
    public List<PurchaseDetails> getPurchaseDetailsByMedicineIdInDateRange(
            Connection conn,
            int medicineId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {

        List<PurchaseDetails> details = new ArrayList<>();

        String sql = """
                    SELECT * FROM Purchase_Details
                    WHERE medicine_id = ? AND purchase_date BETWEEN ? AND ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PurchaseDetails detail = new PurchaseDetails();
                detail.setPurchaseId(rs.getInt("purchase_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantity(rs.getInt("quantity"));
                detail.setUnitPrice(rs.getBigDecimal("unit_price"));
                detail.setBatchId(rs.getInt("batch_id"));
                detail.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }
}