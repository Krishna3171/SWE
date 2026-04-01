package com.msa.dto;
import java.time.LocalDate;

public class ExpiredBatchReportItem {
    private String medicineCode;
    private String batchNumber;
    private LocalDate expiryDate;
    private int quantityDiscarded;
    private int vendorId;

    public ExpiredBatchReportItem() {
    }   

    public ExpiredBatchReportItem(String medicineCode, String batchNumber,
                                  LocalDate expiryDate, int quantityDiscarded,
                                  int vendorId) {
        this.medicineCode = medicineCode;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantityDiscarded = quantityDiscarded;
        this.vendorId = vendorId;
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getQuantityDiscarded() {
        return quantityDiscarded;
    }

    public int getVendorId() {
        return vendorId;
    }
}
