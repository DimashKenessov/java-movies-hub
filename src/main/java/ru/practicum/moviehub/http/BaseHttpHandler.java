
package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = String.format("{\"error\":\"%s\"}", escapeJson(message));
        sendJson(exchange, statusCode, json);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message, List<String> details) throws IOException {
        StringBuilder detailsJson = new StringBuilder("[");
        for (int i = 0; i < details.size(); i++) {
            detailsJson.append("\"").append(escapeJson(details.get(i))).append("\"");
            if (i < details.size() - 1) {
                detailsJson.append(",");
            }
        }
        detailsJson.append("]");

        String json = String.format("{\"error\":\"%s\",\"details\":%s}",
                escapeJson(message), detailsJson);
        sendJson(exchange, statusCode, json);
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    protected boolean isJsonContentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    protected String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    protected String movieToJson(Movie movie) {
        return String.format("{\"id\":%d,\"title\":\"%s\",\"year\":%d}",
                movie.getId(), escapeJson(movie.getTitle()), movie.getYear());
    }

    protected String moviesListToJson(List<Movie> movies) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < movies.size(); i++) {
            json.append(movieToJson(movies.get(i)));
            if (i < movies.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }
}