package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.Batch;

public class BatchDAO {

    // INSERT batch (used during purchase)
    public boolean insertBatch(Connection conn,Batch batch) {

        String sql = """
            INSERT INTO Batch (
                medicine_id,
                batch_number,
                expiry_date,
                quantity,
                vendor_id
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {

            ps.setInt(1, batch.getMedicineId());
            ps.setString(2, batch.getBatchNumber());
            ps.setDate(3, Date.valueOf(batch.getExpiryDate()));
            ps.setInt(4, batch.getQuantity());
            ps.setInt(5, batch.getVendorId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    batch.setBatchId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET all batches for a medicine
    public List<Batch> getBatchesByMedicineId(Connection conn,int medicineId) {

        List<Batch> batches = new ArrayList<>();

        String sql = "SELECT * FROM Batch WHERE medicine_id = ? ";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Batch batch = new Batch();
                batch.setBatchId(rs.getInt("batch_id"));
                batch.setMedicineId(rs.getInt("medicine_id"));
                batch.setBatchNumber(rs.getString("batch_number"));
                batch.setExpiryDate(rs.getDate("expiry_date").toLocalDate());
                batch.setQuantity(rs.getInt("quantity"));
                batch.setVendorId(rs.getInt("vendor_id"));
                batches.add(batch);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return batches;
    }

    // GET all expired batches (global)
    public List<Batch> getExpiredBatches(Connection conn) {

        List<Batch> expired = new ArrayList<>();

        String sql = """
            SELECT * FROM Batch
            WHERE expiry_date < CURRENT_DATE
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                Batch batch = new Batch();
                batch.setBatchId(rs.getInt("batch_id"));
                batch.setMedicineId(rs.getInt("medicine_id"));
                batch.setBatchNumber(rs.getString("batch_number"));
                batch.setExpiryDate(rs.getDate("expiry_date").toLocalDate());
                batch.setQuantity(rs.getInt("quantity"));
                batch.setVendorId(rs.getInt("vendor_id"));
                expired.add(batch);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return expired;
    }

    // GET expired batches vendor-wise
    public List<Batch> getExpiredBatchesByVendor(Connection conn,int vendorId) {

        List<Batch> expired = new ArrayList<>();

        String sql = """
            SELECT * FROM Batch
            WHERE expiry_date < CURRENT_DATE
              AND vendor_id = ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, vendorId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Batch batch = new Batch();
                batch.setBatchId(rs.getInt("batch_id"));
                batch.setMedicineId(rs.getInt("medicine_id"));
                batch.setBatchNumber(rs.getString("batch_number"));
                batch.setExpiryDate(rs.getDate("expiry_date").toLocalDate());
                batch.setQuantity(rs.getInt("quantity"));
                batch.setVendorId(rs.getInt("vendor_id"));
                expired.add(batch);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return expired;
    }

    public boolean reduceBatchQuantity(Connection conn,int batchId, int quantityToReduce) {

        String sql = """
            UPDATE Batch
            SET quantity = quantity - ?
            WHERE batch_id = ?
            AND quantity >= ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, quantityToReduce);
            ps.setInt(2, batchId);
            ps.setInt(3, quantityToReduce);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteBatch(Connection conn,int batchId) {

        String sql = "DELETE FROM Batch WHERE batch_id = ?";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, batchId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
