package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GraphEngine {
    private static final int BUCKET_COUNT = 16;
    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;
    private boolean autoIndexing = false;

    public GraphEngine(String dbDirectory) {
        this.dbDirectory = dbDirectory;
        this.crypto = new CryptoManager();
        this.segments = new DataSegment[BUCKET_COUNT];
        initialize();
    }

    private void initialize() {
        File dir = new File(dbDirectory);
        if (!dir.exists()) dir.mkdirs();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            segments[i] = new DataSegment(i, dbDirectory, crypto);
        }
    }

    public void setAutoIndexing(boolean enabled) {
        this.autoIndexing = enabled;
        System.out.println(" [ENGINE] Indexing set to: " + enabled + " (Rebuilding...)");
        for (DataSegment seg : segments) {
            seg.setIndexing(enabled);
        }
    }

    public boolean isAutoIndexing() { return autoIndexing; }

    public List<Node> search(String query) {
        List<Node> results = new ArrayList<>();
        // In sharded mode, we must ask every shard to search itself
        for (DataSegment seg : segments) {
            results.addAll(seg.search(query));
        }
        return results;
    }

    // --- Routing & CRUD (Delegates) ---
    private DataSegment getSegment(String id) {
        return segments[Math.abs(id.hashCode()) % BUCKET_COUNT];
    }

    public void persistNode(Node node) {
        getSegment(node.getId()).putNode(node);
        commit();
    }

    public boolean deleteNode(String id) {
        boolean removed = getSegment(id).removeNode(id);
        if (removed) {
            for (DataSegment seg : segments) seg.removeRelationsTo(id);
            commit();
        }
        return removed;
    }

    public boolean updateNode(String id, String key, String value) {
        Node node = getSegment(id).getNode(id);
        if (node == null) return false;
        node.addProperty(key, value);
        getSegment(id).putNode(node); // Trigger index update
        commit();
        return true;
    }

    public void persistRelation(String fromId, String toId, String type) {
        if (getSegment(fromId).getNode(fromId) == null || 
            getSegment(toId).getNode(toId) == null) {
            throw new IllegalArgumentException("Nodes not found");
        }
        getSegment(fromId).addRelation(new Relation(fromId, toId, type));
        commit();
    }

    public Node getNode(String id) { return getSegment(id).getNode(id); }

    public List<Node> traverse(String fromId, String type) {
        List<Relation> links = getSegment(fromId).getRelationsFrom(fromId);
        return links.stream()
                .filter(r -> r.getType().equalsIgnoreCase(type))
                .map(r -> getSegment(r.getTargetId()).getNode(r.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<Node> getAllNodes() {
        List<Node> all = new ArrayList<>();
        for (DataSegment seg : segments) all.addAll(seg.getNodes());
        return all;
    }

    public List<Relation> getAllRelations() {
        List<Relation> all = new ArrayList<>();
        for (DataSegment seg : segments) all.addAll(seg.getAllRelations());
        return all;
    }

    public void wipeDatabase() {
        // Simple wipe logic for testing
        for(DataSegment s : segments) s.setIndexing(false);
        File dir = new File(dbDirectory);
        if(dir.exists()) for(File f: dir.listFiles()) f.delete();
        initialize();
    }
    
    public void commit() {
        for (DataSegment seg : segments) seg.save();
    }
}