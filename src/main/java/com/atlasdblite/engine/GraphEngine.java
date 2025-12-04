package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * The core of the AtlasDB-Lite database.
 * This class orchestrates the sharding of data across multiple {@link DataSegment} files
 * and manages an LRU (Least Recently Used) cache to keep only a few segments in memory at a time.
 * It acts as the primary public API for all database operations.
 */
public class GraphEngine {
    /** The total number of data shards (buckets) to distribute data across. */
    private static final int BUCKET_COUNT = 16;
    /** The maximum number of data segments to keep loaded in memory at once. */
    private static final int MAX_ACTIVE_SEGMENTS = 4;

    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;
    /** A queue to manage the Least Recently Used (LRU) cache of segments. */
    private final ConcurrentLinkedDeque<Integer> lruQueue = new ConcurrentLinkedDeque<>();
    private boolean autoIndexing = false;

    public GraphEngine(String dbDirectory) {
        this.dbDirectory = dbDirectory;
        this.crypto = new CryptoManager();
        this.segments = new DataSegment[BUCKET_COUNT];
        initialize();
    }

    /** Initializes the database directory and creates all segment handlers. */
    private void initialize() {
        File dir = new File(dbDirectory);
        if (!dir.exists()) dir.mkdirs();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            segments[i] = new DataSegment(i, dbDirectory, crypto);
        }
    }

    /**
     * Determines which data segment a given ID belongs to and returns it.
     * This method also updates the LRU cache to mark the segment as recently used.
     * @param id The ID of a node or other data entity.
     * @return The {@link DataSegment} responsible for that ID.
     */
    private DataSegment getSegment(String id) {
        int segId = Math.abs(id.hashCode()) % BUCKET_COUNT;
        touchSegment(segId);
        return segments[segId];
    }

    /**
     * Updates the LRU cache. Moves the specified segment to the front of the queue
     * and unloads the least recently used segment if the cache is full.
     * @param segId The ID of the segment to mark as recently used.
     */
    private void touchSegment(int segId) {
        lruQueue.remove(segId);
        lruQueue.addFirst(segId);
        if (lruQueue.size() > MAX_ACTIVE_SEGMENTS) {
            Integer lruId = lruQueue.pollLast();
            if (lruId != null) segments[lruId].unload();
        }
    }

    /**
     * Finds the shortest path between two nodes using a Breadth-First Search (BFS).
     * @param startId The ID of the starting node.
     * @param endId The ID of the target node.
     * @param maxDepth The maximum number of hops to search.
     * @return A list of node IDs representing the shortest path, or an empty list if no path is found.
     */
    public List<String> findShortestPath(String startId, String endId, int maxDepth) {
        if (getNode(startId) == null || getNode(endId) == null) return Collections.emptyList();
        if (startId.equals(endId)) return Collections.singletonList(startId);

        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        
        Set<String> visited = new HashSet<>();
        visited.add(startId);
        Map<String, String> parentMap = new HashMap<>(); // Key=Child, Value=Parent

        int currentDepth = 0;
        while (!queue.isEmpty()) {
            if (currentDepth++ > maxDepth) break; // Stop if max depth is reached.

            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();
                if (current == null) continue;
                if (current.equals(endId)) return reconstructPath(parentMap, endId);

                // This traversal can cause multiple segments to be loaded.
                DataSegment seg = getSegment(current);
                for (Relation r : seg.getRelationsFrom(current)) {
                    String neighbor = r.getTargetId();
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parentMap.put(neighbor, current);
                        queue.add(neighbor);
                        if (neighbor.equals(endId)) return reconstructPath(parentMap, endId);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> reconstructPath(Map<String, String> parentMap, String endId) {
        LinkedList<String> path = new LinkedList<>();
        String curr = endId;
        while (curr != null) {
            path.addFirst(curr);
            curr = parentMap.get(curr);
        }
        return path;
    }

    // --- CRUD Operation Delegates ---

    public void persistNode(Node node) {
        getSegment(node.getId()).putNode(node);
        commit();
    }

    public boolean updateNode(String id, String key, String value) {
        DataSegment seg = getSegment(id);
        Node node = seg.getNode(id);
        if (node == null) return false;
        node.addProperty(key, value);
        seg.putNode(node); // `putNode` marks the segment as dirty
        commit();
        return true;
    }

    public boolean deleteNode(String id) {
        boolean removed = getSegment(id).removeNode(id);
        if (removed) {
            // This is a slow, brute-force approach. A better design might use a back-reference index.
            for (int i = 0; i < BUCKET_COUNT; i++) {
                touchSegment(i);
                segments[i].removeRelationsTo(id);
            }
            commit();
        }
        return removed;
    }

    public void persistRelation(String fromId, String toId, String type) {
        if (getNode(fromId) == null || getNode(toId) == null) {
            throw new IllegalArgumentException("Cannot create relation: one or both nodes not found.");
        }
        getSegment(fromId).addRelation(new Relation(fromId, toId, type));
        commit();
    }

    public boolean deleteRelation(String fromId, String toId, String type) {
        boolean removed = getSegment(fromId).removeRelation(fromId, toId, type);
        if (removed) commit();
        return removed;
    }

    public boolean updateRelation(String fromId, String toId, String oldType, String newType) {
        DataSegment seg = getSegment(fromId);
        if (seg.removeRelation(fromId, toId, oldType)) {
            seg.addRelation(new Relation(fromId, toId, newType));
            commit();
            return true;
        }
        return false;
    }

    // --- Read/Query Operations ---

    public Node getNode(String id) {
        return getSegment(id).getNode(id);
    }

    public List<Node> search(String query) {
        List<Node> results = new ArrayList<>();
        // This aggregates search results from all segments.
        for (int i = 0; i < BUCKET_COUNT; i++) {
            touchSegment(i);
            results.addAll(segments[i].search(query));
        }
        return results;
    }

    public List<Node> traverse(String fromId, String type) {
        return getSegment(fromId).getRelationsFrom(fromId).stream()
                .filter(r -> r.getType().equalsIgnoreCase(type))
                .map(r -> getNode(r.getTargetId())) // getNode handles segment loading
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<Node> getAllNodes() {
        List<Node> all = new ArrayList<>();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            touchSegment(i);
            all.addAll(segments[i].getNodes());
        }
        return all;
    }

    public List<Relation> getAllRelations() {
        List<Relation> all = new ArrayList<>();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            touchSegment(i);
            all.addAll(segments[i].getAllRelations());
        }
        return all;
    }

    // --- Administrative Methods ---

    public void setAutoIndexing(boolean enabled) {
        this.autoIndexing = enabled;
        for (DataSegment seg : segments) seg.setIndexing(enabled);
    }

    public boolean isAutoIndexing() { return autoIndexing; }
    public int getSegmentCount() { return BUCKET_COUNT; }
    public int getMaxActiveSegments() { return MAX_ACTIVE_SEGMENTS; }

    /** Saves all modified (dirty) segments that are currently loaded in memory. */
    public void commit() {
        for (Integer id : lruQueue) segments[id].save();
    }

    /** Deletes all data files from the database directory and re-initializes the segments. */
    public void wipeDatabase() {
        for (DataSegment s : segments) s.unload(); // Unload to release file locks
        File dir = new File(dbDirectory);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".dat") || f.getName().endsWith(".tmp")) {
                    f.delete();
                }
            }
        }
        initialize(); // Re-create empty segment handlers
    }
}