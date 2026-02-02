package com.msa.model;

public class VendorMedicine {

    private int vendorId;
    private int medicineId;

    public VendorMedicine() {}

    public VendorMedicine(int vendorId, int medicineId) {
        this.vendorId = vendorId;
        this.medicineId = medicineId;
    }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
}
