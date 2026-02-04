package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.msa.db.DBConnection;
import com.msa.model.VendorMedicine;

public class VendorMedicineDAO {

    // LINK vendor to medicine
    public boolean linkVendorToMedicine(Connection conn, int vendorId, int medicineId) {

        String sql = """
            INSERT INTO Vendor_Medicine (vendor_id, medicine_id)
            VALUES (?, ?)
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, vendorId);
            ps.setInt(2, medicineId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET all vendors supplying a medicine
    public List<VendorMedicine> getVendorsForMedicine(Connection conn, int medicineId) {

        List<VendorMedicine> mappings = new ArrayList<>();

        String sql = """
            SELECT * FROM Vendor_Medicine
            WHERE medicine_id = ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, medicineId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                VendorMedicine vm = new VendorMedicine();
                vm.setVendorId(rs.getInt("vendor_id"));
                vm.setMedicineId(rs.getInt("medicine_id"));
                mappings.add(vm);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return mappings;
    }

    // GET all medicines supplied by a vendor
    public List<VendorMedicine> getMedicinesForVendor(Connection conn, int vendorId) {

        List<VendorMedicine> mappings = new ArrayList<>();

        String sql = """
            SELECT * FROM Vendor_Medicine
            WHERE vendor_id = ?
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, vendorId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                VendorMedicine vm = new VendorMedicine();
                vm.setVendorId(rs.getInt("vendor_id"));
                vm.setMedicineId(rs.getInt("medicine_id"));
                mappings.add(vm);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return mappings;
    }
}
