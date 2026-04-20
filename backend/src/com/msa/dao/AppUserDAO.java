package com.msa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    // GET user by id
    public AppUser getUserById(Connection conn, int userId) {

        String sql = "SELECT * FROM App_User WHERE user_id = ?";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);
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

    // GET all users
    public List<AppUser> getAllUsers(Connection conn) {

        List<AppUser> users = new ArrayList<>();
        String sql = "SELECT * FROM App_User ORDER BY user_id ASC";

        try (
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

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

    // UPDATE user
    public boolean updateUser(Connection conn, AppUser user) {

        String sql = "UPDATE App_User SET username = ?, password = ?, role = ? WHERE user_id = ?";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.setInt(4, user.getUserId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // DELETE user
    public boolean deleteUser(Connection conn, int userId) {

        String sql = "DELETE FROM App_User WHERE user_id = ?";

        try (
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
