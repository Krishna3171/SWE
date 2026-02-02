package com.msa.model;

public class Medicine {

    private int medicineId;
    private String medicineCode;
    private String tradeName;
    private String genericName;
    private double unitSellingPrice;
    private double unitPurchasePrice;

    public Medicine() {}

    public Medicine(String medicineCode, String tradeName,
                    String genericName, double unitSellingPrice,
                    double unitPurchasePrice) {
        this.medicineCode = medicineCode;
        this.tradeName = tradeName;
        this.genericName = genericName;
        this.unitSellingPrice = unitSellingPrice;
        this.unitPurchasePrice = unitPurchasePrice;
    }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineCode() { return medicineCode; }
    public void setMedicineCode(String medicineCode) { this.medicineCode = medicineCode; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public double getUnitSellingPrice() { return unitSellingPrice; }
    public void setUnitSellingPrice(double unitSellingPrice) {
        this.unitSellingPrice = unitSellingPrice;
    }

    public double getUnitPurchasePrice() { return unitPurchasePrice; }
    public void setUnitPurchasePrice(double unitPurchasePrice) {
        this.unitPurchasePrice = unitPurchasePrice;
    }
}
