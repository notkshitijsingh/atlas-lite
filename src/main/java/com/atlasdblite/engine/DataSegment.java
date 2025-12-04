package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Represents a single, independent shard of the graph database.
 * Each segment is responsible for its own persistence, loading, indexing, and thread-safe access.
 * Data is lazily loaded from disk only when needed and unloaded to conserve memory.
 */
public class DataSegment {
    private final int id;
    private final String filePath;
    private final CryptoManager crypto;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    // In-memory storage for this segment's data.
    private final Map<String, Node> nodes = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();

    // State flags
    private boolean indexingEnabled = false;
    private boolean isLoaded = false;
    private boolean isDirty = false;

    /**
     * Constructs a new DataSegment.
     * @param id The unique identifier for this segment.
     * @param rootDir The root directory where the segment file will be stored.
     * @param crypto The {@link CryptoManager} instance for handling encryption and decryption.
     */
    public DataSegment(int id, String rootDir, CryptoManager crypto) {
        this.id = id;
        this.filePath = rootDir + File.separator + "part_" + id + ".dat";
        this.crypto = crypto;
    }

    /**
     * Lazily loads the segment's data from its file on disk.
     * This method is thread-safe and ensures data is only loaded once.
     * If the file doesn't exist, it initializes an empty segment.
     */
    public void loadIfRequired() {
        if (isLoaded) return;
        rwLock.writeLock().lock();
        try {
            if (isLoaded) return; // Double-check inside lock
            File file = new File(filePath);
            if (!file.exists()) {
                isLoaded = true;
                return;
            }
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String rawBase64 = crypto.decrypt(new String(fileBytes));
            byte[] binaryData = Base64.getDecoder().decode(rawBase64);
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(binaryData))) {
                if (!"SEG_V1".equals(in.readUTF())) throw new IOException("Bad Header");
                int nodeCount = in.readInt();
                for (int i = 0; i < nodeCount; i++) {
                    Node n = Node.readFrom(in);
                    nodes.put(n.getId(), n);
                    if (indexingEnabled) indexNode(n);
                }
                int relationCount = in.readInt();
                for (int i = 0; i < relationCount; i++) relations.add(Relation.readFrom(in));
            }
            isLoaded = true;
        } catch (Exception e) {
            System.err.println("FATAL: Load Failed for segment " + id + ": " + e.getMessage());
            // In a real DB, you might quarantine the file and start fresh.
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Saves the segment's data to disk if it has been modified (is "dirty").
     * The save operation is atomic, writing to a temporary file first and then renaming it.
     * This prevents data corruption if the application crashes mid-write.
     */
    public void save() {
        rwLock.readLock().lock();
        try {
            if (!isDirty || !isLoaded) return;
        } finally {
            rwLock.readLock().unlock();
        }

        rwLock.writeLock().lock();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.writeUTF("SEG_V1"); // File Header/Magic Number
                out.writeInt(nodes.size());
                for (Node n : nodes.values()) n.writeTo(out);
                out.writeInt(relations.size());
                for (Relation r : relations) r.writeTo(out);
            }
            String encryptedData = crypto.encrypt(Base64.getEncoder().encodeToString(baos.toByteArray()));
            
            // Atomic write operation
            Path targetPath = Paths.get(filePath);
            Path tempPath = Paths.get(filePath + ".tmp");
            Files.write(tempPath, encryptedData.getBytes());
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            isDirty = false;
        } catch (Exception e) {
            System.err.println("FATAL: Save Failed for segment " + id + ": " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Adds or updates a node in the segment's in-memory store and marks the segment as dirty.
     * @param node The {@link Node} to persist.
     */
    public void putNode(Node node) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            if (indexingEnabled) {
                if (nodes.containsKey(node.getId())) removeFromIndex(nodes.get(node.getId()));
                indexNode(node);
            }
            nodes.put(node.getId(), node);
            isDirty = true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a node by its ID from this segment.
     * @param id The ID of the node to find.
     * @return The {@link Node}, or {@code null} if not found in this segment.
     */
    public Node getNode(String id) {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return nodes.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Removes a node and any outgoing relationships from it within this segment.
     * @param id The ID of the node to remove.
     * @return {@code true} if the node was found and removed, {@code false} otherwise.
     */
    public boolean removeNode(String id) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            Node n = nodes.remove(id);
            if (n != null) {
                if (indexingEnabled) removeFromIndex(n);
                relations.removeIf(r -> r.getSourceId().equals(id));
                isDirty = true;
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** Adds a relationship to this segment. */
    public void addRelation(Relation r) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            relations.add(r);
isDirty = true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** Removes a specific relationship from this segment. */
    public boolean removeRelation(String sourceId, String targetId, String type) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            boolean removed = relations.removeIf(r -> r.getSourceId().equals(sourceId) &&
                    r.getTargetId().equals(targetId) &&
                    r.getType().equalsIgnoreCase(type));
            if (removed) isDirty = true;
            return removed;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** Saves any changes and clears all in-memory data for this segment. */
    public void unload() {
        rwLock.writeLock().lock();
        try {
            if (!isLoaded) return;
            save();
            nodes.clear();
            relations.clear();
            invertedIndex.clear();
            isLoaded = false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /** Enables or disables indexing for this segment. Rebuilds the index if enabled. */
    public void setIndexing(boolean enabled) {
        rwLock.writeLock().lock();
        try {
            this.indexingEnabled = enabled;
            if (enabled && isLoaded) rebuildIndex();
            else invertedIndex.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void rebuildIndex() {
        invertedIndex.clear();
        for (Node n : nodes.values()) indexNode(n);
    }

    private void indexNode(Node n) {
        addToIndex(n.getId(), n.getId());
        addToIndex(n.getLabel(), n.getId());
        for (String val : n.getProperties().values()) addToIndex(val, n.getId());
    }

    private void addToIndex(String key, String nodeId) {
        invertedIndex.computeIfAbsent(key.toLowerCase(), k -> new HashSet<>()).add(nodeId);
    }

    private void removeFromIndex(Node n) {
        removeFromIndexKey(n.getId(), n.getId());
        removeFromIndexKey(n.getLabel(), n.getId());
        for (String val : n.getProperties().values()) removeFromIndexKey(val, n.getId());
    }

    private void removeFromIndexKey(String key, String nodeId) {
        String k = key.toLowerCase();
        if (invertedIndex.containsKey(k)) {
            Set<String> ids = invertedIndex.get(k);
            ids.remove(nodeId);
            if (ids.isEmpty()) invertedIndex.remove(k);
        }
    }

    /**
     * Searches for nodes within this segment. Uses the inverted index if enabled,
     * otherwise performs a linear scan.
     * @param query The search term.
     * @return A list of matching {@link Node}s.
     */
    public List<Node> search(String query) {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            if (indexingEnabled) {
                Set<String> ids = invertedIndex.getOrDefault(query.toLowerCase(), Collections.emptySet());
                return ids.stream().map(nodes::get).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                // Fallback to slow, linear scan if indexing is off
                String q = query.toLowerCase();
                return nodes.values().stream().filter(n -> n.toString().toLowerCase().contains(q))
                        .collect(Collectors.toList());
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /** Removes any relationships pointing to the specified target node ID. */
    public void removeRelationsTo(String targetId) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            if (relations.removeIf(r -> r.getTargetId().equals(targetId))) isDirty = true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    /** Gets all relationships originating from the specified source node ID. */
    public List<Relation> getRelationsFrom(String sourceId) {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return relations.stream().filter(r -> r.getSourceId().equals(sourceId)).collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /** Returns all nodes stored in this segment. */
    public Collection<Node> getNodes() {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(nodes.values());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /** Returns all relationships stored in this segment. */
    public List<Relation> getAllRelations() {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(relations);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}