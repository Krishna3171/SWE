package com.msa.dto;


public class BatchAllocationPlan {
    private int batchId;
    private int quantityToConsume;

    public BatchAllocationPlan(int batchId, int quantityToConsume) {
        this.batchId = batchId;
        this.quantityToConsume = quantityToConsume;
    }

    public int getBatchId() {
        return batchId;
    }

    public int getQuantityToConsume() {
        return quantityToConsume;
    }
}
