package com.msa.dto;

import java.math.BigDecimal;

/**
 * Response DTO for individual medicine profit report
 */
public class MedicineProfitReportResponse {
    
    private int medicineId;
    private String medicineName;
    private BigDecimal totalSalesRevenue;
    private BigDecimal totalPurchaseCost;
    private BigDecimal profit;
    private BigDecimal profitMargin; // percentage
    private String message;

    public MedicineProfitReportResponse() {
    }

    public MedicineProfitReportResponse(
            int medicineId,
            String medicineName,
            BigDecimal totalSalesRevenue,
            BigDecimal totalPurchaseCost,
            BigDecimal profit,
            BigDecimal profitMargin,
            String message
    ) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.totalSalesRevenue = totalSalesRevenue;
        this.totalPurchaseCost = totalPurchaseCost;
        this.profit = profit;
        this.profitMargin = profitMargin;
        this.message = message;
    }

    // Getters and Setters
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public BigDecimal getTotalSalesRevenue() { return totalSalesRevenue; }
    public void setTotalSalesRevenue(BigDecimal totalSalesRevenue) { this.totalSalesRevenue = totalSalesRevenue; }

    public BigDecimal getTotalPurchaseCost() { return totalPurchaseCost; }
    public void setTotalPurchaseCost(BigDecimal totalPurchaseCost) { this.totalPurchaseCost = totalPurchaseCost; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }

    public BigDecimal getProfitMargin() { return profitMargin; }
    public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}