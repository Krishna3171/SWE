package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.msa.db.DBConnection;
import com.msa.model.Sales;

public class SalesDAO {

    // INSERT sales header
    public boolean insertSale(Sales sale) {

        String sql = """
            INSERT INTO Sales (
                sale_date,
                total_amount
            )
            VALUES (?, ?)
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                sql,
                PreparedStatement.RETURN_GENERATED_KEYS
            )
        ) {

            ps.setDate(1, Date.valueOf(sale.getSaleDate()));
            ps.setBigDecimal(2, sale.getTotalAmount());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    sale.setSaleId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET sale by ID
    public Sales getSaleById(int saleId) {

        String sql = "SELECT * FROM Sales WHERE sale_id = ?";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

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
    public List<Sales> getSalesInDateRange(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {

        List<Sales> salesList = new ArrayList<>();

        String sql = """
            SELECT * FROM Sales
            WHERE sale_date BETWEEN ? AND ?
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

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
}
