package com.atlasdblite.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AtlasDB-Lite Core Engine.
 * Manages in-memory graph state and handles disk persistence.
 */
public class GraphEngine {
    private Map<String, Node> nodeIndex = new HashMap<>();
    private List<Relation> relationStore = new ArrayList<>();
    
    private final String storagePath;
    private final Gson gson;

    public GraphEngine(String storagePath) {
        this.storagePath = storagePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initializeStorage();
    }

    // --- Transactional Operations ---

    public void persistNode(Node node) {
        nodeIndex.put(node.getId(), node);
        commit(); // Commit to disk
    }

    public void persistRelation(String fromId, String toId, String type) {
        if (!nodeIndex.containsKey(fromId) || !nodeIndex.containsKey(toId)) {
            throw new IllegalStateException("Integrity Error: Both nodes must exist before linking.");
        }
        relationStore.add(new Relation(fromId, toId, type));
        commit(); // Commit to disk
    }

    // --- Query Layer ---

    public List<Node> traverse(String fromId, String relationType) {
        return relationStore.stream()
                .filter(r -> r.getSourceId().equals(fromId) && r.getType().equals(relationType))
                .map(r -> nodeIndex.get(r.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // --- Storage Layer ---

    private void commit() {
        try (Writer writer = new FileWriter(storagePath)) {
            StorageSchema schema = new StorageSchema(nodeIndex, relationStore);
            gson.toJson(schema, writer);
        } catch (IOException e) {
            System.err.println("[AtlasDB] Write Error: " + e.getMessage());
        }
    }

    private void initializeStorage() {
        if (!Files.exists(Paths.get(storagePath))) {
            System.out.println("[AtlasDB] New storage file created: " + storagePath);
            return;
        }
        
        try (Reader reader = new FileReader(storagePath)) {
            StorageSchema schema = gson.fromJson(reader, StorageSchema.class);
            if (schema != null) {
                this.nodeIndex = schema.nodes != null ? schema.nodes : new HashMap<>();
                this.relationStore = schema.relations != null ? schema.relations : new ArrayList<>();
                System.out.println("[AtlasDB] Loaded " + nodeIndex.size() + " nodes from disk.");
            }
        } catch (IOException e) {
            System.err.println("[AtlasDB] Read Error: " + e.getMessage());
        }
    }

    // Internal Schema for JSON Serialization
    private static class StorageSchema {
        Map<String, Node> nodes;
        List<Relation> relations;

        StorageSchema(Map<String, Node> nodes, List<Relation> relations) {
            this.nodes = nodes;
            this.relations = relations;
        }
    }
}