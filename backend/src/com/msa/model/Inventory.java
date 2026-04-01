package com.msa.model;

public class Inventory {

    private int medicineId;
    private int quantityAvailable;
    private int reorderThreshold;

    public Inventory() {}

    public Inventory(int medicineId, int quantityAvailable, int reorderThreshold) {
        this.medicineId = medicineId;
        this.quantityAvailable = quantityAvailable;
        this.reorderThreshold = reorderThreshold;
    }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }
}
