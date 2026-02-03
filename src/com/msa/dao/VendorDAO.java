package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.msa.db.DBConnection;
import com.msa.model.Vendor;

public class VendorDAO {

    // INSERT
    public boolean insertVendor(Vendor vendor) {

        String sql = """
            INSERT INTO Vendor (vendor_name, address, contact_no)
            VALUES (?, ?, ?)
        """;

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {

            ps.setString(1, vendor.getVendorName());
            ps.setString(2, vendor.getAddress());
            ps.setString(3, vendor.getContactNo());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    vendor.setVendorId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET BY ID
    public Vendor getVendorById(int vendorId) {

        String sql = "SELECT * FROM Vendor WHERE vendor_id = ?";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, vendorId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Vendor vendor = new Vendor();
                vendor.setVendorId(rs.getInt("vendor_id"));
                vendor.setVendorName(rs.getString("vendor_name"));
                vendor.setAddress(rs.getString("address"));
                vendor.setContactNo(rs.getString("contact_no"));
                return vendor;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // GET BY NAME
    public Vendor getVendorByName(String vendorName) {

        String sql = "SELECT * FROM Vendor WHERE vendor_name ILIKE ?";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, vendorName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Vendor vendor = new Vendor();
                vendor.setVendorId(rs.getInt("vendor_id"));
                vendor.setVendorName(rs.getString("vendor_name"));
                vendor.setAddress(rs.getString("address"));
                vendor.setContactNo(rs.getString("contact_no"));
                return vendor;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // GET ALL
    public List<Vendor> getAllVendors() {

        List<Vendor> vendors = new ArrayList<>();
        String sql = "SELECT * FROM Vendor";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                Vendor vendor = new Vendor();
                vendor.setVendorId(rs.getInt("vendor_id"));
                vendor.setVendorName(rs.getString("vendor_name"));
                vendor.setAddress(rs.getString("address"));
                vendor.setContactNo(rs.getString("contact_no"));
                vendors.add(vendor);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return vendors;
    }
}
