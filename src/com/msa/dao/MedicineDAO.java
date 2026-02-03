package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.msa.db.DBConnection;
import com.msa.model.Medicine;

public class MedicineDAO {

    public Medicine getMedicineByCode(String medicineCode) {

        String sql = "SELECT * FROM Medicine WHERE medicine_code = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, medicineCode);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Medicine medicine = new Medicine();

                medicine.setMedicineId(rs.getInt("medicine_id"));
                medicine.setMedicineCode(rs.getString("medicine_code"));
                medicine.setTradeName(rs.getString("trade_name"));
                medicine.setGenericName(rs.getString("generic_name"));
                medicine.setUnitSellingPrice(rs.getBigDecimal("unit_selling_price"));
                medicine.setUnitPurchasePrice(rs.getBigDecimal("unit_purchase_price"));

                return medicine;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // not found
    }

    public boolean insertMedicine(Medicine medicine) {

        String sql = """
                    INSERT INTO Medicine (
                        medicine_code,
                        trade_name,
                        generic_name,
                        unit_selling_price,
                        unit_purchase_price
                    )
                    VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            // 1️⃣ Generate medicine code
            String medicineCode = "MED" + System.currentTimeMillis();
            medicine.setMedicineCode(medicineCode);

            // 2️⃣ Bind parameters
            ps.setString(1, medicine.getMedicineCode());
            ps.setString(2, medicine.getTradeName());
            ps.setString(3, medicine.getGenericName());
            ps.setBigDecimal(4, medicine.getUnitSellingPrice());
            ps.setBigDecimal(5, medicine.getUnitPurchasePrice());

            // 3️⃣ Execute INSERT
            int rowsInserted = ps.executeUpdate();

            // 4️⃣ Fetch generated medicine_id
            if (rowsInserted > 0) {
                var rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    medicine.setMedicineId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateMedicine(Medicine medicine) {

        String sql = """
                    UPDATE Medicine
                    SET
                        trade_name = ?,
                        generic_name = ?,
                        unit_selling_price = ?,
                        unit_purchase_price = ?
                    WHERE medicine_code = ?
                """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1️⃣ Bind updated values
            ps.setString(1, medicine.getTradeName());
            ps.setString(2, medicine.getGenericName());
            ps.setBigDecimal(3, medicine.getUnitSellingPrice());
            ps.setBigDecimal(4, medicine.getUnitPurchasePrice());

            // 2️⃣ WHERE condition
            ps.setString(5, medicine.getMedicineCode());

            // 3️⃣ Execute update
            int rowsUpdated = ps.executeUpdate();

            return rowsUpdated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Medicine getMedicineById(int medicineId) {

        String sql = "SELECT * FROM Medicine WHERE medicine_id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Medicine medicine = new Medicine();
                medicine.setMedicineId(rs.getInt("medicine_id"));
                medicine.setMedicineCode(rs.getString("medicine_code"));
                medicine.setTradeName(rs.getString("trade_name"));
                medicine.setGenericName(rs.getString("generic_name"));
                medicine.setUnitSellingPrice(rs.getBigDecimal("unit_selling_price"));
                medicine.setUnitPurchasePrice(rs.getBigDecimal("unit_purchase_price"));
                return medicine;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Medicine> searchByName(String keyword) {

        List<Medicine> medicines = new ArrayList<>();

        String sql = """
                    SELECT * FROM Medicine
                    WHERE trade_name ILIKE ?
                       OR generic_name ILIKE ?
                """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Medicine medicine = new Medicine();
                medicine.setMedicineId(rs.getInt("medicine_id"));
                medicine.setMedicineCode(rs.getString("medicine_code"));
                medicine.setTradeName(rs.getString("trade_name"));
                medicine.setGenericName(rs.getString("generic_name"));
                medicine.setUnitSellingPrice(rs.getBigDecimal("unit_selling_price"));
                medicine.setUnitPurchasePrice(rs.getBigDecimal("unit_purchase_price"));

                medicines.add(medicine);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return medicines;
    }

    public List<Medicine> getAllMedicines() {

        List<Medicine> medicines = new ArrayList<>();

        String sql = "SELECT * FROM Medicine";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Medicine medicine = new Medicine();
                medicine.setMedicineId(rs.getInt("medicine_id"));
                medicine.setMedicineCode(rs.getString("medicine_code"));
                medicine.setTradeName(rs.getString("trade_name"));
                medicine.setGenericName(rs.getString("generic_name"));
                medicine.setUnitSellingPrice(rs.getBigDecimal("unit_selling_price"));
                medicine.setUnitPurchasePrice(rs.getBigDecimal("unit_purchase_price"));

                medicines.add(medicine);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return medicines;
    }

}
