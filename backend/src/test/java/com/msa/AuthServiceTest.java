package com.msa;

import com.msa.dao.AppUserDAO;
import com.msa.model.AppUser;
import com.msa.service.AuthService;
import org.junit.Test;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class AuthServiceTest {

    @Test
    public void loginReturnsUserAndClearsPasswordWhenCredentialsAndRoleMatch() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed);
        StubAppUserDAO appUserDAO = new StubAppUserDAO();
        appUserDAO.userToReturn = user("testuser", "password123", "admin");
        Supplier<Connection> connectionProvider = () -> connection;
        AuthService service = new AuthService(appUserDAO, connectionProvider);

        AppUser result = service.login("testuser", "password123", "admin");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("admin", result.getRole());
        assertNull(result.getPassword());
        assertTrue(closed.get());
        assertEquals("testuser", appUserDAO.lastRequestedUsername);
    }

    @Test
    public void loginReturnsNullWhenPasswordIsInvalid() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed);
        StubAppUserDAO appUserDAO = new StubAppUserDAO();
        appUserDAO.userToReturn = user("testuser", "password123", "admin");
        Supplier<Connection> connectionProvider = () -> connection;
        AuthService service = new AuthService(appUserDAO, connectionProvider);

        AppUser result = service.login("testuser", "wrongpassword", "admin");

        assertNull(result);
        assertTrue(closed.get());
    }

    @Test
    public void loginReturnsNullWhenRoleDoesNotMatchIgnoringCase() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed);
        StubAppUserDAO appUserDAO = new StubAppUserDAO();
        appUserDAO.userToReturn = user("testuser", "password123", "staff");
        Supplier<Connection> connectionProvider = () -> connection;
        AuthService service = new AuthService(appUserDAO, connectionProvider);

        AppUser result = service.login("testuser", "password123", "admin");

        assertNull(result);
        assertTrue(closed.get());
    }

    @Test
    public void loginSkipsRoleCheckWhenRoleInputIsBlank() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed);
        StubAppUserDAO appUserDAO = new StubAppUserDAO();
        appUserDAO.userToReturn = user("testuser", "password123", "staff");
        Supplier<Connection> connectionProvider = () -> connection;
        AuthService service = new AuthService(appUserDAO, connectionProvider);

        AppUser result = service.login("testuser", "password123", " ");

        assertNotNull(result);
        assertEquals("staff", result.getRole());
        assertNull(result.getPassword());
        assertTrue(closed.get());
    }

    @Test
    public void loginReturnsNullWhenUserIsNotFound() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed);
        StubAppUserDAO appUserDAO = new StubAppUserDAO();
        appUserDAO.userToReturn = null;
        Supplier<Connection> connectionProvider = () -> connection;
        AuthService service = new AuthService(appUserDAO, connectionProvider);

        AppUser result = service.login("nonexistent", "password", "admin");

        assertNull(result);
        assertTrue(closed.get());
    }

    @Test
    public void loginWrapsExceptionAsRuntimeException() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed);
        StubAppUserDAO appUserDAO = new StubAppUserDAO();
        appUserDAO.shouldThrow = true;
        Supplier<Connection> connectionProvider = () -> connection;
        AuthService service = new AuthService(appUserDAO, connectionProvider);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.login("testuser", "password123", "admin"));

        assertTrue(ex.getMessage().contains("Login failed"));
        assertTrue(closed.get());
    }

    private static AppUser user(String username, String password, String role) {
        AppUser mockUser = new AppUser();
        mockUser.setUsername(username);
        mockUser.setPassword(password);
        mockUser.setRole(role);
        return mockUser;
    }

    private static Connection stubConnection(AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        closed.set(true);
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return closed.get();
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == char.class) return '\0';
        return null;
    }

    private static class StubAppUserDAO extends AppUserDAO {
        private AppUser userToReturn;
        private boolean shouldThrow;
        private String lastRequestedUsername;

        @Override
        public AppUser getUserByUsername(Connection conn, String username) {
            this.lastRequestedUsername = username;
            if (shouldThrow) {
                throw new RuntimeException("db fail");
            }
            return userToReturn;
        }
    }
}