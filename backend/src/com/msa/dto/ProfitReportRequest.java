package com.msa.dto;

import java.time.LocalDate;

/**
 * Request DTO for profit report generation
 */
public class ProfitReportRequest {
    
    private LocalDate startDate;
    private LocalDate endDate;

    public ProfitReportRequest() {
    }

    public ProfitReportRequest(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}