package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sharded Graph Engine.
 * Routes data to 16 buckets based on hash(ID).
 */
public class GraphEngine {
    private static final int BUCKET_COUNT = 16;
    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;

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
        // Lazy load: We don't load data until requested
    }

    // --- Routing Logic ---
    private DataSegment getSegment(String id) {
        int hash = Math.abs(id.hashCode());
        return segments[hash % BUCKET_COUNT];
    }

    // --- CRUD ---

    public void persistNode(Node node) {
        getSegment(node.getId()).putNode(node);
        commit(); // In "Lite" mode we still auto-save, but now it's partial!
    }

    public boolean updateNode(String id, String key, String value) {
        Node node = getSegment(id).getNode(id);
        if (node == null) return false;
        node.addProperty(key, value);
        // Mark bucket dirty implicitly? DataSegment.putNode/getNode references are object refs.
        // We re-put to ensure dirty flag is set
        getSegment(id).putNode(node); 
        commit();
        return true;
    }

    public boolean deleteNode(String id) {
        // 1. Remove Node from its home bucket
        boolean removed = getSegment(id).removeNode(id);
        if (!removed) return false;

        // 2. Cross-Shard Cleanup: Remove relations pointing TO this node in ALL buckets
        for (DataSegment seg : segments) {
            seg.removeRelationsTo(id);
        }
        
        commit();
        return true;
    }

    public void persistRelation(String fromId, String toId, String type) {
        // Ensure both exist (requires checking their respective buckets)
        if (getSegment(fromId).getNode(fromId) == null || 
            getSegment(toId).getNode(toId) == null) {
            throw new IllegalArgumentException("Source or Target node does not exist.");
        }

        // Store relation in SOURCE bucket (adjacency list style)
        Relation r = new Relation(fromId, toId, type);
        getSegment(fromId).addRelation(r);
        commit();
    }

    // --- Querying ---

    public Node getNode(String id) {
        return getSegment(id).getNode(id);
    }

    public List<Node> traverse(String fromId, String type) {
        // 1. Get relations from source bucket
        List<Relation> links = getSegment(fromId).getRelationsFrom(fromId);
        
        // 2. Resolve targets (may span multiple buckets)
        return links.stream()
                .filter(r -> r.getType().equalsIgnoreCase(type))
                .map(r -> getSegment(r.getTargetId()).getNode(r.getTargetId())) // Cross-shard lookup
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // --- Admin ---

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
        for (DataSegment seg : segments) {
            // Delete files physically
            // Implementation simplified: just empty memory and save
            // Real impl would delete files
        }
        File dir = new File(dbDirectory);
        for(File f: dir.listFiles()) f.delete();
        initialize(); // Reset segments
    }

    // --- Storage ---
    
    private void commit() {
        // Only saves buckets that are marked 'dirty'
        for (DataSegment seg : segments) {
            seg.save();
        }
    }
}