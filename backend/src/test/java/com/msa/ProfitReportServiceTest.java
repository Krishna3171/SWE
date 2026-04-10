package com.msa;

import com.msa.dao.MedicineDAO;
import com.msa.dao.PurchaseDAO;
import com.msa.dao.PurchaseDetailsDAO;
import com.msa.dao.SalesDAO;
import com.msa.dao.SalesDetailsDAO;
import com.msa.dao.VendorDAO;
import com.msa.dto.MedicineProfitReportResponse;
import com.msa.dto.ProfitReportRequest;
import com.msa.dto.ProfitReportResponse;
import com.msa.model.Medicine;
import com.msa.model.Purchase;
import com.msa.model.PurchaseDetails;
import com.msa.model.Sales;
import com.msa.model.SalesDetails;
import com.msa.model.Vendor;
import com.msa.service.ProfitReportService;
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

public class ProfitReportServiceTest {

    @Test
    public void generateProfitReportComputesTotalsAndBreakdowns() {
        TxnState txn = new TxnState();
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(txn, closed);
        Supplier<Connection> provider = () -> conn;

        StubSalesDAO salesDAO = new StubSalesDAO();
        salesDAO.salesBetweenDates = List.of(sale(100, "50.00"));
        StubPurchaseDAO purchaseDAO = new StubPurchaseDAO();
        purchaseDAO.purchasesBetweenDates = List.of(purchase(10, "30.00"));
        StubPurchaseDetailsDAO purchaseDetailsDAO = new StubPurchaseDetailsDAO();
        purchaseDetailsDAO.priceInRange = new BigDecimal("3.00");
        StubSalesDetailsDAO salesDetailsDAO = new StubSalesDetailsDAO();
        salesDetailsDAO.bySaleId.put(100, List.of(salesDetail(1, 5, "10.00")));
        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byId.put(1, medicine(1, "Aspirin"));
        StubVendorDAO vendorDAO = new StubVendorDAO();
        vendorDAO.byId.put(10, vendor(10, "BestVendor"));

        ProfitReportService service = new ProfitReportService(
                salesDAO, purchaseDAO, purchaseDetailsDAO, salesDetailsDAO, medicineDAO, vendorDAO, provider);

        ProfitReportResponse response = service.generateProfitReport(
                new ProfitReportRequest(LocalDate.now().minusDays(7), LocalDate.now()));

        assertEquals(0, new BigDecimal("50.00").compareTo(response.getTotalSalesRevenue()));
        assertEquals(0, new BigDecimal("30.00").compareTo(response.getTotalPurchaseCost()));
        assertEquals(0, new BigDecimal("20.00").compareTo(response.getTotalProfit()));
        assertEquals(1, response.getMedicineProfits().size());
        assertEquals(1, response.getVendorProfits().size());
        assertTrue(txn.committed);
        assertTrue(closed.get());
    }

    @Test
    public void generateMedicineProfitReportReturnsValues() {
        TxnState txn = new TxnState();
        Connection conn = stubConnection(txn, new AtomicBoolean(false));
        Supplier<Connection> provider = () -> conn;

        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        medicineDAO.byId.put(1, medicine(1, "Aspirin"));
        StubSalesDetailsDAO salesDetailsDAO = new StubSalesDetailsDAO();
        salesDetailsDAO.byMedicineId.put(1, List.of(salesDetail(1, 4, "10.00")));
        StubPurchaseDetailsDAO purchaseDetailsDAO = new StubPurchaseDetailsDAO();
        purchaseDetailsDAO.byMedicineId.put(1, List.of(purchaseDetail(1, 4, "6.00")));

        ProfitReportService service = new ProfitReportService(
                new StubSalesDAO(), new StubPurchaseDAO(), purchaseDetailsDAO, salesDetailsDAO,
                medicineDAO, new StubVendorDAO(), provider);

        MedicineProfitReportResponse response = service.generateMedicineProfitReport(1);

        assertEquals(1, response.getMedicineId());
        assertEquals(0, new BigDecimal("40.00").compareTo(response.getTotalSalesRevenue()));
        assertEquals(0, new BigDecimal("24.00").compareTo(response.getTotalPurchaseCost()));
        assertEquals(0, new BigDecimal("16.00").compareTo(response.getProfit()));
        assertTrue(txn.committed);
    }

