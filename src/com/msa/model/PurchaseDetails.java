package com.msa.model;

import java.math.BigDecimal;
public class PurchaseDetails {

    private int purchaseId;
    private int medicineId;
    private int quantity;
    private BigDecimal unitPrice;
    private int batchId;

    public PurchaseDetails() {}

    public PurchaseDetails(int purchaseId, int medicineId,
                           int quantity, BigDecimal unitPrice, int batchId) {
        this.purchaseId = purchaseId;
        this.medicineId = medicineId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.batchId = batchId;
    }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
}
