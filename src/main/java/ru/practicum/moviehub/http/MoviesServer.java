package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;

    public MoviesServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);


        MoviesStore store = MoviesStore.getInstance();


        MovieHandler movieHandler = new MovieHandler(store);
        server.createContext("/movies", movieHandler);

        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на порту " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}
