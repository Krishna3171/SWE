package com.msa.model;

import java.math.BigDecimal;

public class Medicine {

    private int medicineId;
    private String medicineCode;
    private String tradeName;
    private String genericName;
    private BigDecimal unitSellingPrice;
    private BigDecimal unitPurchasePrice;

    public Medicine() {}

    public Medicine(String medicineCode, String tradeName,
                    String genericName, BigDecimal unitSellingPrice,
                    BigDecimal unitPurchasePrice) {
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

    public BigDecimal getUnitSellingPrice() { return unitSellingPrice; }
    public void setUnitSellingPrice(BigDecimal unitSellingPrice) {
        this.unitSellingPrice = unitSellingPrice;
    }

    public BigDecimal getUnitPurchasePrice() { return unitPurchasePrice; }
    public void setUnitPurchasePrice(BigDecimal unitPurchasePrice) {
        this.unitPurchasePrice = unitPurchasePrice;
    }
}
