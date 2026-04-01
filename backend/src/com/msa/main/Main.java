package com.msa.main;

import com.msa.api.ApiServer;
import com.msa.config.ServerConfig;
import com.msa.dto.*;
import com.msa.service.ExpiredBatchDiscardService;
import com.msa.service.ProfitReportService;
import com.msa.service.PurchaseService;
import com.msa.service.ReorderService;
import com.msa.service.SalesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (shouldRunDemo(args)) {
            runDemo();
            return;
        }

        startApiServer();
    }

    private static boolean shouldRunDemo(String[] args) {
        if (args == null) {
            return false;
        }

        for (String arg : args) {
            if ("demo".equalsIgnoreCase(arg) || "--demo".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void startApiServer() {
        int port = ServerConfig.getPort();

        try {
            ApiServer apiServer = new ApiServer(port);
            apiServer.start();

            System.out.println("MSA API server started on port " + port);
            System.out.println("Health endpoint: http://localhost:" + port + "/api/health");
            System.out.println("Login endpoint: http://localhost:" + port + "/api/users/login");

            Runtime.getRuntime().addShutdownHook(new Thread(apiServer::stop));
        } catch (Exception e) {
            System.out.println("Failed to start API server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDemo() {
        PurchaseService purchaseService = new PurchaseService();
        SalesService salesService = new SalesService();
        ProfitReportService profitReportService = new ProfitReportService();
        ExpiredBatchDiscardService expiredBatchDiscardService = new ExpiredBatchDiscardService();
        ReorderService reorderService = new ReorderService();

        System.out.println("================= DEMO START =================");

        // 1) Purchase demo (requires existing vendor/medicine mappings in DB)
        System.out.println("\n[1] Purchase Service Demo");
        try {
            String futureExpiry = LocalDate.now().plusMonths(18).toString();
            PurchaseRequest purchaseRequest = new PurchaseRequest(
                    1,
                    List.of(
                            new PurchaseItemRequest("MED001", "BATCH-DEMO-001", futureExpiry, 20,
                                    new BigDecimal("8.50"))));

            PurchaseResponse purchaseResponse = purchaseService.makePurchase(purchaseRequest);
            System.out.println("Purchase OK -> ID: " + purchaseResponse.getPurchaseId()
                    + ", Total: " + purchaseResponse.getTotalAmount());
        } catch (Exception e) {
            System.out.println("Purchase demo skipped/failed: " + e.getMessage());
        }

        // 2) Sales demo (requires available stock in DB)
        System.out.println("\n[2] Sales Service Demo");
        try {
            SaleRequest saleRequest = new SaleRequest(
                    List.of(new SaleItemRequest("MED001", 2)));

            SaleResponse saleResponse = salesService.makeSale(saleRequest);
            System.out.println("Sale OK -> ID: " + saleResponse.getSaleId()
                    + ", Total: " + saleResponse.getTotalAmount());
        } catch (Exception e) {
            System.out.println("Sales demo skipped/failed: " + e.getMessage());
        }

        // 3) Profit report demo (works even when there are no transactions)
        System.out.println("\n[3] Profit Report Service Demo");
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            ProfitReportRequest reportRequest = new ProfitReportRequest(start, end);
            ProfitReportResponse report = profitReportService.generateProfitReport(reportRequest);

            System.out.println("Profit Report OK -> Range: " + report.getStartDate() + " to " + report.getEndDate());
            System.out.println("Revenue: " + report.getTotalSalesRevenue());
            System.out.println("Purchase Cost: " + report.getTotalPurchaseCost());
            System.out.println("Profit: " + report.getTotalProfit());
            System.out.println("Profit Margin (%): " + report.getProfitMargin());
            System.out.println("Medicine Breakdown Count: " + report.getMedicineProfits().size());
            System.out.println("Vendor Breakdown Count: " + report.getVendorProfits().size());
        } catch (Exception e) {
            System.out.println("Profit report demo failed: " + e.getMessage());
        }

        // 4) Expired batch discard demo
        System.out.println("\n[4] Expired Batch Discard Service Demo");
        try {
            ExpiredBatchReport expiredReport = expiredBatchDiscardService.discardExpiredBatches();
            System.out.println("Expired Discard OK -> Batches: " + expiredReport.getTotalBatchesDiscarded()
                    + ", Units: " + expiredReport.getTotalUnitsDiscarded());

            if (!expiredReport.getItems().isEmpty()) {
                ExpiredBatchReportItem first = expiredReport.getItems().get(0);
                System.out.println("First discarded item -> Medicine: " + first.getMedicineCode()
                        + ", Batch: " + first.getBatchNumber());
            }
        } catch (Exception e) {
            System.out.println("Expired batch demo failed: " + e.getMessage());
        }

        // 5) Reorder report demo
        System.out.println("\n[5] Reorder Service Demo");
        try {
            ReorderReport reorderReport = reorderService.generateReorderReport();
            System.out.println("Reorder Report OK -> Total Low Stock Items: " + reorderReport.getTotalItems());

            if (!reorderReport.getReorderItems().isEmpty()) {
                ReorderItem first = reorderReport.getReorderItems().get(0);
                System.out.println("First reorder item -> Medicine: " + first.getMedicineCode()
                        + ", Current: " + first.getCurrentStock()
                        + ", Threshold: " + first.getReorderThreshold()
                        + ", Recommended: " + first.getRecommendedOrderQty()
                        + ", Vendors: " + first.getVendorIds());
            }
        } catch (Exception e) {
            System.out.println("Reorder demo failed: " + e.getMessage());
        }

        System.out.println("\n================= DEMO END =================");
    }
}
