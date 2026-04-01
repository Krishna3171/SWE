package com.msa.dto;

import java.math.BigDecimal;
import java.util.List;

public class SaleLinePlan {

    private int medicineId;
    private String medicineCode;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private List<BatchAllocationPlan> batchAllocationPlans;

    public SaleLinePlan() {}

    public SaleLinePlan(int medicineId, String medicineCode,
                        BigDecimal unitPrice, int quantity,
                        BigDecimal lineTotal, List<BatchAllocationPlan> batchAllocationPlans) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
        this.batchAllocationPlans = batchAllocationPlans;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public List<BatchAllocationPlan> getBatchAllocationPlans() {
        return batchAllocationPlans;
    }
}
