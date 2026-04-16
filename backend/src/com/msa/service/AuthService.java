package com.msa.service;

import com.msa.dao.AppUserDAO;
import com.msa.db.DBConnection;
import com.msa.model.AppUser;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

public class AuthService {

    private final AppUserDAO appUserDAO;

    public AuthService() {
        this.appUserDAO = new AppUserDAO();
    }

    public AppUser login(String username, String password, String role) {
        try (Connection conn = DBConnection.getConnection()) {
            AppUser user = appUserDAO.getUserByUsername(conn, normalize(username));
            if (user == null) {
                return null;
            }

            if (!passwordMatches(user.getPassword(), password)) {
                return null;
            }

            if (!isRoleMatch(user.getRole(), role)) {
                return null;
            }

            // Never return password in API response.
            user.setPassword(null);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    public boolean updateAdminCredentials(String adminUsername, String adminPassword, String newUsername,
            String newPassword) {
        return withAuthenticatedAdmin(adminUsername, adminPassword, admin -> appUserDAO
                .updateUserCredentials(admin.connection, admin.user.getUsername(), newUsername, newPassword));
    }

    public boolean addCashier(String adminUsername, String adminPassword, String cashierUsername,
            String cashierPassword) {
        return withAuthenticatedAdmin(adminUsername, adminPassword, admin -> {
            AppUser cashier = new AppUser(cashierUsername, cashierPassword, "cashier");
            return appUserDAO.insertUser(admin.connection, cashier);
        });
    }

    public boolean deleteCashier(String adminUsername, String adminPassword, String cashierUsername) {
        return withAuthenticatedAdmin(adminUsername, adminPassword,
                admin -> appUserDAO.deleteUserByUsernameAndRole(admin.connection, cashierUsername, "cashier"));
    }

    public boolean updateCashierCredentials(String adminUsername, String adminPassword, String currentUsername,
            String newUsername, String newPassword) {
        return withAuthenticatedAdmin(adminUsername, adminPassword, admin -> appUserDAO.updateUserCredentials(
                admin.connection,
                currentUsername,
                newUsername,
                newPassword));
    }

    public List<AppUser> listCashiers(String adminUsername, String adminPassword) {
        return withAuthenticatedAdminResult(adminUsername, adminPassword,
                admin -> appUserDAO.getUsersByRole(admin.connection, "cashier"));
    }

    private boolean withAuthenticatedAdmin(String username, String password, AdminOperation operation) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            AppUser user = appUserDAO.getUserByUsername(conn, normalize(username));
            if (user == null || !"admin".equalsIgnoreCase(normalize(user.getRole()))
                    || !passwordMatches(user.getPassword(), password)) {
                conn.rollback();
                return false;
            }

            boolean success = operation.execute(new AdminContext(conn, user));
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (Exception e) {
            throw new RuntimeException("Admin operation failed", e);
        }
    }

    private interface AdminOperation {
        boolean execute(AdminContext admin) throws Exception;
    }

    private interface AdminQuery<T> {
        T execute(AdminContext admin) throws Exception;
    }

    private static final class AdminContext {
        private final Connection connection;
        private final AppUser user;

        private AdminContext(Connection connection, AppUser user) {
            this.connection = connection;
            this.user = user;
        }
    }

    private <T> T withAuthenticatedAdminResult(String username, String password, AdminQuery<T> query) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            AppUser user = appUserDAO.getUserByUsername(conn, normalize(username));
            if (user == null || !"admin".equalsIgnoreCase(normalize(user.getRole()))
                    || !passwordMatches(user.getPassword(), password)) {
                conn.rollback();
                return null;
            }

            T result = query.execute(new AdminContext(conn, user));
            conn.commit();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Admin query failed", e);
        }
    }

    private static boolean isRoleMatch(String persistedRole, String requestedRole) {
        String requested = normalize(requestedRole);
        if (requested.isBlank()) {
            return true;
        }
        return normalize(persistedRole).equalsIgnoreCase(requested);
    }

    private static boolean passwordMatches(String persistedPassword, String providedPassword) {
        if (Objects.equals(persistedPassword, providedPassword)) {
            return true;
        }
        return Objects.equals(normalize(persistedPassword), normalize(providedPassword));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
