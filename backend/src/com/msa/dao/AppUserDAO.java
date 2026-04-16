package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.msa.model.AppUser;

public class AppUserDAO {

    // INSERT user (admin/setup)
    public boolean insertUser(Connection conn, AppUser user) {

        if (getUserByUsername(conn, user.getUsername()) != null) {
            return false;
        }

        String sql = """
                    INSERT INTO App_User (
                        username,
                        password,
                        role
                    )
                    VALUES (?, ?, ?)
                """;

        try (
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

    public boolean updateUserCredentials(Connection conn, String currentUsername, String newUsername,
            String newPassword) {

        if (!currentUsername.equalsIgnoreCase(newUsername) && getUserByUsername(conn, newUsername) != null) {
            return false;
        }

        String sql = """
                    UPDATE App_User
                    SET username = ?,
                        password = ?
                    WHERE username = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, newPassword);
            ps.setString(3, currentUsername);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUserByUsernameAndRole(Connection conn, String username, String role) {

        String sql = """
                    DELETE FROM App_User
                    WHERE username = ?
                      AND LOWER(role) = LOWER(?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, role);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<AppUser> getUsersByRole(Connection conn, String role) {

        List<AppUser> users = new ArrayList<>();

        String sql = "SELECT * FROM App_User WHERE LOWER(role) = LOWER(?) ORDER BY username";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                AppUser user = new AppUser();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    // GET user by username (login)
    public AppUser getUserByUsername(Connection conn, String username) {

        String sql = "SELECT * FROM App_User WHERE LOWER(username) = LOWER(?) LIMIT 1";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

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
