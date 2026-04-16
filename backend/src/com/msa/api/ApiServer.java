package com.msa.api;

import com.msa.config.ServerConfig;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ApiServer {

    private final HttpServer server;

    public ApiServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/api/health", new HealthController());
        this.server.createContext("/api/users", new UserController());
        this.server.createContext("/api/purchases", new PurchaseController());
        this.server.createContext("/api/sales", new SalesController());
        this.server.createContext("/api/reports", new ReportController());
        this.server.createContext("/api/medicines", new MedicineController());
        this.server.createContext("/api/vendors", new VendorController());
        this.server.createContext("/api/vendor-medicine", new VendorMedicineController());
        this.server.createContext("/api/inventory", new InventoryController());
        this.server.setExecutor(Executors.newFixedThreadPool(ServerConfig.getThreadPoolSize()));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }
}
