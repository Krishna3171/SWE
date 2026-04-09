package com.msa.main;

import com.msa.api.ApiServer;
import com.msa.config.ServerConfig;

public class Main {

    public static void main(String[] args) {
        startApiServer();
    }

    private static void startApiServer() {
        int port = ServerConfig.getPort();

        try {
            ApiServer apiServer = new ApiServer(port);
            apiServer.start();

            System.out.println("MSA API server started on port " + port);
            System.out.println("Health endpoint: http://localhost:" + port + "/api/health");
            System.out.println("Login endpoint: http://localhost:" + port + "/api/users/login");

            Runtime.getRuntime().addShutdownHook(new Thread(apiServer::stop));
        } catch (Exception e) {
            System.out.println("Failed to start API server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
