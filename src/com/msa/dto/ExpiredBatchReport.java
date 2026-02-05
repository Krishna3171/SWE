package com.msa.dto;

import java.util.List;

public class ExpiredBatchReport {
    private List<ExpiredBatchReportItem> items;
    private int totalBatchesDiscarded;
    private int totalUnitsDiscarded;

    public ExpiredBatchReport() {
    }

    public ExpiredBatchReport(List<ExpiredBatchReportItem> items,
                              int totalBatchesDiscarded,
                              int totalUnitsDiscarded) {
        this.items = items;
        this.totalBatchesDiscarded = totalBatchesDiscarded;
        this.totalUnitsDiscarded = totalUnitsDiscarded;
    }

    public List<ExpiredBatchReportItem> getItems() {
        return items;
    }

    public int getTotalBatchesDiscarded() {
        return totalBatchesDiscarded;
    }

    public int getTotalUnitsDiscarded() {
        return totalUnitsDiscarded;
    }

}
