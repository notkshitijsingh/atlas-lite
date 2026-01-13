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
import java.util.HashMap;
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
        if (server != null)
            return;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // 1. Dashboard UI
        server.createContext("/", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }
            try (InputStream is = getClass().getResourceAsStream("/web/index.html")) {
                if (is == null) {
                    sendResponse(exchange, 500, "Dashboard not found");
                    return;
                }
                byte[] htmlBytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlBytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error");
            }
        });

        // 2. Full Graph Data
        server.createContext("/api/graph", exchange -> {
            Map<String, Double> scores = engine.getPageRankScores();

            if (scores.isEmpty()) {
                scores = engine.calculatePageRank(20, 0.85);
            }

            GraphDTO dto = new GraphDTO(
                    engine.getAllNodes(),
                    engine.getAllRelations(),
                    scores);
            sendResponse(exchange, 200, gson.toJson(dto));
        });

        // 3. Node Operations (Create, Update, Delete)
        server.createContext("/api/node", exchange -> {
            String method = exchange.getRequestMethod();
            try {
                if ("POST".equalsIgnoreCase(method)) {
                    NodeDTO dto = parseBody(exchange, NodeDTO.class);
                    Node node = new Node(dto.id, dto.label);
                    if (dto.props != null)
                        dto.props.forEach(node::addProperty);
                    engine.persistNode(node);
                    sendResponse(exchange, 201, "{\"message\":\"Node Created\"}");
                } else if ("PUT".equalsIgnoreCase(method)) {
                    NodeDTO dto = parseBody(exchange, NodeDTO.class);
                    if (engine.getNode(dto.id) == null) {
                        sendResponse(exchange, 404, "{\"error\":\"Node not found\"}");
                        return;
                    }
                    if (dto.props != null) {
                        dto.props.forEach((k, v) -> engine.updateNode(dto.id, k, v.toString()));
                    }
                    sendResponse(exchange, 200, "{\"message\":\"Node Updated\"}");
                } else if ("DELETE".equalsIgnoreCase(method)) {
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
                    if (params.containsKey("id")) {
                        String id = params.get("id");
                        if (engine.deleteNode(id)) {
                            sendResponse(exchange, 200, "{\"message\":\"Node Deleted\"}");
                        } else {
                            sendResponse(exchange, 404, "{\"error\":\"Node not found\"}");
                        }
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Missing id parameter\"}");
                    }
                } else {
                    sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // 4. Link Operations (Create, Update, Delete)
        server.createContext("/api/link", exchange -> {
            String method = exchange.getRequestMethod();
            try {
                if ("POST".equalsIgnoreCase(method)) {
                    LinkDTO dto = parseBody(exchange, LinkDTO.class);
                    Map<String, Object> props = dto.props != null ? dto.props : new HashMap<>();
                    engine.persistRelation(dto.from, dto.to, dto.type, props);
                    sendResponse(exchange, 201, "{\"message\":\"Link Created\"}");
                } else if ("PUT".equalsIgnoreCase(method)) { // Update Link Type (Old -> New)
                    // Expects body: { "from": "A", "to": "B", "type": "OLD_TYPE", "newType":
                    // "NEW_TYPE" }
                    LinkDTO dto = parseBody(exchange, LinkDTO.class);
                    if (dto.newType != null) {
                        if (engine.updateRelation(dto.from, dto.to, dto.type, dto.newType)) {
                            sendResponse(exchange, 200, "{\"message\":\"Link Updated\"}");
                        } else {
                            sendResponse(exchange, 404, "{\"error\":\"Link not found\"}");
                        }
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Missing newType\"}");
                    }
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    // Expects query: ?from=A&to=B&type=KNOWS
                    Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
                    if (params.containsKey("from") && params.containsKey("to") && params.containsKey("type")) {
                        if (engine.deleteRelation(params.get("from"), params.get("to"), params.get("type"))) {
                            sendResponse(exchange, 200, "{\"message\":\"Link Deleted\"}");
                        } else {
                            sendResponse(exchange, 404, "{\"error\":\"Link not found\"}");
                        }
                    } else {
                        sendResponse(exchange, 400, "{\"error\":\"Missing parameters\"}");
                    }
                } else {
                    sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // Standard Endpoints
        server.createContext("/api/status", exchange -> sendResponse(exchange, 200, "{\"status\":\"online\"}"));
        server.createContext("/api/nodes", exchange -> sendResponse(exchange, 200, gson.toJson(engine.getAllNodes())));
        server.createContext("/api/search", exchange -> {
            String q = exchange.getRequestURI().getQuery();
            if (q != null && q.startsWith("q=")) {
                List<Node> matches = engine.search(q.split("=")[1].toLowerCase());
                sendResponse(exchange, 200, gson.toJson(matches));
            } else {
                sendResponse(exchange, 400, "Missing q");
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
        if (!exchange.getResponseHeaders().containsKey("Content-Type"))
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private <T> T parseBody(HttpExchange exchange, Class<T> clazz) {
        return gson.fromJson(new InputStreamReader(exchange.getRequestBody()), clazz);
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null)
            return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1)
                result.put(entry[0], entry[1]);
        }
        return result;
    }

    private static class NodeDTO {
        String id;
        String label;
        Map<String, Object> props;
    }

    // Updated LinkDTO
    private static class LinkDTO {
        String from;
        String to;
        String type;
        String newType; // For updates
        Map<String, Object> props;
    }

    private static class GraphDTO {
        Collection<Node> nodes;
        List<Relation> edges;
        Map<String, Double> analytics;

        GraphDTO(Collection<Node> nodes, List<Relation> edges, Map<String, Double> analytics) {
            this.nodes = nodes;
            this.edges = edges;
            this.analytics = analytics;
        }
    }
}