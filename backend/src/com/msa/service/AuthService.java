package com.msa.service;

import com.msa.dao.AppUserDAO;
import com.msa.db.DBConnection;
import com.msa.model.AppUser;

import java.sql.Connection;

public class AuthService {

    private final AppUserDAO appUserDAO;

    public AuthService() {
        this.appUserDAO = new AppUserDAO();
    }

    public AppUser login(String username, String password, String role) {
        try (Connection conn = DBConnection.getConnection()) {
            AppUser user = appUserDAO.getUserByUsername(conn, username);
            if (user == null) {
                return null;
            }

            if (!user.getPassword().equals(password)) {
                return null;
            }

            if (role != null && !role.isBlank() && !user.getRole().equalsIgnoreCase(role)) {
                return null;
            }

            // Never return password in API response.
            user.setPassword(null);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }
}
