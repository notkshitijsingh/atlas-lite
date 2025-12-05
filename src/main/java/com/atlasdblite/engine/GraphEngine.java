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
    private static final int MAX_ACTIVE_SEGMENTS = 8;

    private final DataSegment[] segments;
    private final String dbDirectory;
    private final CryptoManager crypto;
    private final TransactionManager wal;
    private final Gson gson;
    private final ConcurrentLinkedDeque<Integer> lruQueue = new ConcurrentLinkedDeque<>();

    private boolean autoIndexing = false;

    // Cache for Analytics
    private Map<String, Double> pageRankScores = new HashMap<>();

    public GraphEngine(String dbDirectory) {
        this.dbDirectory = dbDirectory;
        this.crypto = new CryptoManager();
        this.gson = new Gson();
        this.wal = new TransactionManager(crypto);
        this.segments = new DataSegment[BUCKET_COUNT];

        initialize();
        recover();
    }

    private void initialize() {
        File dir = new File(dbDirectory);
        if (!dir.exists())
            dir.mkdirs();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            segments[i] = new DataSegment(i, dbDirectory, crypto);
        }
    }

    // --- NEW: PageRank Algorithm ---

    public Map<String, Double> calculatePageRank(int iterations, double dampingFactor) {
        System.out.println(" [ANALYTICS] Loading topology for PageRank...");

        // 1. Build lightweight adjacency map (ID -> List<TargetID>)
        // We load all data to build this map.
        Collection<Node> allNodes = getAllNodes();
        List<Relation> allEdges = getAllRelations();

        Map<String, List<String>> incomingLinks = new HashMap<>(); // Who points to me?
        Map<String, Integer> outDegree = new HashMap<>(); // How many do I point to?

        for (Node n : allNodes) {
            incomingLinks.put(n.getId(), new ArrayList<>());
            outDegree.put(n.getId(), 0);
        }

        for (Relation r : allEdges) {
            // Only count if both nodes exist (Data integrity check)
            if (incomingLinks.containsKey(r.getTargetId()) && incomingLinks.containsKey(r.getSourceId())) {
                incomingLinks.get(r.getTargetId()).add(r.getSourceId());
                outDegree.put(r.getSourceId(), outDegree.get(r.getSourceId()) + 1);
            }
        }

        // 2. Initialize Ranks
        Map<String, Double> ranks = new HashMap<>();
        double initialRank = 1.0 / allNodes.size();
        for (Node n : allNodes)
            ranks.put(n.getId(), initialRank);

        // 3. Iterate
        System.out.println(" [ANALYTICS] Running " + iterations + " iterations...");
        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();

            for (String nodeId : ranks.keySet()) {
                double rankSum = 0.0;
                // Sum(PR(neighbor) / OutDegree(neighbor))
                for (String neighbor : incomingLinks.get(nodeId)) {
                    if (outDegree.get(neighbor) > 0) {
                        rankSum += ranks.get(neighbor) / outDegree.get(neighbor);
                    }
                }

                // PageRank Formula
                double pr = (1 - dampingFactor) + (dampingFactor * rankSum);
                newRanks.put(nodeId, pr);
            }
            ranks = newRanks;
        }

        System.out.print("atlas> ");

        // 4. Normalize scores (0.0 to 10.0 for easier reading)
        double maxScore = Collections.max(ranks.values());
        for (Map.Entry<String, Double> entry : ranks.entrySet()) {
            entry.setValue((entry.getValue() / maxScore) * 10.0);
        }

        this.pageRankScores = ranks; // Cache it
        return ranks;
    }

    public Map<String, Double> getPageRankScores() {
        return pageRankScores;
    }

    // ... (Keep ALL existing methods: CRUD, WAL, Pathfinding, etc.) ...

    // RECOVERY
    private void recover() {
        List<TransactionManager.WalEntry> logs = wal.readLog();
        if (logs.isEmpty())
            return;
        System.out.println(" [RECOVERY] Replaying " + logs.size() + " ops...");
        for (TransactionManager.WalEntry entry : logs)
            applyOpToMemory(entry.operation, entry.payload);
        System.out.println(" [RECOVERY] Done.");
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
                    if (getSegment(id).removeNode(id))
                        for (DataSegment s : segments)
                            if (s != null)
                                s.removeRelationsTo(id);
                    break;
                case "ADD_LINK":
                    Relation r = gson.fromJson(json, Relation.class);
                    getSegment(r.getSourceId()).addRelation(r);
                    break;
                case "DELETE_LINK":
                    Relation d = gson.fromJson(json, Relation.class);
                    getSegment(d.getSourceId()).removeRelation(d.getSourceId(), d.getTargetId(), d.getType());
                    break;
            }
        } catch (Exception e) {
        }
    }

    // Routing
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

    // CRUD Delegates
    public void persistNode(Node n) {
        wal.writeEntry(new TransactionManager.WalEntry("ADD_NODE", gson.toJson(n)));
        getSegment(n.getId()).putNode(n);
    }

    public boolean updateNode(String id, String k, String v) {
        Node n = getSegment(id).getNode(id);
        if (n == null)
            return false;
        n.addProperty(k, v);
        wal.writeEntry(new TransactionManager.WalEntry("UPDATE_NODE", gson.toJson(n)));
        getSegment(id).putNode(n);
        return true;
    }

    public boolean deleteNode(String id) {
        wal.writeEntry(new TransactionManager.WalEntry("DELETE_NODE", id));
        boolean rem = getSegment(id).removeNode(id);
        if (rem)
            for (int i = 0; i < BUCKET_COUNT; i++) {
                touchSegment(i);
                segments[i].removeRelationsTo(id);
            }
        return rem;
    }

    public void persistRelation(String f, String t, String type, Map<String, Object> p) {
        if (getSegment(f).getNode(f) == null || getSegment(t).getNode(t) == null)
            throw new IllegalArgumentException("Nodes not found");
        Relation r = new Relation(f, t, type, p);
        wal.writeEntry(new TransactionManager.WalEntry("ADD_LINK", gson.toJson(r)));
        getSegment(f).addRelation(r);
    }

    public void persistRelation(String f, String t, String type) {
        persistRelation(f, t, type, new HashMap<>());
    }

    public boolean deleteRelation(String f, String t, String type) {
        Relation tg = new Relation(f, t, type);
        wal.writeEntry(new TransactionManager.WalEntry("DELETE_LINK", gson.toJson(tg)));
        return getSegment(f).removeRelation(f, t, type);
    }

    public boolean updateRelation(String f, String t, String old, String newT) {
        if (deleteRelation(f, t, old)) {
            persistRelation(f, t, newT);
            return true;
        }
        return false;
    }

    public void checkpoint() {
        System.out.println(" [ENGINE] Checkpointing...");
        for (Integer id : lruQueue)
            segments[id].save();
        wal.clearLog();
        System.out.println(" [ENGINE] Done.");
    }

    // Read
    public Node getNode(String id) {
        return getSegment(id).getNode(id);
    }

    public Relation getRelation(String f, String t) {
        for (Relation r : getSegment(f).getRelationsFrom(f))
            if (r.getTargetId().equals(t))
                return r;
        return null;
    }

    public List<Node> search(String q) {
        List<Node> r = new ArrayList<>();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            touchSegment(i);
            r.addAll(segments[i].search(q));
        }
        return r;
    }

    public List<Node> traverse(String f, String t) {
        return getSegment(f).getRelationsFrom(f).stream().filter(r -> r.getType().equalsIgnoreCase(t))
                .map(r -> getSegment(r.getTargetId()).getNode(r.getTargetId())).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // Pathfinding
    public PathResult findWeightedPath(String s, String e, String k, boolean min) {
        // (Paste previous Dijkstra logic here)
        // For brevity in this response, assume previous logic exists
        return null;
    }

    public List<String> findShortestPath(String s, String e, int d) {
        // (Paste previous BFS logic here)
        return Collections.emptyList();
    }

    public static class PathResult {
        public List<String> path;
        public double totalCost;

        public PathResult(List<String> p, double c) {
            this.path = p;
            this.totalCost = c;
        }
    }

    // Admin
    public void setAutoIndexing(boolean e) {
        this.autoIndexing = e;
        for (DataSegment s : segments)
            s.setIndexing(e);
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

    public void wipeDatabase() {
        wal.clearLog();
        for (DataSegment s : segments)
            s.unload();
        File d = new File(dbDirectory);
        if (d.exists())
            for (File f : d.listFiles())
                f.delete();
        initialize();
    }

    public void commit() {
        checkpoint();
    }
}