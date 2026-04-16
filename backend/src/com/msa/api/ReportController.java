package com.msa.api;

import com.msa.dto.ExpiredBatchReport;
import com.msa.dto.ExpiredBatchReportItem;
import com.msa.dto.MedicineProfitReportResponse;
import com.msa.dto.ProfitReportRequest;
import com.msa.dto.ProfitReportResponse;
import com.msa.dto.ReorderItem;
import com.msa.dto.ReorderReport;
import com.msa.service.ExpiredBatchDiscardService;
import com.msa.service.ProfitReportService;
import com.msa.service.ReorderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportController extends BaseController implements HttpHandler {

    private static final int MAX_REQUEST_BYTES = 4096;

    private final ProfitReportService profitReportService;
    private final ExpiredBatchDiscardService expiredBatchDiscardService;
    private final ReorderService reorderService;

    public ReportController() {
        this.profitReportService = new ProfitReportService();
        this.expiredBatchDiscardService = new ExpiredBatchDiscardService();
        this.reorderService = new ReorderService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET, POST");

        if (isPreflight(exchange)) {
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/reports/profit".equals(path) && "POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
                handleProfitReport(exchange, body);
                return;
            }

            if ("/api/reports/profit/medicine".equals(path) && "POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange, MAX_REQUEST_BYTES);
                handleMedicineProfitReport(exchange, body);
                return;
            }

            if ("/api/reports/expired/discard".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleDiscardExpired(exchange);
                return;
            }

            if ("/api/reports/reorder".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleReorderReport(exchange);
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"Unknown report endpoint\"}");

        } catch (IllegalArgumentException e) {
            writeJson(exchange, 413, "{\"error\":\"Request body too large\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleProfitReport(HttpExchange exchange, String body) throws IOException {
        String startDateRaw = extractJsonValue(body, "startDate");
        String endDateRaw = extractJsonValue(body, "endDate");

        if (isBlank(startDateRaw) || isBlank(endDateRaw)) {
            writeJson(exchange, 400, "{\"error\":\"startDate and endDate are required\"}");
            return;
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateRaw.trim());
            endDate = LocalDate.parse(endDateRaw.trim());
        } catch (Exception e) {
            writeJson(exchange, 400, "{\"error\":\"Dates must be ISO format (yyyy-MM-dd)\"}");
            return;
        }

        ProfitReportRequest request = new ProfitReportRequest(startDate, endDate);
        ProfitReportResponse response = profitReportService.generateProfitReport(request);

        writeJson(exchange, 200, toProfitReportJson(response));
    }

    private void handleMedicineProfitReport(HttpExchange exchange, String body) throws IOException {
        String medicineIdRaw = extractJsonValue(body, "medicineId");
        if (isBlank(medicineIdRaw)) {
            writeJson(exchange, 400, "{\"error\":\"medicineId is required\"}");
            return;
        }

        int medicineId;
        try {
            medicineId = Integer.parseInt(medicineIdRaw.trim());
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"error\":\"medicineId must be a valid number\"}");
            return;
        }

        MedicineProfitReportResponse response = profitReportService.generateMedicineProfitReport(medicineId);
        writeJson(exchange, 200, toMedicineProfitReportJson(response));
    }

    private void handleDiscardExpired(HttpExchange exchange) throws IOException {
        ExpiredBatchReport report = expiredBatchDiscardService.discardExpiredBatches();
        writeJson(exchange, 200, toExpiredBatchReportJson(report));
    }

    private void handleReorderReport(HttpExchange exchange) throws IOException {
        ReorderReport report = reorderService.generateReorderReport();
        writeJson(exchange, 200, toReorderReportJson(report));
    }

    private static String toProfitReportJson(ProfitReportResponse response) {
        StringBuilder medicineBuilder = new StringBuilder();
        List<ProfitReportResponse.MedicineProfitDetail> medicineProfits = response.getMedicineProfits();
        for (int i = 0; i < medicineProfits.size(); i++) {
            ProfitReportResponse.MedicineProfitDetail item = medicineProfits.get(i);
            if (i > 0) {
                medicineBuilder.append(',');
            }
            medicineBuilder.append('{')
                    .append("\"medicineId\":").append(item.getMedicineId()).append(',')
                    .append("\"medicineName\":\"").append(escapeJson(item.getMedicineName())).append("\",")
                    .append("\"totalRevenue\":").append(toNumber(item.getTotalRevenue())).append(',')
                    .append("\"totalCost\":").append(toNumber(item.getTotalCost())).append(',')
                    .append("\"profit\":").append(toNumber(item.getProfit())).append(',')
                    .append("\"profitMargin\":").append(toNumber(item.getProfitMargin())).append(',')
                    .append("\"totalQuantity\":").append(item.getTotalQuantity())
                    .append('}');
        }

        StringBuilder vendorBuilder = new StringBuilder();
        List<ProfitReportResponse.VendorProfitDetail> vendorProfits = response.getVendorProfits();
        for (int i = 0; i < vendorProfits.size(); i++) {
            ProfitReportResponse.VendorProfitDetail item = vendorProfits.get(i);
            if (i > 0) {
                vendorBuilder.append(',');
            }
            vendorBuilder.append('{')
                    .append("\"vendorId\":").append(item.getVendorId()).append(',')
                    .append("\"vendorName\":\"").append(escapeJson(item.getVendorName())).append("\",")
                    .append("\"totalPurchaseCost\":").append(toNumber(item.getTotalPurchaseCost()))
                    .append('}');
        }

        return "{"
                + "\"startDate\":\"" + response.getStartDate() + "\","
                + "\"endDate\":\"" + response.getEndDate() + "\","
                + "\"totalSalesRevenue\":" + toNumber(response.getTotalSalesRevenue()) + ","
                + "\"totalPurchaseCost\":" + toNumber(response.getTotalPurchaseCost()) + ","
                + "\"totalProfit\":" + toNumber(response.getTotalProfit()) + ","
                + "\"profitMargin\":" + toNumber(response.getProfitMargin()) + ","
                + "\"medicineProfits\":[" + medicineBuilder + "],"
                + "\"vendorProfits\":[" + vendorBuilder + "],"
                + "\"message\":\"" + escapeJson(response.getMessage()) + "\""
                + "}";
    }

    private static String toMedicineProfitReportJson(MedicineProfitReportResponse response) {
        return "{"
                + "\"medicineId\":" + response.getMedicineId() + ","
                + "\"medicineName\":\"" + escapeJson(response.getMedicineName()) + "\","
                + "\"totalSalesRevenue\":" + toNumber(response.getTotalSalesRevenue()) + ","
                + "\"totalPurchaseCost\":" + toNumber(response.getTotalPurchaseCost()) + ","
                + "\"profit\":" + toNumber(response.getProfit()) + ","
                + "\"profitMargin\":" + toNumber(response.getProfitMargin()) + ","
                + "\"message\":\"" + escapeJson(response.getMessage()) + "\""
                + "}";
    }

    private static String toExpiredBatchReportJson(ExpiredBatchReport report) {
        StringBuilder itemsBuilder = new StringBuilder();
        List<ExpiredBatchReportItem> items = report.getItems();

        for (int i = 0; i < items.size(); i++) {
            ExpiredBatchReportItem item = items.get(i);
            if (i > 0) {
                itemsBuilder.append(',');
            }
            itemsBuilder.append('{')
                    .append("\"medicineCode\":\"").append(escapeJson(item.getMedicineCode())).append("\",")
                    .append("\"batchNumber\":\"").append(escapeJson(item.getBatchNumber())).append("\",")
                    .append("\"expiryDate\":\"").append(item.getExpiryDate()).append("\",")
                    .append("\"quantityDiscarded\":").append(item.getQuantityDiscarded()).append(',')
                    .append("\"vendorId\":").append(item.getVendorId())
                    .append('}');
        }

        return "{"
                + "\"items\":[" + itemsBuilder + "],"
                + "\"totalBatchesDiscarded\":" + report.getTotalBatchesDiscarded() + ","
                + "\"totalUnitsDiscarded\":" + report.getTotalUnitsDiscarded()
                + "}";
    }

    private static String toReorderReportJson(ReorderReport report) {
        StringBuilder itemsBuilder = new StringBuilder();
        List<ReorderItem> items = report.getReorderItems();

        for (int i = 0; i < items.size(); i++) {
            ReorderItem item = items.get(i);
            if (i > 0) {
                itemsBuilder.append(',');
            }

            StringBuilder vendors = new StringBuilder();
            List<Integer> vendorIds = item.getVendorIds();
            for (int j = 0; j < vendorIds.size(); j++) {
                if (j > 0) {
                    vendors.append(',');
                }
                vendors.append(vendorIds.get(j));
            }

            itemsBuilder.append('{')
                    .append("\"medicineCode\":\"").append(escapeJson(item.getMedicineCode())).append("\",")
                    .append("\"currentStock\":").append(item.getCurrentStock()).append(',')
                    .append("\"reorderThreshold\":").append(item.getReorderThreshold()).append(',')
                    .append("\"recommendedOrderQty\":").append(item.getRecommendedOrderQty()).append(',')
                    .append("\"vendorIds\":[").append(vendors).append(']')
                    .append('}');
        }

        return "{"
                + "\"reorderItems\":[" + itemsBuilder + "],"
                + "\"totalItems\":" + report.getTotalItems()
                + "}";
    }

    private static String toNumber(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private static String extractJsonValue(String body, String key) {
        String quotedPatternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
        Matcher quotedMatcher = Pattern.compile(quotedPatternText).matcher(body);
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1);
        }

        String numberPatternText = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*([0-9]+)";
        Matcher numberMatcher = Pattern.compile(numberPatternText).matcher(body);
        if (numberMatcher.find()) {
            return numberMatcher.group(1);
        }

        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
