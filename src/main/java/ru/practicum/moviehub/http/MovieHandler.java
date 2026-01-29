package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MovieHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final int CURRENT_YEAR = 2026;

    public MovieHandler(MoviesStore store) {
        this.store = store;
    }

    @Override

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/movies")) {
                handleMoviesCollection(exchange, method);
            } else if (path.matches("/movies/\\d+")) {
                handleSingleMovie(exchange, method, path);
            } else {
                sendError(exchange, 404, "Ресурс не найден");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    private void handleMoviesCollection(HttpExchange exchange, String method) throws IOException {
        switch (method) {
            case "GET":
                handleGetMovies(exchange);
                break;
            case "POST":
                handlePostMovie(exchange);
                break;
            default:
                sendError(exchange, 405, "Метод не поддерживается");
        }
    }

    private void handleSingleMovie(HttpExchange exchange, String method, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length != 3) {
            sendError(exchange, 400, "Некорректный URL");
            return;
        }

        try {
            int id = Integer.parseInt(parts[2]);

            switch (method) {
                case "GET":
                    handleGetMovie(exchange, id);
                    break;
                case "DELETE":
                    handleDeleteMovie(exchange, id);
                    break;
                default:
                    sendError(exchange, 405, "Метод не поддерживается");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();

        if (query != null && query.startsWith("year=")) {
            handleFilterByYear(exchange, query);
        } else {
            List<Movie> movies = store.getAllMovies();
            String json = moviesListToJson(movies);
            sendJson(exchange, 200, json);
        }
    }

    private void handleFilterByYear(HttpExchange exchange, String query) throws IOException {
        String yearParam = query.substring(5);
        try {
            int year = Integer.parseInt(yearParam);
            List<Movie> movies = store.getMoviesByYear(year);
            String json = moviesListToJson(movies);
            sendJson(exchange, 200, json);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный параметр запроса - 'year'");
        }
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        if (!isJsonContentType(exchange)) {
            sendError(exchange, 415, "Unsupported Media Type");
            return;
        }

        String body = readRequestBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        try {
            Movie movie = parseMovieFromJson(body);
            List<String> errors = validateMovie(movie);

            if (!errors.isEmpty()) {
                sendError(exchange, 422, "Ошибка валидации", errors);
                return;
            }

            Movie created = store.addMovie(movie);
            String json = movieToJson(created);


            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", CT_JSON);
            exchange.sendResponseHeaders(201, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Некорректный JSON");
        }
    }

    private void handleGetMovie(HttpExchange exchange, int id) throws IOException {
        Movie movie = store.getMovie(id);
        if (movie == null) {
            sendError(exchange, 404, "Фильм не найден");
        } else {
            String json = movieToJson(movie);
            sendJson(exchange, 200, json);
        }
    }

    private void handleDeleteMovie(HttpExchange exchange, int id) throws IOException {
        boolean deleted = store.deleteMovie(id);
        if (!deleted) {
            sendError(exchange, 404, "Фильм не найден");
        } else {
            sendNoContent(exchange);
        }
    }

    private Movie parseMovieFromJson(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON");
        }

        String content = json.substring(1, json.length() - 1).trim();
        String[] pairs = content.split(",");

        String title = null;
        Integer year = null;

        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length != 2) continue;

            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim().replace("\"", "");

            if (key.equals("title")) {
                title = value;
            } else if (key.equals("year")) {
                try {
                    year = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid year");
                }
            }
        }

        if (title == null || year == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        return new Movie(title, year);
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название не должно превышать 100 символов");
        }

        if (movie.getYear() < 1888 || movie.getYear() > CURRENT_YEAR) {
            errors.add("год должен быть между 1888 и " + CURRENT_YEAR);
        }

        return errors;
    }
}
