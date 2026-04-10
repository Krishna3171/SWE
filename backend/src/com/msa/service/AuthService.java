package com.msa.service;

import com.msa.dao.AppUserDAO;
import com.msa.db.DBConnection;
import com.msa.model.AppUser;

import java.sql.Connection;
import java.util.function.Supplier;

public class AuthService {

    private final AppUserDAO appUserDAO;
    private final Supplier<Connection> connectionProvider;

    public AuthService() {
        this(new AppUserDAO(), () -> {
            try {
                return DBConnection.getConnection();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get database connection", e);
            }
        });
    }

    public AuthService(AppUserDAO appUserDAO, Supplier<Connection> connectionProvider) {
        this.appUserDAO = appUserDAO;
        this.connectionProvider = connectionProvider;
    }

    public AppUser login(String username, String password, String role) {
        try (Connection conn = connectionProvider.get()) {
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