    @Test
    public void generateMedicineProfitReportThrowsWhenMedicineMissing() {
        Supplier<Connection> provider = () -> stubConnection(new TxnState(), new AtomicBoolean(false));
        ProfitReportService service = new ProfitReportService(
                new StubSalesDAO(), new StubPurchaseDAO(), new StubPurchaseDetailsDAO(),
                new StubSalesDetailsDAO(), new StubMedicineDAO(), new StubVendorDAO(), provider);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.generateMedicineProfitReport(999));
        assertTrue(ex.getMessage().contains("Medicine profit report generation failed"));
    }

    private static Connection stubConnection(TxnState txn, AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setAutoCommit": return null;
                        case "commit": txn.committed = true; return null;
                        case "rollback": txn.rolledBack = true; return null;
                        case "close": closed.set(true); return null;
                        default: return null;
                    }
                });
    }

    private static Sales sale(int id, String total) {
        Sales s = new Sales();
        s.setSaleId(id);
        s.setTotalAmount(new BigDecimal(total));
        return s;
    }
    private static Purchase purchase(int vendorId, String total) {
        Purchase p = new Purchase();
        p.setVendorId(vendorId);
        p.setTotalAmount(new BigDecimal(total));
        return p;
    }
    private static SalesDetails salesDetail(int medicineId, int qty, String unitPrice) {
        SalesDetails sd = new SalesDetails();
        sd.setMedicineId(medicineId);
        sd.setQuantity(qty);
        sd.setUnitSalePrice(new BigDecimal(unitPrice));
        return sd;
    }
    private static PurchaseDetails purchaseDetail(int medicineId, int qty, String unitPrice) {
        PurchaseDetails pd = new PurchaseDetails();
        pd.setMedicineId(medicineId);
        pd.setQuantity(qty);
        pd.setUnitPurchasePrice(new BigDecimal(unitPrice));
        return pd;
    }
    private static Medicine medicine(int id, String name) {
        Medicine m = new Medicine();
        m.setMedicineId(id);
        m.setMedicineName(name);
        return m;
    }
    private static Vendor vendor(int id, String name) {
        Vendor v = new Vendor();
        v.setVendorId(id);
        v.setVendorName(name);
        return v;
    }

    private static class TxnState { boolean committed; boolean rolledBack; }

    private static class StubSalesDAO extends SalesDAO {
        List<Sales> salesBetweenDates = List.of();
        @Override
        public List<Sales> getSalesBetweenDates(Connection conn, LocalDate startDate, LocalDate endDate) {
            return salesBetweenDates;
        }
    }
    private static class StubPurchaseDAO extends PurchaseDAO {
        List<Purchase> purchasesBetweenDates = List.of();
        @Override
        public List<Purchase> getPurchasesBetweenDates(Connection conn, LocalDate startDate, LocalDate endDate) {
            return purchasesBetweenDates;
        }
    }
    private static class StubPurchaseDetailsDAO extends PurchaseDetailsDAO {
        BigDecimal priceInRange;
        Map<Integer, List<PurchaseDetails>> byMedicineId = new HashMap<>();
        @Override
        public BigDecimal getAveragePurchasePriceForMedicineInDateRange(Connection conn, int medicineId, LocalDate startDate, LocalDate endDate) {
            return priceInRange;
        }
        @Override
        public BigDecimal getAveragePurchasePriceForMedicine(Connection conn, int medicineId) {
            return new BigDecimal("0.00");
        }
        @Override
        public List<PurchaseDetails> getPurchaseDetailsByMedicineId(Connection conn, int medicineId) {
            return byMedicineId.getOrDefault(medicineId, List.of());
        }
    }
    private static class StubSalesDetailsDAO extends SalesDetailsDAO {
        Map<Integer, List<SalesDetails>> bySaleId = new HashMap<>();
        Map<Integer, List<SalesDetails>> byMedicineId = new HashMap<>();
        @Override
        public List<SalesDetails> getSalesDetailsBySalesId(Connection conn, int saleId) {
            return bySaleId.getOrDefault(saleId, List.of());
        }
        @Override
        public List<SalesDetails> getSalesDetailsByMedicineId(Connection conn, int medicineId) {
            return byMedicineId.getOrDefault(medicineId, List.of());
        }
    }
    private static class StubMedicineDAO extends MedicineDAO {
        Map<Integer, Medicine> byId = new HashMap<>();
        @Override
        public Medicine getMedicineById(Connection conn, int medicineId) {
            return byId.get(medicineId);
        }
    }
    private static class StubVendorDAO extends VendorDAO {
        Map<Integer, Vendor> byId = new HashMap<>();
        @Override
        public Vendor getVendorById(Connection conn, int vendorId) {
            return byId.get(vendorId);
        }
    }
}
