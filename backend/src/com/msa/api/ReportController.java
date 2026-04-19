package com.msa.api;

import com.msa.dto.ProfitReportRequest;
import com.msa.dto.ProfitReportResponse;
import com.msa.service.ProfitReportService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReportController extends BaseController implements HttpHandler {

    private final ProfitReportService reportService;

    public ReportController() {
        this.reportService = new ProfitReportService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange, "GET");

        if (isPreflight(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        if (!requireRole(exchange, null, "admin")) {
            return;
        }

        try {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            LocalDate startDate = LocalDate.of(2000, 1, 1);
            LocalDate endDate = LocalDate.of(2050, 1, 1);

            if (params.containsKey("startDate")) {
                startDate = LocalDate.parse(params.get("startDate"));
            }
            if (params.containsKey("endDate")) {
                endDate = LocalDate.parse(params.get("endDate"));
            }

            ProfitReportRequest req = new ProfitReportRequest(startDate, endDate);
            ProfitReportResponse resp = reportService.generateProfitReport(req);

            StringBuilder json = new StringBuilder();
            json.append("{")
                    .append("\"startDate\":\"").append(resp.getStartDate()).append("\",")
                    .append("\"endDate\":\"").append(resp.getEndDate()).append("\",")
                    .append("\"totalSalesRevenue\":").append(resp.getTotalSalesRevenue()).append(",")
                    .append("\"totalPurchaseCost\":").append(resp.getTotalPurchaseCost()).append(",")
                    .append("\"totalProfit\":").append(resp.getTotalProfit()).append(",")
                    .append("\"profitMargin\":").append(resp.getProfitMargin()).append(",");

            json.append("\"medicineProfits\":[");
            if (resp.getMedicineProfits() != null) {
                for (int i = 0; i < resp.getMedicineProfits().size(); i++) {
                    var m = resp.getMedicineProfits().get(i);
                    json.append("{")
                            .append("\"medicineId\":").append(m.getMedicineId()).append(",")
                            .append("\"medicineName\":\"").append(escapeJson(m.getMedicineName())).append("\",")
                            .append("\"totalRevenue\":").append(m.getTotalRevenue()).append(",")
                            .append("\"totalCost\":").append(m.getTotalCost()).append(",")
                            .append("\"profit\":").append(m.getProfit()).append(",")
                            .append("\"profitMargin\":").append(m.getProfitMargin()).append(",")
                            .append("\"totalQuantity\":").append(m.getTotalQuantity())
                            .append("}");
                    if (i < resp.getMedicineProfits().size() - 1)
                        json.append(",");
                }
            }
            json.append("],");

            json.append("\"vendorProfits\":[");
            if (resp.getVendorProfits() != null) {
                for (int i = 0; i < resp.getVendorProfits().size(); i++) {
                    var v = resp.getVendorProfits().get(i);
                    json.append("{")
                            .append("\"vendorId\":").append(v.getVendorId()).append(",")
                            .append("\"vendorName\":\"").append(escapeJson(v.getVendorName())).append("\",")
                            .append("\"totalPurchaseCost\":").append(v.getTotalPurchaseCost())
                            .append("}");
                    if (i < resp.getVendorProfits().size() - 1)
                        json.append(",");
                }
            }
            json.append("]");

            json.append("}");

            writeJson(exchange, 200, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty())
            return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                map.put(pair[0], pair[1]);
            }
        }
        return map;
    }
}
