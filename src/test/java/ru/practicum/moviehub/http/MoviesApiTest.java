package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static final Gson gson = new GsonBuilder().create();

    @BeforeAll
    void setUpAll() throws Exception {
        store = MoviesStore.getInstance();
        server = new MoviesServer(8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();


        Thread.sleep(1000);
    }

    @AfterAll
    void tearDownAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void setUp() {
        store.clear();
    }


    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", response.body().trim());
    }

    @Test
    void getMovies_withMovies_returnsList() throws Exception {

        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "Test Movie");
        movieJson.addProperty("year", 2020);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        client.send(postRequest, HttpResponse.BodyHandlers.ofString());


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Test Movie"));
        assertTrue(response.body().contains("2020"));
    }


    @Test
    void postMovie_withValidData_returnsCreatedMovie() throws Exception {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "Inception");
        movieJson.addProperty("year", 2010);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("Inception"));
        assertTrue(response.body().contains("2010"));
        assertTrue(response.body().contains("\"id\""));
    }

    @Test
    void postMovie_withEmptyTitle_returnsError() throws Exception {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "");
        movieJson.addProperty("year", 2020);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_withLongTitle_returnsError() throws Exception {
        String longTitle = "A".repeat(101);
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", longTitle);
        movieJson.addProperty("year", 2020);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("название не должно превышать 100 символов"));
    }

    @Test
    void postMovie_withInvalidYear_returnsError() throws Exception {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "Test Movie");
        movieJson.addProperty("year", 1800);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("год должен быть между 1888 и 2026"));
    }

    @Test
    void postMovie_withWrongContentType_returns415() throws Exception {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "Test");
        movieJson.addProperty("year", 2000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(415, response.statusCode());
    }


    @Test
    void getMovie_byId_returnsMovie() throws Exception {

        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "The Matrix");
        movieJson.addProperty("year", 1999);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResponse.statusCode());


        JsonObject responseObj = gson.fromJson(postResponse.body(), JsonObject.class);
        int id = responseObj.get("id").getAsInt();


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("The Matrix"));
        assertTrue(getResponse.body().contains("1999"));
    }

    @Test
    void getMovie_nonExistentId_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Фильм не найден"));
    }

    @Test
    void getMovie_invalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());


        assertEquals(404, response.statusCode());


        assertTrue(response.body().contains("Ресурс не найден") ||
                response.body().contains("Некорректный ID"));
    }


    @Test
    void deleteMovie_existingId_returns204() throws Exception {

        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "To Delete");
        movieJson.addProperty("year", 2020);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest,
                HttpResponse.BodyHandlers.ofString());
        JsonObject responseObj = gson.fromJson(postResponse.body(), JsonObject.class);
        int id = responseObj.get("id").getAsInt();


        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = client.send(deleteRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(204, deleteResponse.statusCode());


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(404, getResponse.statusCode());
    }

    @Test
    void deleteMovie_nonExistentId_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }


    @Test
    void getMovies_filterByYear_returnsFilteredList() throws Exception {

        createMovie("Movie 2020", 2020);
        createMovie("Movie 2021", 2021);
        createMovie("Another 2020", 2020);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("Movie 2020"));
        assertTrue(body.contains("Another 2020"));
        assertFalse(body.contains("Movie 2021"));
    }

    @Test
    void getMovies_filterByYear_invalidYear_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=not-a-number"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный параметр запроса - 'year'"));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("Метод не поддерживается"));
    }

    private int createMovie(String title, int year) throws Exception {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", title);
        movieJson.addProperty("year", year);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movieJson)))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
        return responseObj.get("id").getAsInt();
    }
}