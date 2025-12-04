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

public class DataSegment {
    // ... (Existing fields and constructor remain unchanged) ...
    private final int id;
    private final String filePath;
    private final CryptoManager crypto;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, Node> nodes = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();
    private boolean indexingEnabled = false;
    private boolean isLoaded = false;
    private boolean isDirty = false;

    public DataSegment(int id, String rootDir, CryptoManager crypto) {
        this.id = id;
        this.filePath = rootDir + File.separator + "part_" + id + ".dat";
        this.crypto = crypto;
    }

    // ... (Keep existing load/save/unload logic exactly as is) ...
    // ... (Keep existing putNode/getNode/removeNode logic exactly as is) ...

    public void loadIfRequired() {
        if (isLoaded)
            return;
        rwLock.writeLock().lock();
        try {
            if (isLoaded)
                return;
            File file = new File(filePath);
            if (!file.exists()) {
                isLoaded = true;
                return;
            }
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String rawBase64 = crypto.decrypt(new String(fileBytes));
            byte[] binaryData = Base64.getDecoder().decode(rawBase64);
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(binaryData))) {
                if (!"SEG_V1".equals(in.readUTF()))
                    throw new IOException("Bad Header");
                int nc = in.readInt();
                for (int i = 0; i < nc; i++) {
                    Node n = Node.readFrom(in);
                    nodes.put(n.getId(), n);
                    if (indexingEnabled)
                        indexNode(n);
                }
                int rc = in.readInt();
                for (int i = 0; i < rc; i++)
                    relations.add(Relation.readFrom(in));
            }
            isLoaded = true;
        } catch (Exception e) {
            System.err.println("Load Failed: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void save() {
        rwLock.readLock().lock();
        try {
            if (!isDirty && isLoaded)
                return;
        } finally {
            rwLock.readLock().unlock();
        }
        rwLock.writeLock().lock();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("SEG_V1");
            out.writeInt(nodes.size());
            for (Node n : nodes.values())
                n.writeTo(out);
            out.writeInt(relations.size());
            for (Relation r : relations)
                r.writeTo(out);
            String enc = crypto.encrypt(Base64.getEncoder().encodeToString(baos.toByteArray()));
            Path targetPath = Paths.get(filePath);
            Path tempPath = Paths.get(filePath + ".tmp");
            Files.write(tempPath, enc.getBytes());
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            isDirty = false;
        } catch (Exception e) {
            System.err.println("Save Failed: " + e.getMessage());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void putNode(Node node) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            if (indexingEnabled) {
                if (nodes.containsKey(node.getId()))
                    removeFromIndex(nodes.get(node.getId()));
                indexNode(node);
            }
            nodes.put(node.getId(), node);
            isDirty = true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Node getNode(String id) {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return nodes.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean removeNode(String id) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            Node n = nodes.remove(id);
            if (n != null) {
                if (indexingEnabled)
                    removeFromIndex(n);
                relations.removeIf(r -> r.getSourceId().equals(id));
                isDirty = true;
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

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

    // --- NEW: Remove specific relation ---
    public boolean removeRelation(String sourceId, String targetId, String type) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            boolean removed = relations.removeIf(r -> r.getSourceId().equals(sourceId) &&
                    r.getTargetId().equals(targetId) &&
                    r.getType().equalsIgnoreCase(type));
            if (removed)
                isDirty = true;
            return removed;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // ... (Keep existing search/index/helper methods exactly as is) ...
    public void unload() {
        rwLock.writeLock().lock();
        try {
            if (!isLoaded)
                return;
            save();
            nodes.clear();
            relations.clear();
            invertedIndex.clear();
            isLoaded = false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void setIndexing(boolean enabled) {
        rwLock.writeLock().lock();
        try {
            this.indexingEnabled = enabled;
            if (enabled && isLoaded)
                rebuildIndex();
            else
                invertedIndex.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void rebuildIndex() {
        invertedIndex.clear();
        for (Node n : nodes.values())
            indexNode(n);
    }

    private void indexNode(Node n) {
        addToIndex(n.getId(), n.getId());
        addToIndex(n.getLabel(), n.getId());
        for (String val : n.getProperties().values())
            addToIndex(val, n.getId());
    }

    private void addToIndex(String key, String nodeId) {
        invertedIndex.computeIfAbsent(key.toLowerCase(), k -> new HashSet<>()).add(nodeId);
    }

    private void removeFromIndex(Node n) {
        removeFromIndexKey(n.getId(), n.getId());
        removeFromIndexKey(n.getLabel(), n.getId());
        for (String val : n.getProperties().values())
            removeFromIndexKey(val, n.getId());
    }

    private void removeFromIndexKey(String key, String nodeId) {
        String k = key.toLowerCase();
        if (invertedIndex.containsKey(k)) {
            Set<String> ids = invertedIndex.get(k);
            ids.remove(nodeId);
            if (ids.isEmpty())
                invertedIndex.remove(k);
        }
    }

    public List<Node> search(String query) {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            if (indexingEnabled) {
                Set<String> ids = invertedIndex.getOrDefault(query.toLowerCase(), Collections.emptySet());
                return ids.stream().map(nodes::get).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                String q = query.toLowerCase();
                return nodes.values().stream().filter(n -> n.toString().toLowerCase().contains(q))
                        .collect(Collectors.toList());
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void removeRelationsTo(String tId) {
        loadIfRequired();
        rwLock.writeLock().lock();
        try {
            if (relations.removeIf(r -> r.getTargetId().equals(tId)))
                isDirty = true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<Relation> getRelationsFrom(String sId) {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return relations.stream().filter(r -> r.getSourceId().equals(sId)).collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Collection<Node> getNodes() {
        loadIfRequired();
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(nodes.values());
        } finally {
            rwLock.readLock().unlock();
        }
    }

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