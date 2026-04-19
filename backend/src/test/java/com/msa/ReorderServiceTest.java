package com.msa;

import com.msa.dao.InventoryDAO;
import com.msa.dao.MedicineDAO;
import com.msa.dao.SalesDetailsDAO;
import com.msa.dao.VendorMedicineDAO;
import com.msa.dto.ReorderReport;
import com.msa.model.Inventory;
import com.msa.model.Medicine;
import com.msa.service.ReorderService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ReorderServiceTest {

    @Test
    public void generateReorderReportBuildsItemsAndUsesUnknownFallback() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Connection conn = stubConnection(closed);
        Supplier<Connection> provider = () -> conn;

        StubInventoryDAO inventoryDAO = new StubInventoryDAO();
        inventoryDAO.lowStock = List.of(
                new Inventory(1, 2, 10),
                new Inventory(2, 1, 5));

        StubMedicineDAO medicineDAO = new StubMedicineDAO();
        Medicine m1 = new Medicine();
        m1.setMedicineId(1);
        m1.setMedicineCode("MED1");
        medicineDAO.byId.put(1, m1);
        medicineDAO.byId.put(2, null);

        StubVendorMedicineDAO vendorMedicineDAO = new StubVendorMedicineDAO();
        vendorMedicineDAO.byMedicine.put(1, List.of(7, 8));
        vendorMedicineDAO.byMedicine.put(2, List.of(9));

        ReorderService service = new ReorderService(inventoryDAO, medicineDAO, vendorMedicineDAO, new StubSalesDetailsDAO(), provider);
        ReorderReport report = service.generateReorderReport();

        assertEquals(2, report.getTotalItems());
        assertEquals("MED1", report.getReorderItems().get(0).getMedicineCode());
        assertEquals("UNKNOWN-2", report.getReorderItems().get(1).getMedicineCode());
        assertEquals(8, report.getReorderItems().get(0).getRecommendedOrderQty());
        assertTrue(closed.get());
    }

    @Test
    public void generateReorderReportWrapsFailure() {
        Supplier<Connection> provider = () -> {
            throw new RuntimeException("db down");
        };

        ReorderService service = new ReorderService(
                new StubInventoryDAO(), new StubMedicineDAO(), new StubVendorMedicineDAO(), new StubSalesDetailsDAO(), provider);

        RuntimeException ex = assertThrows(RuntimeException.class, service::generateReorderReport);
        assertTrue(ex.getMessage().contains("Failed to generate reorder report"));
    }

    private static Connection stubConnection(AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        closed.set(true);
                    }
                    return null;
                });
    }

    private static class StubInventoryDAO extends InventoryDAO {
        List<Inventory> lowStock = List.of();
        @Override
        public List<Inventory> getLowStockMedicines(Connection conn) {
            return lowStock;
        }
    }

    private static class StubMedicineDAO extends MedicineDAO {
        Map<Integer, Medicine> byId = new HashMap<>();
        @Override
        public Medicine getMedicineById(Connection conn, int medicineId) {
            return byId.get(medicineId);
        }
    }

    private static class StubVendorMedicineDAO extends VendorMedicineDAO {
        Map<Integer, List<Integer>> byMedicine = new HashMap<>();
        @Override
        public List<Integer> getVendorsForMedicine(Connection conn, int medicineId) {
            return byMedicine.getOrDefault(medicineId, List.of());
        }
    }

    private static class StubSalesDetailsDAO extends SalesDetailsDAO {
        @Override
        public Integer getAverageDailySalesLast7Days(Connection conn, int medicineId) {
            return null; // no sales data — service falls back to static threshold
        }
    }
}
