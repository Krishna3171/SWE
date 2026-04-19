package com.msa.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.SalesDetails;

public class SalesDetailsDAO {

    // INSERT sales line item
    public boolean insertSalesDetail(Connection conn, SalesDetails detail) {

        String sql = """
                    INSERT INTO Sales_Details (
                        sale_id,
                        medicine_id,
                        quantity_sold,
                        price,
                        sale_date
                    )
                    VALUES (?, ?, ?, ?, ?)
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, detail.getSaleId());
            ps.setInt(2, detail.getMedicineId());
            ps.setInt(3, detail.getQuantitySold());
            ps.setBigDecimal(4, detail.getPrice());
            ps.setDate(5, Date.valueOf(detail.getSaleDate()));

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET all line items for a sale
    public List<SalesDetails> getSalesDetailsBySaleId(Connection conn, int saleId) {

        List<SalesDetails> details = new ArrayList<>();

        String sql = """
                    SELECT d.sale_id,
                           d.medicine_id,
                           d.quantity_sold,
                           d.price,
                           s.sale_date
                    FROM Sales_Details d
                    JOIN Sales s ON d.sale_id = s.sale_id
                    WHERE d.sale_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                SalesDetails detail = new SalesDetails();
                detail.setSaleId(rs.getInt("sale_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantitySold(rs.getInt("quantity_sold"));
                detail.setPrice(rs.getBigDecimal("price"));
                detail.setSaleDate(rs.getDate("sale_date").toLocalDate());
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }

    // GET sales details by medicine ID (for profit calculation)
    public List<SalesDetails> getSalesDetailsByMedicineId(Connection conn, int medicineId) {

        List<SalesDetails> details = new ArrayList<>();

        String sql = """
                    SELECT d.sale_id,
                           d.medicine_id,
                           d.quantity_sold,
                           d.price,
                           s.sale_date
                    FROM Sales_Details d
                    JOIN Sales s ON d.sale_id = s.sale_id
                    WHERE d.medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                SalesDetails detail = new SalesDetails();
                detail.setSaleId(rs.getInt("sale_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantitySold(rs.getInt("quantity_sold"));
                detail.setPrice(rs.getBigDecimal("price"));
                detail.setSaleDate(rs.getDate("sale_date").toLocalDate());
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }

    // GET unit sale price (named as unitSalePrice for compatibility)
    public BigDecimal getUnitSalePrice(Connection conn, int saleId, int medicineId) {

        String sql = """
                    SELECT price FROM Sales_Details
                    WHERE sale_id = ? AND medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleId);
            ps.setInt(2, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("price");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // GET quantity for a medicine-sale combination
    public int getQuantity(Connection conn, int saleId, int medicineId) {

        String sql = """
                    SELECT quantity_sold FROM Sales_Details
                    WHERE sale_id = ? AND medicine_id = ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleId);
            ps.setInt(2, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("quantity_sold");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Alias for compatibility
    public List<SalesDetails> getSalesDetailsBySalesId(Connection conn, int saleId) {
        return getSalesDetailsBySaleId(conn, saleId);
    }

    // GET sales details by medicine ID and sale date range
    public List<SalesDetails> getSalesDetailsByMedicineIdInDateRange(
            Connection conn,
            int medicineId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {

        List<SalesDetails> details = new ArrayList<>();

        String sql = """
                    SELECT d.sale_id,
                           d.medicine_id,
                           d.quantity_sold,
                           d.price,
                           s.sale_date
                    FROM Sales_Details d
                    JOIN Sales s ON d.sale_id = s.sale_id
                    WHERE d.medicine_id = ? AND s.sale_date BETWEEN ? AND ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                SalesDetails detail = new SalesDetails();
                detail.setSaleId(rs.getInt("sale_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantitySold(rs.getInt("quantity_sold"));
                detail.setPrice(rs.getBigDecimal("price"));
                detail.setSaleDate(rs.getDate("sale_date").toLocalDate());
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }

    /**
     * Average daily sales for the current week window (last 7 days including today).
     * Returns null when no sales happened in that period.
     */
    public Integer getAverageDailySalesLast7Days(Connection conn, int medicineId) {
        String sql = """
            SELECT COALESCE(SUM(d.quantity_sold), 0) AS total_sold,
                   COUNT(DISTINCT s.sale_date) AS active_days
            FROM Sales_Details d
            JOIN Sales s ON s.sale_id = d.sale_id
            WHERE d.medicine_id = ?
              AND s.sale_date BETWEEN ? AND ?
        """;

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int activeDays = rs.getInt("active_days");
                    if (activeDays == 0) return null; // fallback to default threshold in ReorderService

                    int totalSold = rs.getInt("total_sold");
                    return (int) Math.ceil(totalSold / (double) activeDays);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate 7-day average sales", e);
        }

        return null;
    }
}