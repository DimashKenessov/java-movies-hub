
package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private static MoviesStore instance;

    private MoviesStore() {}

    public static MoviesStore getInstance() {
        if (instance == null) {
            instance = new MoviesStore();
        }
        return instance;
    }

    public Movie addMovie(Movie movie) {
        int id = idCounter.getAndIncrement();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public Movie getMovie(int id) {
        return movies.get(id);
    }

    public boolean deleteMovie(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }

    public void clear() {
        movies.clear();
        idCounter.set(1);
    }
}