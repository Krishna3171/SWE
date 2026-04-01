package com.msa.dto;

import java.util.List;

public class ReorderReport {

    private List<ReorderItem> reorderItems;
    private int totalItems;

    public ReorderReport(List<ReorderItem> reorderItems, int totalItems) {
        this.reorderItems = reorderItems;
        this.totalItems = totalItems;
    }

    public List<ReorderItem> getReorderItems() { return reorderItems; }
    public int getTotalItems() { return totalItems; }
}
