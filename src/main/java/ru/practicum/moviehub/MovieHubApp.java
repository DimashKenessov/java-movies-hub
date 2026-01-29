package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;

public class MovieHubApp {
    public static void main(String[] args) {
        try {
            MoviesServer server = new MoviesServer(8080);
            server.start();

            System.out.println(" MovieHub API запущен на http://localhost:8080");
            System.out.println("Доступные эндпоинты:");
            System.out.println("  GET    /movies");
            System.out.println("  POST   /movies");
            System.out.println("  GET    /movies/{id}");
            System.out.println("  DELETE /movies/{id}");
            System.out.println("  GET    /movies?year=YYYY");
            System.out.println("Для остановки нажмите Ctrl+C");


            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println(" Ошибка запуска сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}