package com.legalpro.accountservice.util;


public final class FrontendUrl {

    private static final String DEFAULT_BASE_URL = "https://bossjustice.com.au";

    private FrontendUrl() {
    }

    public static String base() {
        String configured = System.getenv("FRONTEND_BASE_URL");
        String base = (configured == null || configured.isBlank()) ? DEFAULT_BASE_URL : configured.trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /** @param path a path starting with "/", optionally with a query string */
    public static String of(String path) {
        return base() + path;
    }
}
