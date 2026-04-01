package com.msa.dto;

import java.math.BigDecimal;

public class SaleResponse {

    private int saleId;
    private BigDecimal totalAmount;
    private String message;

    public SaleResponse() {}

    public SaleResponse(int saleId, BigDecimal totalAmount, String message) {
        this.saleId = saleId;
        this.totalAmount = totalAmount;
        this.message = message;
    }

    public int getSaleId() {
        return saleId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getMessage() {
        return message;
    }
}
