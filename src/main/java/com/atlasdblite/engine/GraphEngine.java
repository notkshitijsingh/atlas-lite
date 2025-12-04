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
    private static final int MAX_ACTIVE_SEGMENTS = 4;
    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;
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
        if (!dir.exists())
            dir.mkdirs();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            segments[i] = new DataSegment(i, dbDirectory, crypto);
        }
    }

    // --- Core Routing ---
    private DataSegment getSegment(String id) {
        int segId = Math.abs(id.hashCode()) % BUCKET_COUNT;
        touchSegment(segId);
        return segments[segId];
    }

    private void touchSegment(int segId) {
        lruQueue.remove(segId);
        lruQueue.addFirst(segId);
        while (lruQueue.size() > MAX_ACTIVE_SEGMENTS) {
            Integer lruId = lruQueue.pollLast();
            if (lruId != null)
                segments[lruId].unload();
        }
    }

    // --- NEW: Shortest Path Algorithm (BFS) ---
    public List<String> findShortestPath(String startId, String endId, int maxDepth) {
        if (getNode(startId) == null || getNode(endId) == null)
            return Collections.emptyList();
        if (startId.equals(endId))
            return Collections.singletonList(startId);

        // Queue for BFS: Stores current Node ID
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);

        // Visited Set & Parent Map (to reconstruct path)
        Set<String> visited = new HashSet<>();
        visited.add(startId);
        Map<String, String> parentMap = new HashMap<>(); // Child -> Parent

        int currentDepth = 0;

        while (!queue.isEmpty()) {
            if (currentDepth++ > maxDepth)
                break;

            // Process level by level
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();
                if (current == null)
                    continue;

                if (current.equals(endId))
                    return reconstructPath(parentMap, endId);

                // Get neighbors (Outgoing relations)
                // Note: In sharded graph, this might cause segment swapping
                DataSegment seg = getSegment(current);
                List<Relation> outLinks = seg.getRelationsFrom(current);

                for (Relation r : outLinks) {
                    String neighbor = r.getTargetId();
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parentMap.put(neighbor, current); // Record path
                        queue.add(neighbor);

                        // Optimization: Check immediately before adding to next level
                        if (neighbor.equals(endId))
                            return reconstructPath(parentMap, endId);
                    }
                }
            }
        }
        return Collections.emptyList(); // No path found
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

    // --- CRUD Delegates ---
    public void persistNode(Node node) {
        getSegment(node.getId()).putNode(node);
        commit();
    }

    public boolean updateNode(String id, String key, String value) {
        DataSegment seg = getSegment(id);
        Node node = seg.getNode(id);
        if (node == null)
            return false;
        node.addProperty(key, value);
        seg.putNode(node);
        commit();
        return true;
    }

    public boolean deleteNode(String id) {
        boolean removed = getSegment(id).removeNode(id);
        if (removed) {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                touchSegment(i);
                segments[i].removeRelationsTo(id);
            }
            commit();
        }
        return removed;
    }

    public void persistRelation(String fromId, String toId, String type) {
        if (getSegment(fromId).getNode(fromId) == null || getSegment(toId).getNode(toId) == null) {
            throw new IllegalArgumentException("Nodes not found");
        }
        getSegment(fromId).addRelation(new Relation(fromId, toId, type));
        commit();
    }

    public boolean deleteRelation(String fromId, String toId, String type) {
        boolean removed = getSegment(fromId).removeRelation(fromId, toId, type);
        if (removed)
            commit();
        return removed;
    }

    public boolean updateRelation(String fromId, String toId, String oldType, String newType) {
        DataSegment seg = getSegment(fromId);
        boolean removed = seg.removeRelation(fromId, toId, oldType);
        if (removed) {
            seg.addRelation(new Relation(fromId, toId, newType));
            commit();
            return true;
        }
        return false;
    }

    // --- Read/Query ---
    public Node getNode(String id) {
        return getSegment(id).getNode(id);
    }

    public List<Node> search(String query) {
        List<Node> results = new ArrayList<>();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            touchSegment(i);
            results.addAll(segments[i].search(query));
        }
        return results;
    }

    public List<Node> traverse(String fromId, String type) {
        DataSegment sourceSeg = getSegment(fromId);
        List<Relation> links = sourceSeg.getRelationsFrom(fromId);
        return links.stream().filter(r -> r.getType().equalsIgnoreCase(type))
                .map(r -> getSegment(r.getTargetId()).getNode(r.getTargetId()))
                .filter(Objects::nonNull).collect(Collectors.toList());
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

    // --- Admin ---
    public void setAutoIndexing(boolean enabled) {
        this.autoIndexing = enabled;
        for (DataSegment seg : segments)
            seg.setIndexing(enabled);
    }

    public boolean isAutoIndexing() {
        return autoIndexing;
    }

    public void commit() {
        for (Integer id : lruQueue)
            segments[id].save();
    }

    public void wipeDatabase() {
        for (DataSegment s : segments)
            s.unload();
        File dir = new File(dbDirectory);
        if (dir.exists())
            for (File f : dir.listFiles())
                f.delete();
        initialize();
    }
}