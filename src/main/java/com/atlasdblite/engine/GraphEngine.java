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

    // --- UPDATED: Weighted Pathfinding (Supports Min or Max) ---

    public PathResult findWeightedPath(String startId, String endId, String weightKey, boolean findLowest) {
        if (getNode(startId) == null || getNode(endId) == null)
            return null;

        // Comparator logic:
        // findLowest = true -> Sort Ascending (Smallest first) -> Min-Heap
        // findLowest = false -> Sort Descending (Largest first) -> Max-Heap
        Comparator<PathNode> comparator = findLowest
                ? Comparator.comparingDouble(n -> n.cost)
                : (n1, n2) -> Double.compare(n2.cost, n1.cost);

        PriorityQueue<PathNode> pq = new PriorityQueue<>(comparator);
        pq.add(new PathNode(startId, 0.0));

        Map<String, Double> costMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // Initialize cost map
        // If finding lowest, we want defaults to be Infinity (so any real path is
        // smaller).
        // If finding highest, we want defaults to be -Infinity (so any real path is
        // larger).
        costMap.put(startId, 0.0);

        while (!pq.isEmpty()) {
            PathNode current = pq.poll();
            String currId = current.id;

            // Optimization: If we found a path to endId, and because it's Dijkstra/Greedy,
            // the first time we pull endId from PQ, it is the optimal path.
            if (currId.equals(endId)) {
                return new PathResult(reconstructPath(parentMap, endId), current.cost);
            }

            if (visited.contains(currId))
                continue;
            visited.add(currId);

            DataSegment seg = getSegment(currId);
            List<Relation> links = seg.getRelationsFrom(currId);

            for (Relation r : links) {
                String neighbor = r.getTargetId();
                if (visited.contains(neighbor))
                    continue;

                double weight = 1.0;
                if (weightKey != null && r.getProperties().containsKey(weightKey)) {
                    try {
                        Object val = r.getProperties().get(weightKey);
                        weight = Double.parseDouble(val.toString());
                    } catch (Exception ignored) {
                    }
                }

                double newCost = costMap.get(currId) + weight;

                // Default value for unexplored nodes depends on mode
                double currentNeighborCost = costMap.getOrDefault(neighbor,
                        findLowest ? Double.MAX_VALUE : -Double.MAX_VALUE);

                boolean improved;
                if (findLowest) {
                    improved = newCost < currentNeighborCost;
                } else {
                    improved = newCost > currentNeighborCost;
                }

                if (improved) {
                    costMap.put(neighbor, newCost);
                    parentMap.put(neighbor, currId);
                    pq.add(new PathNode(neighbor, newCost));
                }
            }
        }
        return null;
    }

    private static class PathNode {
        String id;
        double cost;

        PathNode(String id, double cost) {
            this.id = id;
            this.cost = cost;
        }
    }

    public static class PathResult {
        public List<String> path;
        public double totalCost;

        public PathResult(List<String> p, double c) {
            this.path = p;
            this.totalCost = c;
        }
    }

    // --- BFS (Unweighted) ---
    public List<String> findShortestPath(String startId, String endId, int maxDepth) {
        if (getNode(startId) == null || getNode(endId) == null)
            return Collections.emptyList();
        if (startId.equals(endId))
            return Collections.singletonList(startId);

        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        Set<String> visited = new HashSet<>();
        visited.add(startId);
        Map<String, String> parentMap = new HashMap<>();

        int currentDepth = 0;
        while (!queue.isEmpty()) {
            if (currentDepth++ > maxDepth)
                break;
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();
                if (current == null)
                    continue;
                if (current.equals(endId))
                    return reconstructPath(parentMap, endId);

                DataSegment seg = getSegment(current);
                List<Relation> outLinks = seg.getRelationsFrom(current);

                for (Relation r : outLinks) {
                    String neighbor = r.getTargetId();
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parentMap.put(neighbor, current);
                        queue.add(neighbor);
                        if (neighbor.equals(endId))
                            return reconstructPath(parentMap, endId);
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

    // --- CRUD / Query Delegates (Keep exactly as before) ---
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

    public void persistRelation(String fromId, String toId, String type, Map<String, Object> props) {
        if (getSegment(fromId).getNode(fromId) == null || getSegment(toId).getNode(toId) == null)
            throw new IllegalArgumentException("Nodes not found");
        getSegment(fromId).addRelation(new Relation(fromId, toId, type, props));
        commit();
    }

    public void persistRelation(String fromId, String toId, String type) {
        persistRelation(fromId, toId, type, new HashMap<>());
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

    public void setAutoIndexing(boolean enabled) {
        this.autoIndexing = enabled;
        for (DataSegment seg : segments)
            seg.setIndexing(enabled);
    }

    public boolean isAutoIndexing() {
        return autoIndexing;
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