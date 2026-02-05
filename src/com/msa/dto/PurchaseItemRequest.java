package com.msa.dto;
import java.math.BigDecimal;

public class PurchaseItemRequest {

    private String medicineCode;
    private String batchNumber;
    private String expiryDate;
    private int quantity;
    private BigDecimal unitPurchasePrice;

    public PurchaseItemRequest() {
    }

    public PurchaseItemRequest(String medicineCode, String batchNumber,
                               String expiryDate, int quantity,
                               BigDecimal unitPurchasePrice) {
        this.medicineCode = medicineCode;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.unitPurchasePrice = unitPurchasePrice;
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public void setMedicineCode(String medicineCode) {
        this.medicineCode = medicineCode;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public BigDecimal getUnitPurchasePrice() {
        return unitPurchasePrice;
    }

    public void setUnitPurchasePrice(BigDecimal unitPurchasePrice) {
        this.unitPurchasePrice = unitPurchasePrice;
    }
    
}
