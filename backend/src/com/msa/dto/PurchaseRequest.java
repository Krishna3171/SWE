package com.msa.dto;
import java.util.List;

public class PurchaseRequest {
    private int vendorId;
    private List<PurchaseItemRequest> items;

    public PurchaseRequest() {}

    public PurchaseRequest(int vendorId, List<PurchaseItemRequest> items) {
        this.vendorId = vendorId;
        this.items = items;
    }

    public int getVendorId() {
        return vendorId;
    }

    public List<PurchaseItemRequest> getItems() {
        return items;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public void setItems(List<PurchaseItemRequest> items) {
        this.items = items;
    }

}
