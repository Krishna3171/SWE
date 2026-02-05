package com.msa.dto;
import java.math.BigDecimal;

public class PurchaseResponse {

    private final int purchaseId;
    private final BigDecimal totalAmount;
    private final String message;

    public PurchaseResponse(int purchaseId, BigDecimal totalAmount, String message) {
        this.purchaseId = purchaseId;
        this.totalAmount = totalAmount;
        this.message = message;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getMessage() {
        return message;
    }
}