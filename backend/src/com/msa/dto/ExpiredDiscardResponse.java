package com.msa.dto;

public class ExpiredDiscardResponse {

    private int batchesDiscarded;
    private int unitsDiscarded;

    public ExpiredDiscardResponse(int batchesDiscarded, int unitsDiscarded) {
        this.batchesDiscarded = batchesDiscarded;
        this.unitsDiscarded = unitsDiscarded;
    }

    public int getBatchesDiscarded() {
        return batchesDiscarded;
    }

    public int getUnitsDiscarded() {
        return unitsDiscarded;
    }
}
