package com.msa;

import com.msa.dao.BatchDAO;
import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dao.PurchaseDAO;
import com.msa.dao.PurchaseDetailsDAO;
import com.msa.dao.VendorDAO;
import com.msa.dao.VendorMedicineDAO;
import com.msa.dto.PurchaseItemRequest;
import com.msa.dto.PurchaseRequest;
import com.msa.dto.PurchaseResponse;
import com.msa.model.Batch;
import com.msa.model.Medicine;
import com.msa.model.Purchase;
import com.msa.model.PurchaseDetails;
import com.msa.model.Vendor;
import com.msa.service.PurchaseService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class PurchaseServiceTest {

    @Test
    public void makePurchaseSucceedsAndCreatesPendingOrder() {
        TxnState txn = new TxnState();
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(txn, closed);
        Supplier<Connection> provider = () -> conn;

        StubVendorDAO vendorDAO = new StubVendorDAO();
        vendorDAO.vendor = vendor(5, "V1");
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byCode.put("MED1", med(1, "MED1"));
        StubVendorMedicineDAO vendorMedicineDAO = new StubVendorMedicineDAO();
        vendorMedicineDAO.supplied = true;
        StubPurchaseDAO purchaseDAO = new StubPurchaseDAO();
        StubPurchaseDetailsDAO detailsDAO = new StubPurchaseDetailsDAO();
        StubBatchDAO batchDAO = new StubBatchDAO();
        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        inventoryDAO.addQuantityResult = false;

        PurchaseService service = new PurchaseService(vendorDAO, medicineDAO, vendorMedicineDAO,
                purchaseDAO, detailsDAO, batchDAO, inventoryDAO, provider);

        PurchaseRequest request = new PurchaseRequest(5, List.of(
                new PurchaseItemRequest("MED1", "B-1", LocalDate.now().plusDays(30).toString(), 4,
                        new BigDecimal("2.50"))));

        PurchaseResponse response = service.makePurchase(request);

        assertEquals(7001, response.getPurchaseId());
        assertEquals(0, new BigDecimal("10.00").compareTo(response.getTotalAmount()));
        assertTrue(txn.committed);
        assertFalse(txn.rolledBack);
        assertTrue(closed.get());
        assertEquals(1, detailsDAO.insertCalls);
        assertEquals(1, batchDAO.insertCalls);
        assertNotNull(detailsDAO.lastInsertedDetail);
        assertFalse(detailsDAO.lastInsertedDetail.isReceived());
        assertEquals(0, batchDAO.lastInsertedBatch.getQuantity());
        assertEquals(0, inventoryDAO.createCalls);
    }

    @Test
    public void receivePurchaseLineSucceedsAndMarksReceived() {
        TxnState txn = new TxnState();
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(txn, closed);
        Supplier<Connection> provider = () -> conn;

        StubPurchaseDetailsDAO detailsDAO = new StubPurchaseDetailsDAO();
        detailsDAO.detailByPurchaseAndBatch = new PurchaseDetails(
                7001,
                1,
                6,
                new BigDecimal("2.25"),
                9001,
                LocalDate.now(),
                false);

        StubBatchDAO batchDAO = new StubBatchDAO();
        batchDAO.addBatchQuantityResult = true;

        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        inventoryDAO.addQuantityResult = true;

        PurchaseService service = new PurchaseService(
                new StubVendorDAO(),
                new StubMedicineDAO(),
                new StubVendorMedicineDAO(),
                new StubPurchaseDAO(),
                detailsDAO,
                batchDAO,
                inventoryDAO,
                provider);

        PurchaseDetails received = service.receivePurchaseLine(7001, 9001);

        assertTrue(received.isReceived());
        assertTrue(txn.committed);
        assertFalse(txn.rolledBack);
        assertTrue(closed.get());
        assertEquals(1, detailsDAO.markAsReceivedCalls);
        assertEquals(1, batchDAO.addBatchQuantityCalls);
        assertEquals(0, inventoryDAO.createCalls);
    }

    @Test
    public void receivePurchaseLineRollsBackWhenAlreadyReceived() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubPurchaseDetailsDAO detailsDAO = new StubPurchaseDetailsDAO();
        detailsDAO.detailByPurchaseAndBatch = new PurchaseDetails(
                7001,
                1,
                6,
                new BigDecimal("2.25"),
                9001,
                LocalDate.now(),
                true);

        PurchaseService service = new PurchaseService(
                new StubVendorDAO(),
                new StubMedicineDAO(),
                new StubVendorMedicineDAO(),
                new StubPurchaseDAO(),
                detailsDAO,
                new StubBatchDAO(),
                new StubInventoryDAO(),
                provider);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.receivePurchaseLine(7001, 9001));

        assertTrue(ex.getMessage().contains("Receive failed"));
        assertTrue(txn.rolledBack);
    }

    @Test
    public void getPendingPurchaseDetailsReturnsDaoResults() {
        TxnState txn = new TxnState();
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(txn, closed);
        Supplier<Connection> provider = () -> conn;

        StubPurchaseDetailsDAO detailsDAO = new StubPurchaseDetailsDAO();
        detailsDAO.pending = List.of(
                new PurchaseDetails(1, 1, 2, new BigDecimal("1.00"), 11, LocalDate.now(), false),
                new PurchaseDetails(2, 2, 3, new BigDecimal("2.00"), 12, LocalDate.now(), false));

        PurchaseService service = new PurchaseService(
                new StubVendorDAO(),
                new StubMedicineDAO(),
                new StubVendorMedicineDAO(),
                new StubPurchaseDAO(),
                detailsDAO,
                new StubBatchDAO(),
                new StubInventoryDAO(),
                provider);

        List<PurchaseDetails> pending = service.getPendingPurchaseDetails();

        assertEquals(2, pending.size());
        assertFalse(pending.get(0).isReceived());
        assertTrue(closed.get());
    }

    @Test
    public void makePurchaseRollsBackWhenVendorMissing() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubVendorDAO vendorDAO = new StubVendorDAO();
        vendorDAO.vendor = null;

        PurchaseService service = new PurchaseService(vendorDAO, new StubMedicineDAO(), new StubVendorMedicineDAO(),
                new StubPurchaseDAO(), new StubPurchaseDetailsDAO(), new StubBatchDAO(), new StubInventoryDAO(),
                provider);

        PurchaseRequest request = new PurchaseRequest(99, List.of(
                new PurchaseItemRequest("MED1", "B-1", LocalDate.now().plusDays(30).toString(), 1,
                        new BigDecimal("2.00"))));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.makePurchase(request));
        assertTrue(ex.getMessage().contains("Purchase failed"));
        assertTrue(txn.rolledBack);
    }

    @Test
    public void makePurchaseRollsBackOnInvalidExpiryDate() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubVendorDAO vendorDAO = new StubVendorDAO();
        vendorDAO.vendor = vendor(5, "V1");
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byCode.put("MED1", med(1, "MED1"));
        StubVendorMedicineDAO vendorMedicineDAO = new StubVendorMedicineDAO();
        vendorMedicineDAO.supplied = true;

        PurchaseService service = new PurchaseService(vendorDAO, medicineDAO, vendorMedicineDAO,
                new StubPurchaseDAO(), new StubPurchaseDetailsDAO(), new StubBatchDAO(), new StubInventoryDAO(),
                provider);

        PurchaseRequest request = new PurchaseRequest(5, List.of(
                new PurchaseItemRequest("MED1", "B-1", LocalDate.now().minusDays(1).toString(), 2,
                        new BigDecimal("1.25"))));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.makePurchase(request));
        assertTrue(ex.getMessage().contains("Purchase failed"));
        assertTrue(txn.rolledBack);
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

    private static Vendor vendor(int id, String name) {
        Vendor v = new Vendor();
        v.setVendorId(id);
        v.setVendorName(name);
        return v;
    }

    private static Medicine med(int id, String code) {
        Medicine m = new Medicine();
        m.setMedicineId(id);
        m.setMedicineCode(code);
        return m;
    }

    private static class TxnState {
        boolean committed;
        boolean rolledBack;
    }

    private static class StubVendorDAO extends VendorDAO {
        Vendor vendor;

        @Override
        public Vendor getVendorById(Connection conn, int vendorId) {
            return vendor;
        }
    }

    private static class StubMedicineDAO extends MedicineDAO {
        Map<String, Medicine> byCode = new HashMap<>();

        @Override
        public Medicine getMedicineByCode(Connection conn, String medicineCode) {
            return byCode.get(medicineCode);
        }
    }

    private static class StubVendorMedicineDAO extends VendorMedicineDAO {
        boolean supplied;

        @Override
        public boolean existsMapping(Connection conn, int vendorId, int medicineId) {
            return supplied;
        }
    }

    private static class StubPurchaseDAO extends PurchaseDAO {
        @Override
        public int insertPurchase(Connection conn, Purchase purchase) {
            return 7001;
        }
    }

    private static class StubPurchaseDetailsDAO extends PurchaseDetailsDAO {
        int insertCalls;
        int markAsReceivedCalls;
        PurchaseDetails lastInsertedDetail;
        PurchaseDetails detailByPurchaseAndBatch;
        List<PurchaseDetails> pending = List.of();

        @Override
        public boolean insertPurchaseDetail(Connection conn, PurchaseDetails detail) {
            insertCalls++;
            lastInsertedDetail = detail;
            return true;
        }

        @Override
        public PurchaseDetails getPurchaseDetailByPurchaseAndBatch(Connection conn, int purchaseId, int batchId) {
            return detailByPurchaseAndBatch;
        }

        @Override
        public boolean markAsReceived(Connection conn, int purchaseId, int batchId) {
            markAsReceivedCalls++;
            return true;
        }

        @Override
        public List<PurchaseDetails> getPendingPurchaseDetails(Connection conn) {
            return pending;
        }
    }

    private static class StubBatchDAO extends BatchDAO {
        int insertCalls;
        int addBatchQuantityCalls;
        boolean addBatchQuantityResult = true;
        Batch lastInsertedBatch;

        @Override
        public int insertBatch(Connection conn, Batch batch) {
            insertCalls++;
            lastInsertedBatch = batch;
            return 9001;
        }

        @Override
        public boolean addBatchQuantity(Connection conn, int batchId, int quantityToAdd) {
            addBatchQuantityCalls++;
            return addBatchQuantityResult;
        }
    }

    private static class StubInventoryDAO extends InventoryDAO {
        boolean addQuantityResult = true;
        int createCalls;

        @Override
        public boolean addQuantity(Connection conn, int medicineId, int amount) {
            return addQuantityResult;
        }

        @Override
        public boolean createInventoryForMedicine(Connection conn, int medicineId, int quantity, int reorderThreshold) {
            createCalls++;
            return true;
        }
    }
}
