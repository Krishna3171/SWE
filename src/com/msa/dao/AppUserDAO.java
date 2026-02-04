package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.msa.model.AppUser;

public class AppUserDAO {

    // INSERT user (admin/setup)
    public boolean insertUser(Connection conn,AppUser user) {

        String sql = """
            INSERT INTO App_User (
                username,
                password,
                role
            )
            VALUES (?, ?, ?)
        """;

        try (
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword()); // plaintext for now
            ps.setString(3, user.getRole());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET user by username (login)
    public AppUser getUserByUsername(Connection conn, String username) {

        String sql = "SELECT * FROM App_User WHERE username = ?";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                AppUser user = new AppUser();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
