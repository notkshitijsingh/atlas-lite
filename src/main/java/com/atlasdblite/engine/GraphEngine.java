package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;
import com.google.gson.Gson;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class GraphEngine {
    private static final int BUCKET_COUNT = 16;
    private static final int MAX_ACTIVE_SEGMENTS = 8; // Increased cache for performance
    
    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;
    private final TransactionManager wal; // NEW: Write Ahead Log
    private final Gson gson;
    private final ConcurrentLinkedDeque<Integer> lruQueue = new ConcurrentLinkedDeque<>();
    
    private boolean autoIndexing = false;

    public GraphEngine(String dbDirectory) {
        this.dbDirectory = dbDirectory;
        this.crypto = new CryptoManager();
        this.gson = new Gson();
        this.wal = new TransactionManager(crypto); // Initialize WAL
        this.segments = new DataSegment[BUCKET_COUNT];
        
        initialize();
        recover(); // Replay logs on startup
    }

    private void initialize() {
        File dir = new File(dbDirectory);
        if (!dir.exists()) dir.mkdirs();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            segments[i] = new DataSegment(i, dbDirectory, crypto);
        }
    }

    // --- ACID RECOVERY SYSTEM ---
    private void recover() {
        List<TransactionManager.WalEntry> logs = wal.readLog();
        if (logs.isEmpty()) return;

        System.out.println(" [RECOVERY] Replaying " + logs.size() + " operations from WAL...");
        
        for (TransactionManager.WalEntry entry : logs) {
            applyOpToMemory(entry.operation, entry.payload);
        }
        System.out.println(" [RECOVERY] Database state restored.");
    }

    private void applyOpToMemory(String op, String json) {
        try {
            switch (op) {
                case "ADD_NODE":
                case "UPDATE_NODE":
                    Node n = gson.fromJson(json, Node.class);
                    getSegment(n.getId()).putNode(n);
                    break;
                case "DELETE_NODE":
                    String id = json;
                    if (getSegment(id).removeNode(id)) {
                        for (DataSegment seg : segments) {
                            if (seg != null) seg.removeRelationsTo(id);
                        }
                    }
                    break;
                case "ADD_LINK":
                    Relation r = gson.fromJson(json, Relation.class);
                    getSegment(r.getSourceId()).addRelation(r);
                    break;
                case "DELETE_LINK":
                    Relation dRel = gson.fromJson(json, Relation.class);
                    getSegment(dRel.getSourceId()).removeRelation(dRel.getSourceId(), dRel.getTargetId(), dRel.getType());
                    break;
            }
        } catch (Exception e) {
            System.err.println(" [RECOVERY FAIL] " + op + ": " + e.getMessage());
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
            if (lruId != null) segments[lruId].unload();
        }
    }

    // --- WRITE OPERATIONS (Log First, Then Apply) ---

    public void persistNode(Node node) {
        wal.writeEntry(new TransactionManager.WalEntry("ADD_NODE", gson.toJson(node)));
        getSegment(node.getId()).putNode(node);
    }

    public boolean updateNode(String id, String key, String value) {
        DataSegment seg = getSegment(id);
        Node node = seg.getNode(id);
        if (node == null) return false;
        
        node.addProperty(key, value);
        
        wal.writeEntry(new TransactionManager.WalEntry("UPDATE_NODE", gson.toJson(node)));
        seg.putNode(node);
        return true;
    }

    public boolean deleteNode(String id) {
        wal.writeEntry(new TransactionManager.WalEntry("DELETE_NODE", id));
        boolean removed = getSegment(id).removeNode(id);
        if (removed) {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                touchSegment(i); // Ensure segment is loaded to clean up edges
                segments[i].removeRelationsTo(id);
            }
        }
        return removed;
    }

    public void persistRelation(String fromId, String toId, String type, Map<String, Object> props) {
        if (getSegment(fromId).getNode(fromId) == null || getSegment(toId).getNode(toId) == null) {
            throw new IllegalArgumentException("Nodes not found");
        }
        Relation r = new Relation(fromId, toId, type, props);
        wal.writeEntry(new TransactionManager.WalEntry("ADD_LINK", gson.toJson(r)));
        getSegment(fromId).addRelation(r);
    }

    // Overload for compatibility
    public void persistRelation(String fromId, String toId, String type) { 
        persistRelation(fromId, toId, type, new HashMap<>()); 
    }

    public boolean deleteRelation(String fromId, String toId, String type) {
        Relation target = new Relation(fromId, toId, type);
        wal.writeEntry(new TransactionManager.WalEntry("DELETE_LINK", gson.toJson(target)));
        return getSegment(fromId).removeRelation(fromId, toId, type);
    }

    public boolean updateRelation(String fromId, String toId, String oldType, String newType) {
        // Atomic switch logic handled by logging delete then add
        if (deleteRelation(fromId, toId, oldType)) {
            persistRelation(fromId, toId, newType);
            return true;
        }
        return false;
    }

    // --- CHECKPOINT (Replaces old commit) ---
    public void checkpoint() {
        System.out.println(" [ENGINE] Performing Checkpoint...");
        for (Integer id : lruQueue) {
            segments[id].save();
        }
        wal.clearLog(); // Truncate WAL after successful disk save
        System.out.println(" [ENGINE] Checkpoint Complete.");
    }

    // --- READ / QUERY OPERATIONS ---

    public Node getNode(String id) { 
        return getSegment(id).getNode(id); 
    }

    public Relation getRelation(String fromId, String toId) {
        DataSegment seg = getSegment(fromId);
        List<Relation> links = seg.getRelationsFrom(fromId);
        for (Relation r : links) {
            if (r.getTargetId().equals(toId)) return r;
        }
        return null;
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
        return links.stream()
                .filter(r -> r.getType().equalsIgnoreCase(type))
                .map(r -> getSegment(r.getTargetId()).getNode(r.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // --- PATHFINDING ALGORITHMS ---

    // 1. Weighted Dijkstra (Min/Max Cost)
    public PathResult findWeightedPath(String startId, String endId, String weightKey, boolean findLowest) {
        if (getNode(startId) == null || getNode(endId) == null) return null;

        Comparator<PathNode> comparator = findLowest 
            ? Comparator.comparingDouble(n -> n.cost) 
            : (n1, n2) -> Double.compare(n2.cost, n1.cost);

        PriorityQueue<PathNode> pq = new PriorityQueue<>(comparator);
        pq.add(new PathNode(startId, 0.0));

        Map<String, Double> costMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        costMap.put(startId, 0.0);

        while (!pq.isEmpty()) {
            PathNode current = pq.poll();
            String currId = current.id;

            if (currId.equals(endId)) return new PathResult(reconstructPath(parentMap, endId), current.cost);

            if (visited.contains(currId)) continue;
            visited.add(currId);

            DataSegment seg = getSegment(currId);
            for (Relation r : seg.getRelationsFrom(currId)) {
                String neighbor = r.getTargetId();
                if (visited.contains(neighbor)) continue;

                double weight = 1.0;
                if (weightKey != null && r.getProperties().containsKey(weightKey)) {
                    try { weight = Double.parseDouble(r.getProperties().get(weightKey).toString()); } 
                    catch (Exception ignored) {}
                }

                double newCost = costMap.get(currId) + weight;
                double currentNeighborCost = costMap.getOrDefault(neighbor, findLowest ? Double.MAX_VALUE : -Double.MAX_VALUE);
                boolean improved = findLowest ? (newCost < currentNeighborCost) : (newCost > currentNeighborCost);

                if (improved) {
                    costMap.put(neighbor, newCost);
                    parentMap.put(neighbor, currId);
                    pq.add(new PathNode(neighbor, newCost));
                }
            }
        }
        return null;
    }

    // 2. Unweighted BFS (Shortest Hops)
    public List<String> findShortestPath(String startId, String endId, int maxDepth) {
        if (getNode(startId) == null || getNode(endId) == null) return Collections.emptyList();
        if (startId.equals(endId)) return Collections.singletonList(startId);

        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        Set<String> visited = new HashSet<>();
        visited.add(startId);
        Map<String, String> parentMap = new HashMap<>();

        int currentDepth = 0;
        while (!queue.isEmpty()) {
            if (currentDepth++ > maxDepth) break;
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();
                if (current.equals(endId)) return reconstructPath(parentMap, endId);

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

    public static class PathResult {
        public List<String> path;
        public double totalCost;
        public PathResult(List<String> p, double c) { this.path = p; this.totalCost = c; }
    }

    private static class PathNode {
        String id; double cost;
        PathNode(String id, double cost) { this.id = id; this.cost = cost; }
    }

    // --- ADMIN / UTILS ---

    public void setAutoIndexing(boolean enabled) { 
        this.autoIndexing = enabled; 
        for (DataSegment seg : segments) seg.setIndexing(enabled); 
    }
    
    public boolean isAutoIndexing() { return autoIndexing; }

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

    public void wipeDatabase() {
        wal.clearLog();
        for (DataSegment s : segments) s.unload();
        File dir = new File(dbDirectory);
        if (dir.exists()) {
            for (File f : dir.listFiles()) f.delete();
        }
        initialize();
    }
    
    // Note: Use checkpoint() instead of commit() for manual saves
    public void commit() { checkpoint(); }
}