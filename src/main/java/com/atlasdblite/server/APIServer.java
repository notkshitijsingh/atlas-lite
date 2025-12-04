package com.atlasdblite.server;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class APIServer {
    private final GraphEngine engine;
    private HttpServer server;
    private final Gson gson;

    public APIServer(GraphEngine engine) {
        this.engine = engine;
        this.gson = new Gson();
    }

    public void start(int port) throws IOException {
        if (server != null) return;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }
            try (InputStream is = getClass().getResourceAsStream("/web/index.html")) {
                if (is == null) {
                    sendResponse(exchange, 500, "Dashboard file not found in resources.");
                    return;
                }
                byte[] htmlBytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error");
            }
        });

        server.createContext("/api/graph", exchange -> {
            GraphDTO dto = new GraphDTO(engine.getAllNodes(), engine.getAllRelations());
            sendResponse(exchange, 200, gson.toJson(dto));
        });

        server.createContext("/api/status", exchange -> 
            sendResponse(exchange, 200, "{\"status\":\"online\",\"engine\":\"AtlasDB-Lite\"}"));

        server.createContext("/api/nodes", exchange -> 
            sendResponse(exchange, 200, gson.toJson(engine.getAllNodes())));

        server.createContext("/api/node", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    NodeDTO dto = parseBody(exchange, NodeDTO.class);
                    Node node = new Node(dto.id, dto.label);
                    if (dto.props != null) dto.props.forEach(node::addProperty);
                    engine.persistNode(node);
                    sendResponse(exchange, 201, "{\"message\":\"Node Created\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        });

        server.createContext("/api/link", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    LinkDTO dto = parseBody(exchange, LinkDTO.class);
                    engine.persistRelation(dto.from, dto.to, dto.type);
                    sendResponse(exchange, 201, "{\"message\":\"Link Created\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        });
        
        server.createContext("/api/search", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("q=")) {
                String term = query.split("=")[1].toLowerCase();
                List<Node> matches = engine.search(term);
                sendResponse(exchange, 200, gson.toJson(matches));
            } else {
                sendResponse(exchange, 400, "Missing query param 'q'");
            }
        });

        server.start();
        System.out.println(" [WEB] Dashboard available at http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private <T> T parseBody(HttpExchange exchange, Class<T> clazz) {
        return gson.fromJson(new InputStreamReader(exchange.getRequestBody()), clazz);
    }

    private static class NodeDTO { String id; String label; Map<String, String> props; }
    private static class LinkDTO { String from; String to; String type; }
    
    private static class GraphDTO {
        Collection<Node> nodes;
        List<Relation> edges;
        GraphDTO(Collection<Node> n, List<Relation> e) { this.nodes = n; this.edges = e; }
    }
}