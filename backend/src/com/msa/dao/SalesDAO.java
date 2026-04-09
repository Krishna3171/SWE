package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.Sales;

public class SalesDAO {

    // INSERT sales header
    public int insertSale(Connection conn,
            java.math.BigDecimal totalAmount) {
        return insertSale(conn, java.time.LocalDate.now(), totalAmount);
    }

    // INSERT sales header with explicit date
    public int insertSale(Connection conn,
            java.time.LocalDate saleDate,
            java.math.BigDecimal totalAmount) {

        String sql = """
                    INSERT INTO Sales (
                        sale_date,
                        total_amount
                    )
                    VALUES (?, ?)
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(
                        sql,
                        PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, Date.valueOf(saleDate));
            ps.setBigDecimal(2, totalAmount);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // failure
    }

    // GET sale by ID
    public Sales getSaleById(Connection conn, int saleId) {

        String sql = "SELECT * FROM Sales WHERE sale_id = ?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Sales sale = new Sales();
                sale.setSaleId(rs.getInt("sale_id"));
                sale.setSaleDate(rs.getDate("sale_date").toLocalDate());
                sale.setTotalAmount(rs.getBigDecimal("total_amount"));
                return sale;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // GET sales in a date range (reports, revenue)
    public List<Sales> getSalesInDateRange(Connection conn,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {

        List<Sales> salesList = new ArrayList<>();

        String sql = """
                    SELECT * FROM Sales
                    WHERE sale_date BETWEEN ? AND ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Sales sale = new Sales();
                sale.setSaleId(rs.getInt("sale_id"));
                sale.setSaleDate(rs.getDate("sale_date").toLocalDate());
                sale.setTotalAmount(rs.getBigDecimal("total_amount"));
                salesList.add(sale);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return salesList;
    }

    // ALIAS METHOD for profit report (same as getSalesInDateRange)
    public List<Sales> getSalesBetweenDates(Connection conn,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
        return getSalesInDateRange(conn, startDate, endDate);
    }

    // GET the most recent N sales
    public List<Sales> getRecentSales(Connection conn, int limit) {

        List<Sales> salesList = new ArrayList<>();

        String sql = """
                    SELECT * FROM Sales
                    ORDER BY sale_id DESC
                    LIMIT ?
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Sales sale = new Sales();
                sale.setSaleId(rs.getInt("sale_id"));
                sale.setSaleDate(rs.getDate("sale_date").toLocalDate());
                sale.setTotalAmount(rs.getBigDecimal("total_amount"));
                salesList.add(sale);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return salesList;
    }
}