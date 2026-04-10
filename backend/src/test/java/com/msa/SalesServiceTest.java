package com.msa;

import com.msa.dao.BatchDAO;
import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dao.SalesDAO;
import com.msa.dao.SalesDetailsDAO;
import com.msa.dto.SaleItemRequest;
import com.msa.dto.SaleRequest;
import com.msa.dto.SaleResponse;
import com.msa.model.Batch;
import com.msa.model.Medicine;
import com.msa.model.SalesDetails;
import com.msa.service.SalesService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SalesServiceTest {

    @Test
    public void makeSaleSucceedsWithFefoAllocationAndCommit() {
        TxnState txn = new TxnState();
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(txn, closed);
        Supplier<Connection> provider = () -> conn;

        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byCode.put("MED1", med(1, "MED1", "10.00"));

        StubBatchDAO batchDAO = new StubBatchDAO();
        batchDAO.byMedicineId.put(1, Arrays.asList(
                batch(101, 1, LocalDate.now().plusDays(2), 2),
                batch(102, 1, LocalDate.now().plusDays(10), 10)));

        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        StubSalesDAO salesDAO = new StubSalesDAO();
        StubSalesDetailsDAO salesDetailsDAO = new StubSalesDetailsDAO();

        SalesService service = new SalesService(
                medicineDAO, batchDAO, inventoryDAO, salesDAO, salesDetailsDAO, provider);

        SaleResponse response = service.makeSale(new SaleRequest(
                List.of(new SaleItemRequest("MED1", 5))));

        assertEquals(5001, response.getSaleId());
        assertEquals(0, new BigDecimal("50.00").compareTo(response.getTotalAmount()));
        assertTrue(txn.committed);
        assertFalse(txn.rolledBack);
        assertTrue(closed.get());
        assertEquals(Arrays.asList(101, 102), batchDAO.reducedBatchIds);
        assertEquals(Arrays.asList(2, 3), batchDAO.reducedQty);
        assertEquals(1, inventoryDAO.reduceCalls);
        assertEquals(1, salesDetailsDAO.insertCalls);
    }

    @Test
    public void makeSaleRollsBackWhenInsufficientStock() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byCode.put("MED1", med(1, "MED1", "10.00"));

        StubBatchDAO batchDAO = new StubBatchDAO();
        batchDAO.byMedicineId.put(1, List.of(batch(201, 1, LocalDate.now().plusDays(2), 1)));

        SalesService service = new SalesService(
                medicineDAO, batchDAO, new StubInventoryDAO(), new StubSalesDAO(), new StubSalesDetailsDAO(), provider);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.makeSale(new SaleRequest(List.of(new SaleItemRequest("MED1", 5)))));

        assertTrue(ex.getMessage().contains("Sale failed"));
        assertTrue(txn.rolledBack);
        assertFalse(txn.committed);
    }

    @Test
    public void makeSaleRollsBackWhenBatchUpdateFails() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byCode.put("MED1", med(1, "MED1", "10.00"));

        StubBatchDAO batchDAO = new StubBatchDAO();
        batchDAO.byMedicineId.put(1, List.of(batch(301, 1, LocalDate.now().plusDays(2), 10)));
        batchDAO.failReduce = true;

        SalesService service = new SalesService(
                medicineDAO, batchDAO, new StubInventoryDAO(), new StubSalesDAO(), new StubSalesDetailsDAO(), provider);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.makeSale(new SaleRequest(List.of(new SaleItemRequest("MED1", 2)))));

        assertTrue(ex.getMessage().contains("Sale failed"));
        assertTrue(txn.rolledBack);
    }

    private static Medicine med(int id, String code, String unitPrice) {
        Medicine m = new Medicine();
        m.setMedicineId(id);
        m.setMedicineCode(code);
        m.setUnitSellingPrice(new BigDecimal(unitPrice));
        return m;
    }

    private static Batch batch(int id, int medId, LocalDate expiry, int qty) {
        Batch b = new Batch();
        b.setBatchId(id);
        b.setMedicineId(medId);
        b.setExpiryDate(expiry);
        b.setQuantity(qty);
        return b;
    }

    private static Connection stubConnection(TxnState txn, AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setAutoCommit":
                            if (args != null && args.length > 0 && args[0] instanceof Boolean) {
                                if (!((Boolean) args[0])) txn.autoCommitFalseSet = true;
                                if (((Boolean) args[0])) txn.autoCommitTrueSet = true;
                            }
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
        boolean autoCommitFalseSet;
        boolean autoCommitTrueSet;
        boolean committed;
        boolean rolledBack;
    }

    private static class StubMedicineDAO extends MedicineDAO {
        Map<String, Medicine> byCode = new HashMap<>();
        @Override
        public Medicine getMedicineByCode(Connection conn, String medicineCode) {
            return byCode.get(medicineCode);
        }
    }

    private static class StubBatchDAO extends BatchDAO {
        Map<Integer, List<Batch>> byMedicineId = new HashMap<>();
        List<Integer> reducedBatchIds = new ArrayList<>();
        List<Integer> reducedQty = new ArrayList<>();
        boolean failReduce;
        @Override
        public List<Batch> getBatchesByMedicineId(Connection conn, int medicineId) {
            return byMedicineId.getOrDefault(medicineId, List.of());
        }
        @Override
        public boolean reduceBatchQuantity(Connection conn, int batchId, int quantityToReduce) {
            reducedBatchIds.add(batchId);
            reducedQty.add(quantityToReduce);
            return !failReduce;
        }
        @Override
        public boolean deleteBatchIfEmpty(Connection conn, int batchId) {
            return true;
        }
    }

    private static class StubInventoryDAO extends InventoryDAO {
        int reduceCalls;
        @Override
        public boolean reduceQuantity(Connection conn, int medicineId, int amount) {
            reduceCalls++;
            return true;
        }
    }

    private static class StubSalesDAO extends SalesDAO {
        @Override
        public int insertSale(Connection conn, LocalDate saleDate, BigDecimal totalAmount) {
            return 5001;
        }
    }

    private static class StubSalesDetailsDAO extends SalesDetailsDAO {
        int insertCalls;
        @Override
        public boolean insertSalesDetail(Connection conn, SalesDetails detail) {
            insertCalls++;
            return true;
        }
    }
}
