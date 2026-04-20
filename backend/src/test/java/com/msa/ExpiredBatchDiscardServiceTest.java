package com.msa;

import com.msa.dao.BatchDAO;
import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dto.ExpiredBatchReport;
import com.msa.model.Batch;
import com.msa.model.Medicine;
import com.msa.service.ExpiredBatchDiscardService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ExpiredBatchDiscardServiceTest {

    @Test
    public void discardExpiredBatchesReturnsReportAndCommits() {
        TxnState txn = new TxnState();
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(txn, closed);
        Supplier<Connection> provider = () -> conn;

        StubBatchDAO batchDAO = new StubBatchDAO();
        batchDAO.expired = List.of(
                batch(11, 1, "B1", 3, 7),
                batch(12, 2, "B2", 2, 9));
        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byId.put(1, med(1, "MED1"));
        medicineDAO.byId.put(2, null);

        ExpiredBatchDiscardService service = new ExpiredBatchDiscardService(batchDAO, inventoryDAO, medicineDAO,
                provider);
        ExpiredBatchReport report = service.discardExpiredBatches();

        assertEquals(2, report.getTotalBatchesDiscarded());
        assertEquals(5, report.getTotalUnitsDiscarded());
        assertEquals("MED1", report.getItems().get(0).getMedicineCode());
        assertEquals("UNKNOWN-2", report.getItems().get(1).getMedicineCode());
        assertTrue(txn.committed);
        assertFalse(txn.rolledBack);
        assertTrue(closed.get());
    }

    @Test
    public void discardExpiredBatchesRollsBackWhenBatchDiscardFails() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubBatchDAO batchDAO = new StubBatchDAO();
        batchDAO.expired = List.of(batch(11, 1, "B1", 3, 7));
        batchDAO.failReduce = true;

        ExpiredBatchDiscardService service = new ExpiredBatchDiscardService(
                batchDAO, new StubInventoryDAO(), new StubMedicineDAO(), provider);

        RuntimeException ex = assertThrows(RuntimeException.class, service::discardExpiredBatches);
        assertTrue(ex.getMessage().contains("Expired batch discard failed"));
        assertTrue(txn.rolledBack);
    }

    private static Batch batch(int id, int medicineId, String number, int qty, int vendorId) {
        Batch b = new Batch();
        b.setBatchId(id);
        b.setMedicineId(medicineId);
        b.setBatchNumber(number);
        b.setQuantity(qty);
        b.setVendorId(vendorId);
        b.setExpiryDate(LocalDate.now().minusDays(1));
        return b;
    }

    private static Medicine med(int id, String code) {
        Medicine m = new Medicine();
        m.setMedicineId(id);
        m.setMedicineCode(code);
        return m;
    }

    private static Connection stubConnection(TxnState txn, AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setAutoCommit":
                            return null;
                        case "commit":
                            txn.committed = true;
                            return null;
                        case "rollback":
                            txn.rolledBack = true;
                            return null;
                        case "close":
                            closed.set(true);
                            return null;
                        default:
                            return null;
                    }
                });
    }

    private static class TxnState {
        boolean committed;
        boolean rolledBack;
    }

    private static class StubBatchDAO extends BatchDAO {
        List<Batch> expired = List.of();
        boolean failReduce;

        @Override
        public List<Batch> getExpiredBatches(Connection conn) {
            return expired;
        }

        @Override
        public boolean reduceBatchQuantity(Connection conn, int batchId, int quantityToReduce) {
            return !failReduce;
        }
    }

    private static class StubInventoryDAO extends InventoryDAO {
        @Override
        public boolean reduceQuantity(Connection conn, int medicineId, int amount) {
            return true;
        }
    }

    private static class StubMedicineDAO extends MedicineDAO {
        Map<Integer, Medicine> byId = new HashMap<>();

        @Override
        public Medicine getMedicineById(Connection conn, int medicineId) {
            return byId.get(medicineId);
        }
    }
}
