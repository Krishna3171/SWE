package com.msa.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalesDetails {

    private int saleId;
    private int medicineId;
    private int quantitySold;
    private BigDecimal price;
    private LocalDate saleDate;

    public SalesDetails() {
    }

    public SalesDetails(int saleId, int medicineId,
            int quantitySold, BigDecimal price, LocalDate saleDate) {
        this.saleId = saleId;
        this.medicineId = medicineId;
        this.quantitySold = quantitySold;
        this.price = price;
        this.saleDate = saleDate;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getUnitSalePrice() {
        return price;
    }

    public void setUnitSalePrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantitySold;
    }

    public void setQuantity(int quantity) {
        this.quantitySold = quantity;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }
}
