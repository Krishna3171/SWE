package com.msa.dto;

import java.util.List;

public class ReorderItem {

    private String medicineCode;
    private int currentStock;
    private int reorderThreshold;
    private int recommendedOrderQty;
    private List<Integer> vendorIds;

    public ReorderItem(String medicineCode,
                       int currentStock,
                       int reorderThreshold,
                       int recommendedOrderQty,
                       List<Integer> vendorIds) {
        this.medicineCode = medicineCode;
        this.currentStock = currentStock;
        this.reorderThreshold = reorderThreshold;
        this.recommendedOrderQty = recommendedOrderQty;
        this.vendorIds = vendorIds;
    }

    public String getMedicineCode() { return medicineCode; }
    public int getCurrentStock() { return currentStock; }
    public int getReorderThreshold() { return reorderThreshold; }
    public int getRecommendedOrderQty() { return recommendedOrderQty; }
    public List<Integer> getVendorIds() { return vendorIds; }
}
