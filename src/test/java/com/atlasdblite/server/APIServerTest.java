package com.atlasdblite.server;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class APIServerTest {

    private static final int TEST_PORT = 8099;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private static final String TEST_DB_DIR = "api_test_db";
    
    private APIServer server;
    private GraphEngine engine;
    private HttpClient client;
    private Gson gson;

    @BeforeClass
    public void setup() throws IOException {
        cleanTestDir(); // Start fresh

        engine = new GraphEngine(TEST_DB_DIR);
        engine.setAutoIndexing(true); // Enable Indexing for tests
        
        server = new APIServer(engine);
        server.start(TEST_PORT);

        client = HttpClient.newHttpClient();
        gson = new Gson();
    }

    @AfterClass
    public void tearDown() {
        if (server != null) server.stop();
        cleanTestDir();
    }

    private void cleanTestDir() {
        try {
            if (Files.exists(Paths.get(TEST_DB_DIR))) {
                Files.walk(Paths.get(TEST_DB_DIR))
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception ignored) {}
    }

    @Test(priority = 1)
    public void testStatus() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/status")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        Assert.assertEquals(response.statusCode(), 200);
    }

    @Test(priority = 2)
    public void testCreateNode() throws Exception {
        String json = "{\"id\":\"u100\", \"label\":\"User\", \"props\":{\"role\":\"Admin\"}}";
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/node"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        Assert.assertEquals(response.statusCode(), 201);
        Assert.assertNotNull(engine.getNode("u100"));
    }

    @Test(priority = 3)
    public void testIndexedSearch() throws Exception {
        // Since AutoIndexing is ON, this search should hit the cache
        // Create another node to ensure specific search works
        String json = "{\"id\":\"u101\", \"label\":\"User\", \"props\":{\"role\":\"Guest\"}}";
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/node"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        // Search for 'Admin' (Should find u100 only)
        HttpResponse<String> res = client.send(
            HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/search?q=Admin")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );

        Type listType = new TypeToken<List<Node>>(){}.getType();
        List<Node> results = gson.fromJson(res.body(), listType);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getId(), "u100");
    }

    @Test(priority = 4)
    public void testCreateLink() throws Exception {
        String json = "{\"from\":\"u100\", \"to\":\"u101\", \"type\":\"MANAGES\"}";
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/link"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        Assert.assertEquals(response.statusCode(), 201);
    }
}