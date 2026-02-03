package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.msa.db.DBConnection;
import com.msa.model.Inventory;

public class InventoryDAO {

    // CREATE inventory row for a new medicine
    public boolean createInventoryForMedicine(int medicineId, int quantity, int reorderThreshold) {

        String sql = """
            INSERT INTO Inventory (medicine_id, quantity_available, reorder_threshold)
            VALUES (?, ?, ?)
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, medicineId);
            ps.setInt(2, quantity);
            ps.setInt(3, reorderThreshold);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET inventory by medicine ID
    public Inventory getInventoryByMedicineId(int medicineId) {

        String sql = "SELECT * FROM Inventory WHERE medicine_id = ?";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Inventory inventory = new Inventory();
                inventory.setMedicineId(rs.getInt("medicine_id"));
                inventory.setQuantityAvailable(rs.getInt("quantity_available"));
                inventory.setReorderThreshold(rs.getInt("reorder_threshold"));
                return inventory;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE quantity (used by sales & purchase)
    public boolean updateQuantity(int medicineId, int newQuantity) {

        String sql = """
            UPDATE Inventory
            SET quantity_available = ?
            WHERE medicine_id = ?
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, newQuantity);
            ps.setInt(2, medicineId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // UPDATE reorder threshold
    public boolean updateReorderThreshold(int medicineId, int reorderThreshold) {

        String sql = """
            UPDATE Inventory
            SET reorder_threshold = ?
            WHERE medicine_id = ?
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, reorderThreshold);
            ps.setInt(2, medicineId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET all medicines below reorder threshold (JIT core)
    public List<Inventory> getLowStockMedicines() {

        List<Inventory> lowStock = new ArrayList<>();

        String sql = """
            SELECT * FROM Inventory
            WHERE quantity_available < reorder_threshold
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                Inventory inventory = new Inventory();
                inventory.setMedicineId(rs.getInt("medicine_id"));
                inventory.setQuantityAvailable(rs.getInt("quantity_available"));
                inventory.setReorderThreshold(rs.getInt("reorder_threshold"));
                lowStock.add(inventory);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lowStock;
    }
}
