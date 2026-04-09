package com.msa;

import com.msa.dao.AppUserDAO;
import com.msa.db.DBConnection;
import com.msa.model.AppUser;
import com.msa.service.AuthService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DBConnection.class, AppUserDAO.class})
public class AuthServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private AppUserDAO mockAppUserDAO;

    @Test
    public void testLoginSuccess() throws Exception {
        // Mock static DBConnection
        PowerMockito.mockStatic(DBConnection.class);
        when(DBConnection.getConnection()).thenReturn(mockConnection);

        // Mock DAO constructor
        PowerMockito.whenNew(AppUserDAO.class).withNoArguments().thenReturn(mockAppUserDAO);

        // Create service
        AuthService service = new AuthService();

        // Mock user
        AppUser mockUser = new AppUser();
        mockUser.setUsername("testuser");
        mockUser.setPassword("password123");
        mockUser.setRole("admin");

        when(mockAppUserDAO.getUserByUsername(mockConnection, "testuser")).thenReturn(mockUser);

        // Test
        AppUser result = service.login("testuser", "password123", "admin");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("admin", result.getRole());
        assertNull(result.getPassword()); // Password should be nullified

        // Verify
        verify(mockAppUserDAO).getUserByUsername(mockConnection, "testuser");
    }

    @Test
    public void testLoginInvalidPassword() throws Exception {
        // Mock static DBConnection
        PowerMockito.mockStatic(DBConnection.class);
        when(DBConnection.getConnection()).thenReturn(mockConnection);

        // Mock DAO constructor
        PowerMockito.whenNew(AppUserDAO.class).withNoArguments().thenReturn(mockAppUserDAO);

        // Create service
        AuthService service = new AuthService();

        // Mock user
        AppUser mockUser = new AppUser();
        mockUser.setUsername("testuser");
        mockUser.setPassword("password123");
        mockUser.setRole("admin");

        when(mockAppUserDAO.getUserByUsername(mockConnection, "testuser")).thenReturn(mockUser);

        // Test
        AppUser result = service.login("testuser", "wrongpassword", "admin");

        assertNull(result);

        // Verify
        verify(mockAppUserDAO).getUserByUsername(mockConnection, "testuser");
    }

    @Test
    public void testLoginUserNotFound() throws Exception {
        // Mock static DBConnection
        PowerMockito.mockStatic(DBConnection.class);
        when(DBConnection.getConnection()).thenReturn(mockConnection);

        // Mock DAO constructor
        PowerMockito.whenNew(AppUserDAO.class).withNoArguments().thenReturn(mockAppUserDAO);

        // Create service
        AuthService service = new AuthService();

        when(mockAppUserDAO.getUserByUsername(mockConnection, "nonexistent")).thenReturn(null);

        // Test
        AppUser result = service.login("nonexistent", "password", "admin");

        assertNull(result);

        // Verify
        verify(mockAppUserDAO).getUserByUsername(mockConnection, "nonexistent");
    }
}