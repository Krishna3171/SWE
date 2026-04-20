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
            writeJsonObject(exchange, 200, resp);

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
