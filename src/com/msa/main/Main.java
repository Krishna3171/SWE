package com.msa.main;

import com.msa.dto.*;
import com.msa.service.SalesService;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        SalesService salesService = new SalesService();

        // // 🟢 TEST CASE 1 — HAPPY PATH
        // SaleRequest successRequest = new SaleRequest(
        //     List.of(
        //         new SaleItemRequest("MED001", 2),
        //         new SaleItemRequest("MED002", 1)
        //     )
        // );

        // try {
        //     SaleResponse response = salesService.makeSale(successRequest);
        //     System.out.println("SUCCESS");
        //     System.out.println("Sale ID: " + response.getSaleId());
        //     System.out.println("Total Amount: " + response.getTotalAmount());
        // } catch (Exception e) {
        //     System.out.println("FAILED (should not happen)");
        //     e.printStackTrace();
        // }

        // 🔴 TEST CASE 2 — FAILURE / ROLLBACK
        SaleRequest failureRequest = new SaleRequest(
            List.of(
                new SaleItemRequest("MED001", 9999) // intentionally too large
            )
        );

        try {
            salesService.makeSale(failureRequest);
            System.out.println("FAILED (rollback did NOT happen)");
        } catch (Exception e) {
            System.out.println("ROLLBACK SUCCESSFUL");
            System.out.println(e.getMessage());
        }
    }
}
