package com.msa.dto;

import java.util.List;

public class SaleRequest {

    private List<SaleItemRequest> items;

    public SaleRequest() {}

    public SaleRequest(List<SaleItemRequest> items) {
        this.items = items;
    }

    public List<SaleItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SaleItemRequest> items) {
        this.items = items;
    }
}
