package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.Purchase;

public class PurchaseDAO {

    // INSERT purchase header
    public int insertPurchase(Connection conn, Purchase purchase) {

        String sql = """
            INSERT INTO Purchase (
                purchase_date,
                vendor_id,
                total_amount
            )
            VALUES (?, ?, ?)
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {

            ps.setDate(1, Date.valueOf(purchase.getPurchaseDate()));
            ps.setInt(2, purchase.getVendorId());
            ps.setBigDecimal(3, purchase.getTotalAmount());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    purchase.setPurchaseId(rs.getInt(1));
                }
                return purchase.getPurchaseId();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // failure
    }

    // GET purchase by ID
    public Purchase getPurchaseById(Connection conn, int purchaseId) {

        String sql = "SELECT * FROM Purchase WHERE purchase_id = ?";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, purchaseId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Purchase purchase = new Purchase();
                purchase.setPurchaseId(rs.getInt("purchase_id"));
                purchase.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
                purchase.setVendorId(rs.getInt("vendor_id"));
                purchase.setTotalAmount(rs.getBigDecimal("total_amount"));
                return purchase;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // GET purchases in a date range (reports)
    public List<Purchase> getPurchasesInDateRange(Connection conn,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {

        List<Purchase> purchases = new ArrayList<>();

        String sql = """
            SELECT * FROM Purchase
            WHERE purchase_date BETWEEN ? AND ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Purchase purchase = new Purchase();
                purchase.setPurchaseId(rs.getInt("purchase_id"));
                purchase.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
                purchase.setVendorId(rs.getInt("vendor_id"));
                purchase.setTotalAmount(rs.getBigDecimal("total_amount"));
                purchases.add(purchase);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return purchases;
    }
}
