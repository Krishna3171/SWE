package com.msa.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Purchase {

    private int purchaseId;
    private LocalDate purchaseDate;
    private int vendorId;
    private BigDecimal totalAmount;

    public Purchase() {}

    public Purchase(LocalDate purchaseDate, int vendorId, BigDecimal totalAmount) {
        this.purchaseDate = purchaseDate;
        this.vendorId = vendorId;
        this.totalAmount = totalAmount;
    }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
