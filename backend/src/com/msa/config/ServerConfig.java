package com.msa.config;

public final class ServerConfig {

    private ServerConfig() {
    }

    public static int getPort() {
        return getIntEnv("PORT", 8080);
    }

    public static int getThreadPoolSize() {
        int cpuBasedSize = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        return getIntEnv("HTTP_THREAD_POOL_SIZE", cpuBasedSize);
    }

    private static int getIntEnv(String key, int defaultValue) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
