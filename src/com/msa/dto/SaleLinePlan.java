package com.msa.dto;

import java.math.BigDecimal;

public class SaleLinePlan {

    private int medicineId;
    private String medicineCode;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;

    public SaleLinePlan() {}

    public SaleLinePlan(int medicineId, String medicineCode,
                        BigDecimal unitPrice, int quantity,
                        BigDecimal lineTotal) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
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
}
