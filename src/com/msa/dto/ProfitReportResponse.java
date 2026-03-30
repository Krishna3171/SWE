package com.msa.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for profit report with detailed breakdown
 */
public class ProfitReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalSalesRevenue;
    private BigDecimal totalPurchaseCost;
    private BigDecimal totalProfit;
    private BigDecimal profitMargin; // percentage
    private List<MedicineProfitDetail> medicineProfits;
    private List<VendorProfitDetail> vendorProfits;
    private String message;

    public ProfitReportResponse() {
    }

    public ProfitReportResponse(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalSalesRevenue,
            BigDecimal totalPurchaseCost,
            BigDecimal totalProfit,
            BigDecimal profitMargin,
            List<MedicineProfitDetail> medicineProfits,
            List<VendorProfitDetail> vendorProfits,
            String message) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalSalesRevenue = totalSalesRevenue;
        this.totalPurchaseCost = totalPurchaseCost;
        this.totalProfit = totalProfit;
        this.profitMargin = profitMargin;
        this.medicineProfits = medicineProfits;
        this.vendorProfits = vendorProfits;
        this.message = message;
    }

    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalSalesRevenue() {
        return totalSalesRevenue;
    }

    public void setTotalSalesRevenue(BigDecimal totalSalesRevenue) {
        this.totalSalesRevenue = totalSalesRevenue;
    }

    public BigDecimal getTotalPurchaseCost() {
        return totalPurchaseCost;
    }

    public void setTotalPurchaseCost(BigDecimal totalPurchaseCost) {
        this.totalPurchaseCost = totalPurchaseCost;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public List<MedicineProfitDetail> getMedicineProfits() {
        return medicineProfits;
    }

    public void setMedicineProfits(List<MedicineProfitDetail> medicineProfits) {
        this.medicineProfits = medicineProfits;
    }

    public List<VendorProfitDetail> getVendorProfits() {
        return vendorProfits;
    }

    public void setVendorProfits(List<VendorProfitDetail> vendorProfits) {
        this.vendorProfits = vendorProfits;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Nested DTOs
    public static class MedicineProfitDetail {
        private int medicineId;
        private String medicineName;
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
        private BigDecimal profit;
        private BigDecimal profitMargin;
        private int totalQuantity;

        public MedicineProfitDetail() {
        }

        public MedicineProfitDetail(
                int medicineId,
                String medicineName,
                BigDecimal totalRevenue,
                BigDecimal totalCost,
                BigDecimal profit,
                BigDecimal profitMargin,
                int totalQuantity) {
            this.medicineId = medicineId;
            this.medicineName = medicineName;
            this.totalRevenue = totalRevenue;
            this.totalCost = totalCost;
            this.profit = profit;
            this.profitMargin = profitMargin;
            this.totalQuantity = totalQuantity;
        }

        // Getters and Setters
        public int getMedicineId() {
            return medicineId;
        }

        public void setMedicineId(int medicineId) {
            this.medicineId = medicineId;
        }

        public String getMedicineName() {
            return medicineName;
        }

        public void setMedicineName(String medicineName) {
            this.medicineName = medicineName;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public void setProfit(BigDecimal profit) {
            this.profit = profit;
        }

        public BigDecimal getProfitMargin() {
            return profitMargin;
        }

        public void setProfitMargin(BigDecimal profitMargin) {
            this.profitMargin = profitMargin;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public void setTotalQuantity(int totalQuantity) {
            this.totalQuantity = totalQuantity;
        }
    }

    public static class VendorProfitDetail {
        private int vendorId;
        private String vendorName;
        private BigDecimal totalPurchaseCost;

        public VendorProfitDetail() {
        }

        public VendorProfitDetail(int vendorId, String vendorName, BigDecimal totalPurchaseCost) {
            this.vendorId = vendorId;
            this.vendorName = vendorName;
            this.totalPurchaseCost = totalPurchaseCost;
        }

        // Getters and Setters
        public int getVendorId() {
            return vendorId;
        }

        public void setVendorId(int vendorId) {
            this.vendorId = vendorId;
        }

        public String getVendorName() {
            return vendorName;
        }

        public void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }

        public BigDecimal getTotalPurchaseCost() {
            return totalPurchaseCost;
        }

        public void setTotalPurchaseCost(BigDecimal totalPurchaseCost) {
            this.totalPurchaseCost = totalPurchaseCost;
        }
    }
}