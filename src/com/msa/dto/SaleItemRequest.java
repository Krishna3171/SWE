package com.msa.dto;

public class SaleItemRequest {

    private String medicineCode;
    private int quantity;

    public SaleItemRequest() {
    }

    public SaleItemRequest(String medicineCode, int quantity) {
        this.medicineCode = medicineCode;
        this.quantity = quantity;
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public void setMedicineCode(String medicineCode) {
        this.medicineCode = medicineCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
