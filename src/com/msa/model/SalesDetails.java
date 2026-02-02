package com.msa.model;

public class SalesDetails {

    private int saleId;
    private int medicineId;
    private int quantitySold;
    private double price;

    public SalesDetails() {}

    public SalesDetails(int saleId, int medicineId,
                        int quantitySold, double price) {
        this.saleId = saleId;
        this.medicineId = medicineId;
        this.quantitySold = quantitySold;
        this.price = price;
    }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getQuantitySold() { return quantitySold; }
    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
