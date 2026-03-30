package com.msa.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Sales {

    private int saleId;
    private LocalDate saleDate;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;

    public Sales() {
    }

    public Sales(LocalDate saleDate, BigDecimal totalAmount) {
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getSalesId() {
        return saleId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
