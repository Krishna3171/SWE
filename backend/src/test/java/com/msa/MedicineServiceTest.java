package com.msa;

import com.msa.dao.BatchDAO;
import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.db.DBConnection;
import com.msa.model.Medicine;
import com.msa.service.MedicineService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DBConnection.class, MedicineDAO.class, InventoryDAO.class, BatchDAO.class})
public class MedicineServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private MedicineDAO mockMedicineDAO;

    @Mock
    private InventoryDAO mockInventoryDAO;

    @Mock
    private BatchDAO mockBatchDAO;

    @Test
    public void testGetAllMedicines() throws Exception {
        // Mock static DBConnection
        PowerMockito.mockStatic(DBConnection.class);
        when(DBConnection.getConnection()).thenReturn(mockConnection);

        // Mock DAO constructor
        PowerMockito.whenNew(MedicineDAO.class).withNoArguments().thenReturn(mockMedicineDAO);

        // Create service
        MedicineService service = new MedicineService();

        // Mock data
        Medicine medicine1 = new Medicine("MED1", "Aspirin", "Acetylsalicylic Acid", new BigDecimal("10.00"), new BigDecimal("8.00"));
        Medicine medicine2 = new Medicine("MED2", "Paracetamol", "Paracetamol", new BigDecimal("5.00"), new BigDecimal("4.00"));
        List<Medicine> expectedMedicines = Arrays.asList(medicine1, medicine2);

        when(mockMedicineDAO.getAllMedicines(mockConnection)).thenReturn(expectedMedicines);

        // Test
        List<Medicine> result = service.getAllMedicines();

        assertEquals(2, result.size());
        assertEquals("Aspirin", result.get(0).getTradeName());
        assertEquals("Paracetamol", result.get(1).getTradeName());

        // Verify
        verify(mockMedicineDAO).getAllMedicines(mockConnection);
    }

    @Test
    public void testAddMedicine() throws Exception {
        // Mock static DBConnection
        PowerMockito.mockStatic(DBConnection.class);
        when(DBConnection.getConnection()).thenReturn(mockConnection);

        // Mock DAO constructors
        PowerMockito.whenNew(MedicineDAO.class).withNoArguments().thenReturn(mockMedicineDAO);
        PowerMockito.whenNew(InventoryDAO.class).withNoArguments().thenReturn(mockInventoryDAO);
        PowerMockito.whenNew(BatchDAO.class).withNoArguments().thenReturn(mockBatchDAO);

        // Create service
        MedicineService service = new MedicineService();

        // Mock data
        Medicine medicine = new Medicine("", "Test Medicine", "Generic", new BigDecimal("15.00"), new BigDecimal("12.00"));
        medicine.setMedicineId(1); // Simulate generated ID

        when(mockMedicineDAO.insertMedicine(eq(mockConnection), any(Medicine.class))).thenReturn(true);
        when(mockBatchDAO.insertBatch(eq(mockConnection), any())).thenReturn(1);

        // Test
        boolean result = service.addMedicine(medicine, 10, 5, "2025-12-31", 1);

        assertTrue(result);

        // Verify
        verify(mockMedicineDAO).insertMedicine(eq(mockConnection), any(Medicine.class));
        verify(mockInventoryDAO).createInventoryForMedicine(mockConnection, 1, 10, 5);
        verify(mockBatchDAO).insertBatch(eq(mockConnection), any());
    }
}