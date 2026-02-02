package com.msa.model;

import java.time.LocalDate;

public class Sales {

    private int saleId;
    private LocalDate saleDate;
    private double totalAmount;

    public Sales() {
    }

    public Sales(LocalDate saleDate, double totalAmount) {
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
