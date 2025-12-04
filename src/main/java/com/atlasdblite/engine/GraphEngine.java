package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class GraphEngine {
    private static final int BUCKET_COUNT = 16;
    
    // MEMORY SETTING: How many shards to keep in RAM?
    // 4 shards * ~6MB each = ~24MB RAM usage minimum
    private static final int MAX_ACTIVE_SEGMENTS = 4;

    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;
    
    // LRU Cache Tracker (Thread-safe)
    private final ConcurrentLinkedDeque<Integer> lruQueue = new ConcurrentLinkedDeque<>();
    
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

    // --- LRU Routing Logic ---
    
    private DataSegment getSegment(String id) {
        int segId = Math.abs(id.hashCode()) % BUCKET_COUNT;
        touchSegment(segId);
        return segments[segId];
    }
    
    /**
     * Updates the Usage queue. If we have too many loaded segments, unload the old ones.
     */
    private void touchSegment(int segId) {
        // Remove if exists, push to front (Most Recently Used)
        lruQueue.remove(segId); 
        lruQueue.addFirst(segId);
        
        // If we exceed memory limit, unload the tail (Least Recently Used)
        while (lruQueue.size() > MAX_ACTIVE_SEGMENTS) {
            Integer lruId = lruQueue.pollLast();
            if (lruId != null) {
                segments[lruId].unload();
            }
        }
    }

    // --- CRUD ---

    public void persistNode(Node node) {
        getSegment(node.getId()).putNode(node);
        commit(); // Partial commit
    }

    public boolean updateNode(String id, String key, String value) {
        DataSegment seg = getSegment(id);
        Node node = seg.getNode(id);
        if (node == null) return false;
        
        node.addProperty(key, value);
        seg.putNode(node); // Trigger re-index
        commit();
        return true;
    }

    public boolean deleteNode(String id) {
        boolean removed = getSegment(id).removeNode(id);
        if (removed) {
            // Expensive: Must check ALL segments for relations pointing to this node
            for (int i=0; i<BUCKET_COUNT; i++) {
                touchSegment(i); // We must load them to check
                segments[i].removeRelationsTo(id);
            }
            commit();
        }
        return removed;
    }

    public void persistRelation(String fromId, String toId, String type) {
        if (getSegment(fromId).getNode(fromId) == null || 
            getSegment(toId).getNode(toId) == null) {
            throw new IllegalArgumentException("Nodes not found");
        }
        getSegment(fromId).addRelation(new Relation(fromId, toId, type));
        commit();
    }

    public Node getNode(String id) {
        return getSegment(id).getNode(id);
    }

    public List<Node> search(String query) {
        List<Node> results = new ArrayList<>();
        // Search requires touching all segments (Expensive but necessary)
        for (int i=0; i<BUCKET_COUNT; i++) {
            touchSegment(i);
            results.addAll(segments[i].search(query));
        }
        return results;
    }
    
    public List<Node> traverse(String fromId, String type) {
        DataSegment sourceSeg = getSegment(fromId);
        List<Relation> links = sourceSeg.getRelationsFrom(fromId);
        
        return links.stream()
                .filter(r -> r.getType().equalsIgnoreCase(type))
                .map(r -> getSegment(r.getTargetId()).getNode(r.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // --- Admin ---

    public void setAutoIndexing(boolean enabled) {
        this.autoIndexing = enabled;
        for (DataSegment seg : segments) seg.setIndexing(enabled);
    }
    
    public boolean isAutoIndexing() { return autoIndexing; }

    public Collection<Node> getAllNodes() {
        List<Node> all = new ArrayList<>();
        // Warning: This loads EVERYTHING into RAM
        for (int i=0; i<BUCKET_COUNT; i++) {
            touchSegment(i);
            all.addAll(segments[i].getNodes());
        }
        return all;
    }
    
    public List<Relation> getAllRelations() {
        List<Relation> all = new ArrayList<>();
        for (int i=0; i<BUCKET_COUNT; i++) {
            touchSegment(i);
            all.addAll(segments[i].getAllRelations());
        }
        return all;
    }

    public void commit() {
        // Only save loaded segments
        for (Integer id : lruQueue) {
            segments[id].save();
        }
    }
    
    public void wipeDatabase() {
        for(DataSegment s : segments) s.unload();
        File dir = new File(dbDirectory);
        if(dir.exists()) for(File f: dir.listFiles()) f.delete();
        initialize();
    }
}