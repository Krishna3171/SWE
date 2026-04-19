package com.msa;

import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.model.Medicine;
import com.msa.service.MedicineService;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class MedicineServiceTest {

    @Test
    public void getAllMedicinesReturnsDataFromDao() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection connection = stubConnection(closed, new TxnState());
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.allMedicines = new ArrayList<>();
        Medicine medicine1 = new Medicine("MED1", "Aspirin", "Acetylsalicylic Acid", new BigDecimal("10.00"), new BigDecimal("8.00"));
        Medicine medicine2 = new Medicine("MED2", "Paracetamol", "Paracetamol", new BigDecimal("5.00"), new BigDecimal("4.00"));
        medicineDAO.allMedicines.addAll(Arrays.asList(medicine1, medicine2));
        Supplier<Connection> connectionProvider = () -> connection;
        MedicineService service = new MedicineService(medicineDAO, new StubInventoryDAO(), connectionProvider);

        List<Medicine> result = service.getAllMedicines();

        assertEquals(2, result.size());
        assertEquals("Aspirin", result.get(0).getTradeName());
        assertEquals("Paracetamol", result.get(1).getTradeName());
        assertTrue(closed.get());
    }

    @Test
    public void addMedicineCreatesInventoryRecordWithDefaults() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        TxnState txn = new TxnState();
        Connection connection = stubConnection(closed, txn);
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        Supplier<Connection> connectionProvider = () -> connection;
        MedicineService service = new MedicineService(medicineDAO, inventoryDAO, connectionProvider);

        Medicine medicine = new Medicine("MEDX", "Test Medicine", "Generic", new BigDecimal("15.00"), new BigDecimal("12.00"));
        medicine.setMedicineId(1);
        medicineDAO.insertResult = true;

        boolean result = service.addMedicine(medicine);

        assertTrue(result);
        assertTrue(txn.autoCommitDisabled);
        assertEquals(1, inventoryDAO.calls);
        assertEquals(1, inventoryDAO.lastMedicineId);
        assertEquals(0, inventoryDAO.lastQuantity);
        assertEquals(10, inventoryDAO.lastReorderThreshold);
        assertTrue(txn.committed);
        assertFalse(txn.rolledBack);
        assertTrue(closed.get());
    }

    @Test
    public void addMedicineReturnsFalseWhenInsertFails() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        TxnState txn = new TxnState();
        Connection connection = stubConnection(closed, txn);
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        Supplier<Connection> connectionProvider = () -> connection;
        MedicineService service = new MedicineService(medicineDAO, inventoryDAO, connectionProvider);

        Medicine medicine = new Medicine("MEDX", "Test Medicine", "Generic", new BigDecimal("15.00"), new BigDecimal("12.00"));
        medicine.setMedicineId(1);
        medicineDAO.insertResult = false;

        boolean result = service.addMedicine(medicine);

        assertFalse(result);
        assertEquals(0, inventoryDAO.calls);
        assertTrue(txn.committed);
        assertFalse(txn.rolledBack);
        assertTrue(closed.get());
    }

    @Test
    public void addMedicineRollsBackAndWrapsExceptionWhenDaoThrows() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        TxnState txn = new TxnState();
        Connection connection = stubConnection(closed, txn);
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        Supplier<Connection> connectionProvider = () -> connection;
        MedicineService service = new MedicineService(medicineDAO, inventoryDAO, connectionProvider);

        Medicine medicine = new Medicine("MEDX", "Test Medicine", "Generic", new BigDecimal("15.00"), new BigDecimal("12.00"));
        medicine.setMedicineId(1);
        medicineDAO.throwOnInsert = true;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.addMedicine(medicine));

        assertTrue(ex.getMessage().contains("Failed to add medicine"));
        assertTrue(txn.rolledBack);
        assertFalse(txn.committed);
        assertTrue(closed.get());
    }

    private static Connection stubConnection(AtomicBoolean closed, TxnState txn) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "close":
                            closed.set(true);
                            return null;
                        case "isClosed":
                            return closed.get();
                        case "setAutoCommit":
                            txn.autoCommitDisabled = args != null && args.length > 0 && Boolean.FALSE.equals(args[0]);
                            return null;
                        case "commit":
                            txn.committed = true;
                            return null;
                        case "rollback":
                            txn.rolledBack = true;
                            return null;
                        default:
                            return defaultValue(method.getReturnType());
                    }
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

    private static class TxnState {
        private boolean autoCommitDisabled;
        private boolean committed;
        private boolean rolledBack;
    }

    private static class StubMedicineDAO extends MedicineDAO {
        private boolean insertResult;
        private boolean throwOnInsert;
        private List<Medicine> allMedicines = new ArrayList<>();

        @Override
        public List<Medicine> getAllMedicines(Connection conn) {
            return allMedicines;
        }

        @Override
        public boolean insertMedicine(Connection conn, Medicine medicine) {
            if (throwOnInsert) {
                throw new RuntimeException("insert failed");
            }
            return insertResult;
        }
    }

    private static class StubInventoryDAO extends InventoryDAO {
        private int calls;
        private int lastMedicineId;
        private int lastQuantity;
        private int lastReorderThreshold;

        @Override
        public boolean createInventoryForMedicine(Connection conn, int medicineId, int quantity, int reorderThreshold) {
            this.calls++;
            this.lastMedicineId = medicineId;
            this.lastQuantity = quantity;
            this.lastReorderThreshold = reorderThreshold;
            return true;
        }
    }
}
