package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.Medicine;

public class MedicineDAO {

    public Medicine getMedicineByCode(Connection conn, String medicineCode) {

        String sql = "SELECT * FROM Medicine WHERE medicine_code = ?";

        try (
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

    public boolean insertMedicine(Connection conn, Medicine medicine) {

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
                PreparedStatement ps = conn.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            // 1️⃣ Use a temporary unique code for insert.
            // Final code is generated after DB returns the real medicine_id.
            String temporaryCode = "TMP-" + UUID.randomUUID();
            medicine.setMedicineCode(temporaryCode);

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
                    int generatedId = rs.getInt(1);
                    medicine.setMedicineId(generatedId);

                    String finalMedicineCode = "MED" + generatedId;
                    medicine.setMedicineCode(finalMedicineCode);

                    String updateCodeSql = "UPDATE Medicine SET medicine_code = ? WHERE medicine_id = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateCodeSql)) {
                        updatePs.setString(1, finalMedicineCode);
                        updatePs.setInt(2, generatedId);
                        if (updatePs.executeUpdate() == 0) {
                            return false;
                        }
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateMedicine(Connection conn, Medicine medicine) {

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

    public Medicine getMedicineById(Connection conn, int medicineId) {

        String sql = "SELECT * FROM Medicine WHERE medicine_id = ?";

        try (
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

    public List<Medicine> searchByName(Connection conn, String keyword) {

        List<Medicine> medicines = new ArrayList<>();

        String sql = """
                    SELECT * FROM Medicine
                    WHERE trade_name ILIKE ?
                       OR generic_name ILIKE ?
                """;

        try (
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

    public List<Medicine> getAllMedicines(Connection conn) {

        List<Medicine> medicines = new ArrayList<>();

        String sql = "SELECT * FROM Medicine";

        try (
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

    public boolean deleteMedicine(Connection conn, String medicineCode) {

        String[] guardSqls = new String[] {
                "SELECT 1 FROM Batch b JOIN Medicine m ON b.medicine_id = m.medicine_id WHERE m.medicine_code = ? LIMIT 1",
                "SELECT 1 FROM Purchase_Details pd JOIN Medicine m ON pd.medicine_id = m.medicine_id WHERE m.medicine_code = ? LIMIT 1",
                "SELECT 1 FROM Sales_Details sd JOIN Medicine m ON sd.medicine_id = m.medicine_id WHERE m.medicine_code = ? LIMIT 1",
                "SELECT 1 FROM Vendor_Medicine vm JOIN Medicine m ON vm.medicine_id = m.medicine_id WHERE m.medicine_code = ? LIMIT 1",
                "SELECT 1 FROM Inventory i JOIN Medicine m ON i.medicine_id = m.medicine_id WHERE m.medicine_code = ? LIMIT 1"
        };

        for (String guardSql : guardSqls) {
            try (PreparedStatement guardPs = conn.prepareStatement(guardSql)) {
                guardPs.setString(1, medicineCode);
                ResultSet guardRs = guardPs.executeQuery();
                if (guardRs.next()) {
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        String sql = "DELETE FROM Medicine WHERE medicine_code = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, medicineCode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

}
