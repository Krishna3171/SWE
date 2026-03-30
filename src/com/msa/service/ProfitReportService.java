package com.msa.service;

import com.msa.dao.*;
import com.msa.db.DBConnection;
import com.msa.dto.*;
import com.msa.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ProfitReportService
 * Generates profit reports based on sales, purchases, and discounts
 * Formula: Profit = (Sales Revenue) - (Purchase Cost) - (Discounts)
 */
public class ProfitReportService {

    private final SalesDAO salesDAO = new SalesDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final PurchaseDetailsDAO purchaseDetailsDAO = new PurchaseDetailsDAO();
    private final SalesDetailsDAO salesDetailsDAO = new SalesDetailsDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    // ==========================
    // PUBLIC ENTRY POINTS
    // ==========================

    /**
     * Generate profit report for a specific date range
     */
    public ProfitReportResponse generateProfitReport(ProfitReportRequest request) {
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // ==========================
            // PHASE 1 — DATA RETRIEVAL
            // ==========================

            // 1️⃣ Get total sales revenue
            BigDecimal totalSalesRevenue = calculateTotalSalesRevenue(
                    conn,
                    request.getStartDate(),
                    request.getEndDate());

            // 2️⃣ Get total purchase cost
            BigDecimal totalPurchaseCost = calculateTotalPurchaseCost(
                    conn,
                    request.getStartDate(),
                    request.getEndDate());

            // 3️⃣ Get total discounts
            BigDecimal totalDiscounts = calculateTotalDiscounts(
                    conn,
                    request.getStartDate(),
                    request.getEndDate());

            // 4️⃣ Calculate profit
            BigDecimal totalProfit = totalSalesRevenue
                    .subtract(totalPurchaseCost)
                    .subtract(totalDiscounts);

            // 5️⃣ Calculate profit margin
            BigDecimal profitMargin = BigDecimal.ZERO;
            if (totalSalesRevenue.compareTo(BigDecimal.ZERO) > 0) {
                profitMargin = totalProfit
                        .divide(totalSalesRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            // ==========================
            // PHASE 2 — DETAILED BREAKDOWN
            // ==========================

            // Get profit by medicine
            List<MedicineProfitDetail> medicineProfits = calculateProfitByMedicine(
                    conn,
                    request.getStartDate(),
                    request.getEndDate());

            // Get profit by vendor
            List<VendorProfitDetail> vendorProfits = calculateProfitByVendor(
                    conn,
                    request.getStartDate(),
                    request.getEndDate());

            List<ProfitReportResponse.MedicineProfitDetail> medicineProfitDtos = toMedicineProfitDtos(medicineProfits);
            List<ProfitReportResponse.VendorProfitDetail> vendorProfitDtos = toVendorProfitDtos(vendorProfits);

            conn.commit();

            return new ProfitReportResponse(
                    request.getStartDate(),
                    request.getEndDate(),
                    totalSalesRevenue,
                    totalPurchaseCost,
                    totalDiscounts,
                    totalProfit,
                    profitMargin,
                    medicineProfitDtos,
                    vendorProfitDtos,
                    "Profit report generated successfully");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException(
                    "Profit report generation failed.",
                    e);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Generate profit report by specific medicine
     */
    public MedicineProfitReportResponse generateMedicineProfitReport(int medicineId) {
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Get medicine details
            Medicine medicine = medicineDAO.getMedicineById(conn, medicineId);
            if (medicine == null) {
                throw new RuntimeException("Medicine not found: " + medicineId);
            }

            // Get medicine sales revenue
            BigDecimal medicineRevenue = calculateMedicineSalesRevenue(conn, medicineId);

            // Get medicine purchase cost
            BigDecimal medicineCost = calculateMedicinePurchaseCost(conn, medicineId);

            // Calculate profit
            BigDecimal profit = medicineRevenue.subtract(medicineCost);

            // Calculate profit margin
            BigDecimal profitMargin = BigDecimal.ZERO;
            if (medicineRevenue.compareTo(BigDecimal.ZERO) > 0) {
                profitMargin = profit
                        .divide(medicineRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            conn.commit();

            return new MedicineProfitReportResponse(
                    medicineId,
                    medicine.getMedicineName(),
                    medicineRevenue,
                    medicineCost,
                    profit,
                    profitMargin,
                    "Medicine profit report generated successfully");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException(
                    "Medicine profit report generation failed.",
                    e);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ==========================
    // PRIVATE HELPER METHODS
    // ==========================

    /**
     * Calculate total sales revenue between dates
     */
    private BigDecimal calculateTotalSalesRevenue(Connection conn, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        List<Sales> sales = salesDAO.getSalesBetweenDates(conn, startDate, endDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Sales sale : sales) {
            totalRevenue = totalRevenue.add(sale.getTotalAmount());
        }
        return totalRevenue;
    }

    /**
     * Calculate total purchase cost between dates
     */
    private BigDecimal calculateTotalPurchaseCost(Connection conn, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        List<Purchase> purchases = purchaseDAO.getPurchasesBetweenDates(conn, startDate, endDate);

        BigDecimal totalCost = BigDecimal.ZERO;
        for (Purchase purchase : purchases) {
            totalCost = totalCost.add(purchase.getTotalAmount());
        }
        return totalCost;
    }

    /**
     * Calculate total discounts between dates
     */
    private BigDecimal calculateTotalDiscounts(Connection conn, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        List<Sales> sales = salesDAO.getSalesBetweenDates(conn, startDate, endDate);

        BigDecimal totalDiscounts = BigDecimal.ZERO;
        for (Sales sale : sales) {
            BigDecimal discount = sale.getDiscountAmount();
            if (discount != null) {
                totalDiscounts = totalDiscounts.add(discount);
            }
        }
        return totalDiscounts;
    }

    /**
     * Calculate profit breakdown by medicine
     */
    private List<MedicineProfitDetail> calculateProfitByMedicine(Connection conn, LocalDate startDate,
            LocalDate endDate)
            throws SQLException {
        List<MedicineProfitDetail> profitDetails = new ArrayList<>();

        // Get all sales between dates
        List<Sales> sales = salesDAO.getSalesBetweenDates(conn, startDate, endDate);

        // Group by medicine
        java.util.Map<Integer, MedicineProfitDetail> medicineMap = new java.util.HashMap<>();

        for (Sales sale : sales) {
            List<SalesDetails> saleItems = salesDetailsDAO.getSalesDetailsBySalesId(conn, sale.getSalesId());

            for (SalesDetails item : saleItems) {
                int medicineId = item.getMedicineId();
                BigDecimal salePrice = item.getUnitSalePrice();
                int quantity = item.getQuantity();

                // Get purchase cost (average or latest)
                BigDecimal purchasePrice = getPurchasePriceForMedicine(conn, medicineId);

                BigDecimal revenue = salePrice.multiply(BigDecimal.valueOf(quantity));
                BigDecimal cost = purchasePrice.multiply(BigDecimal.valueOf(quantity));

                MedicineProfitDetail detail = medicineMap.getOrDefault(
                        medicineId,
                        new MedicineProfitDetail(medicineId));
                detail.addRevenue(revenue);
                detail.addCost(cost);
                detail.addQuantity(quantity);

                medicineMap.put(medicineId, detail);
            }
        }

        profitDetails.addAll(medicineMap.values());
        return profitDetails;
    }

    /**
     * Calculate profit breakdown by vendor
     */
    private List<VendorProfitDetail> calculateProfitByVendor(Connection conn, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        List<VendorProfitDetail> vendorProfits = new ArrayList<>();
        java.util.Map<Integer, VendorProfitDetail> vendorMap = new java.util.HashMap<>();

        // Get all purchases between dates
        List<Purchase> purchases = purchaseDAO.getPurchasesBetweenDates(conn, startDate, endDate);

        for (Purchase purchase : purchases) {
            int vendorId = purchase.getVendorId();

            VendorProfitDetail detail = vendorMap.getOrDefault(
                    vendorId,
                    new VendorProfitDetail(vendorId));
            detail.addPurchaseCost(purchase.getTotalAmount());

            vendorMap.put(vendorId, detail);
        }

        vendorProfits.addAll(vendorMap.values());
        return vendorProfits;
    }

    /**
     * Get purchase price for a medicine (average or latest)
     */
    private BigDecimal getPurchasePriceForMedicine(Connection conn, int medicineId)
            throws SQLException {
        BigDecimal price = purchaseDetailsDAO.getAveragePurchasePriceForMedicine(conn, medicineId);
        return price != null ? price : BigDecimal.ZERO;
    }

    /**
     * Calculate sales revenue for specific medicine
     */
    private BigDecimal calculateMedicineSalesRevenue(Connection conn, int medicineId)
            throws SQLException {
        List<SalesDetails> items = salesDetailsDAO.getSalesDetailsByMedicineId(conn, medicineId);

        BigDecimal revenue = BigDecimal.ZERO;
        for (SalesDetails item : items) {
            BigDecimal lineTotal = item.getUnitSalePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            revenue = revenue.add(lineTotal);
        }
        return revenue;
    }

    /**
     * Calculate purchase cost for specific medicine
     */
    private BigDecimal calculateMedicinePurchaseCost(Connection conn, int medicineId)
            throws SQLException {
        List<PurchaseDetails> items = purchaseDetailsDAO.getPurchaseDetailsByMedicineId(conn, medicineId);

        BigDecimal cost = BigDecimal.ZERO;
        for (PurchaseDetails item : items) {
            BigDecimal lineTotal = item.getUnitPurchasePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            cost = cost.add(lineTotal);
        }
        return cost;
    }

    private List<ProfitReportResponse.MedicineProfitDetail> toMedicineProfitDtos(List<MedicineProfitDetail> details) {
        List<ProfitReportResponse.MedicineProfitDetail> dtos = new ArrayList<>();
        for (MedicineProfitDetail detail : details) {
            String medicineName = "Medicine-" + detail.getMedicineId();
            dtos.add(new ProfitReportResponse.MedicineProfitDetail(
                    detail.getMedicineId(),
                    medicineName,
                    detail.getTotalRevenue(),
                    detail.getTotalCost(),
                    detail.getProfit(),
                    detail.getProfitMargin(),
                    detail.getTotalQuantity()));
        }
        return dtos;
    }

    private List<ProfitReportResponse.VendorProfitDetail> toVendorProfitDtos(List<VendorProfitDetail> details) {
        List<ProfitReportResponse.VendorProfitDetail> dtos = new ArrayList<>();
        for (VendorProfitDetail detail : details) {
            dtos.add(new ProfitReportResponse.VendorProfitDetail(
                    detail.getVendorId(),
                    "",
                    detail.getTotalPurchaseCost()));
        }
        return dtos;
    }

    // ==========================
    // INTERNAL HELPER CLASSES
    // ==========================

    /**
     * Medicine profit detail breakdown
     */
    public static class MedicineProfitDetail {
        private int medicineId;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private int totalQuantity = 0;

        public MedicineProfitDetail(int medicineId) {
            this.medicineId = medicineId;
        }

        public void addRevenue(BigDecimal revenue) {
            this.totalRevenue = this.totalRevenue.add(revenue);
        }

        public void addCost(BigDecimal cost) {
            this.totalCost = this.totalCost.add(cost);
        }

        public void addQuantity(int qty) {
            this.totalQuantity += qty;
        }

        public BigDecimal getProfit() {
            return totalRevenue.subtract(totalCost);
        }

        public BigDecimal getProfitMargin() {
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                return getProfit()
                        .divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            return BigDecimal.ZERO;
        }

        // Getters
        public int getMedicineId() {
            return medicineId;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }
    }

    /**
     * Vendor profit detail breakdown
     */
    public static class VendorProfitDetail {
        private int vendorId;
        private BigDecimal totalPurchaseCost = BigDecimal.ZERO;

        public VendorProfitDetail(int vendorId) {
            this.vendorId = vendorId;
        }

        public void addPurchaseCost(BigDecimal cost) {
            this.totalPurchaseCost = this.totalPurchaseCost.add(cost);
        }

        // Getters
        public int getVendorId() {
            return vendorId;
        }

        public BigDecimal getTotalPurchaseCost() {
            return totalPurchaseCost;
        }
    }
}