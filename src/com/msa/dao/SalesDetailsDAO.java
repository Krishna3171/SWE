package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                price
            )
            VALUES (?, ?, ?, ?)
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, detail.getSaleId());
            ps.setInt(2, detail.getMedicineId());
            ps.setInt(3, detail.getQuantitySold());
            ps.setBigDecimal(4, detail.getPrice());

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
            SELECT * FROM Sales_Details
            WHERE sale_id = ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, saleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                SalesDetails detail = new SalesDetails();
                detail.setSaleId(rs.getInt("sale_id"));
                detail.setMedicineId(rs.getInt("medicine_id"));
                detail.setQuantitySold(rs.getInt("quantity_sold"));
                detail.setPrice(rs.getBigDecimal("price"));
                details.add(detail);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }
}
