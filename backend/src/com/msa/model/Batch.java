package com.msa.model;

import java.time.LocalDate;

public class Batch {

    private int batchId;
    private int medicineId;
    private String batchNumber;
    private LocalDate expiryDate;
    private int quantity;
    private int vendorId;

    public Batch() {}

    public Batch(int medicineId, String batchNumber,
                 LocalDate expiryDate, int quantity, int vendorId) {
        this.medicineId = medicineId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.vendorId = vendorId;
    }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }
}
