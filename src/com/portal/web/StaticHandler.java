package com.portal.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

/** Serves the HTML / CSS / JavaScript files from the web/ folder. */
public final class StaticHandler implements HttpHandler {

    private static final Map<String, String> TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".js", "text/javascript; charset=utf-8",
            ".json", "application/json; charset=utf-8",
            ".svg", "image/svg+xml",
            ".png", "image/png",
            ".ico", "image/x-icon");

    private final Path root;

    public StaticHandler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                reply(ex, 405, "text/plain; charset=utf-8", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            Path file = root.resolve(path.substring(1)).normalize();
            // block "../" tricks: the file must stay inside the web folder
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                reply(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String name = file.getFileName().toString();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
            reply(ex, 200, TYPES.getOrDefault(ext, "application/octet-stream"), Files.readAllBytes(file));
        } catch (InvalidPathException e) {
            reply(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void reply(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", type);
        h.set("Cache-Control", "no-cache");
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
