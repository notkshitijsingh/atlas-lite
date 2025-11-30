package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DataSegment {
    private final int id;
    private final String filePath;
    private final CryptoManager crypto;
    
    private final Map<String, Node> nodes = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();
    
    // Inverted Index: PropertyValue -> Set<NodeID>
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();
    private boolean indexingEnabled = false;

    private boolean isLoaded = false;
    private boolean isDirty = false;

    public DataSegment(int id, String rootDir, CryptoManager crypto) {
        this.id = id;
        this.filePath = rootDir + File.separator + "part_" + id + ".dat";
        this.crypto = crypto;
    }

    public void setIndexing(boolean enabled) {
        this.indexingEnabled = enabled;
        if (enabled) {
            rebuildIndex();
        } else {
            invertedIndex.clear();
        }
    }

    private void rebuildIndex() {
        loadIfRequired();
        invertedIndex.clear();
        for (Node n : nodes.values()) {
            indexNode(n);
        }
    }

    private void indexNode(Node n) {
        // Index ID and Label
        addToIndex(n.getId(), n.getId());
        addToIndex(n.getLabel(), n.getId());
        // Index all Properties
        for (String val : n.getProperties().values()) {
            addToIndex(val, n.getId());
        }
    }

    private void addToIndex(String key, String nodeId) {
        invertedIndex.computeIfAbsent(key.toLowerCase(), k -> new HashSet<>()).add(nodeId);
    }

    private void removeFromIndex(Node n) {
        if (!indexingEnabled) return;
        // This is expensive (O(P)), but necessary for consistency
        // Ideally, we'd store a reverse mapping, but for "Lite" we just clean up known values
        removeFromIndexKey(n.getId(), n.getId());
        removeFromIndexKey(n.getLabel(), n.getId());
        for (String val : n.getProperties().values()) {
            removeFromIndexKey(val, n.getId());
        }
    }

    private void removeFromIndexKey(String key, String nodeId) {
        String k = key.toLowerCase();
        if (invertedIndex.containsKey(k)) {
            invertedIndex.get(k).remove(nodeId);
            if (invertedIndex.get(k).isEmpty()) invertedIndex.remove(k);
        }
    }

    public List<Node> search(String query) {
        loadIfRequired();
        if (indexingEnabled) {
            // Fast Lookup O(1)
            Set<String> ids = invertedIndex.getOrDefault(query.toLowerCase(), Collections.emptySet());
            return ids.stream().map(nodes::get).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            // Slow Scan O(N)
            String q = query.toLowerCase();
            return nodes.values().stream()
                    .filter(n -> n.toString().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }
    }

    // --- CRUD Overrides for Indexing ---

    public void putNode(Node node) {
        loadIfRequired();
        if (indexingEnabled) {
            // If update, remove old index entries first
            if (nodes.containsKey(node.getId())) {
                removeFromIndex(nodes.get(node.getId()));
            }
            indexNode(node);
        }
        nodes.put(node.getId(), node);
        isDirty = true;
    }

    public boolean removeNode(String id) {
        loadIfRequired();
        Node n = nodes.remove(id);
        if (n != null) {
            if (indexingEnabled) removeFromIndex(n);
            relations.removeIf(r -> r.getSourceId().equals(id));
            isDirty = true;
            return true;
        }
        return false;
    }

    // ... (Rest of existing methods: loadIfRequired, save, getters) ...
    // Note: Re-include the load/save logic from previous DataSegment exactly as it was.
    // For brevity in this diff, I assume standard load/save logic exists here.
    
    public void loadIfRequired() {
        if (isLoaded) return;
        File file = new File(filePath);
        if (!file.exists()) { isLoaded = true; return; }
        try {
            // ... (Use existing load logic) ...
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String rawBase64 = crypto.decrypt(new String(fileBytes));
            byte[] binaryData = Base64.getDecoder().decode(rawBase64);
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(binaryData))) {
                 if (!"SEG_V1".equals(in.readUTF())) throw new IOException("Bad Header");
                 int nc = in.readInt();
                 for(int i=0; i<nc; i++) {
                     Node n = Node.readFrom(in);
                     nodes.put(n.getId(), n);
                 }
                 int rc = in.readInt();
                 for(int i=0; i<rc; i++) relations.add(Relation.readFrom(in));
            }
            isLoaded = true;
            // If indexing was enabled globally before load, we might need to sync? 
            // Usually we rely on setIndexing being called or persisted config.
        } catch (Exception e) { System.err.println("Load Error: " + e.getMessage()); }
    }

    public void save() {
        if (!isDirty && isLoaded) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("SEG_V1");
            out.writeInt(nodes.size());
            for(Node n : nodes.values()) n.writeTo(out);
            out.writeInt(relations.size());
            for(Relation r : relations) r.writeTo(out);
            String enc = crypto.encrypt(Base64.getEncoder().encodeToString(baos.toByteArray()));
            Files.write(Paths.get(filePath), enc.getBytes());
            isDirty = false;
        } catch(Exception e) { System.err.println("Save Error: " + e.getMessage()); }
    }

    public Node getNode(String id) { loadIfRequired(); return nodes.get(id); }
    public void addRelation(Relation r) { loadIfRequired(); relations.add(r); isDirty = true; }
    public void removeRelationsTo(String tId) { 
        loadIfRequired(); 
        if(relations.removeIf(r -> r.getTargetId().equals(tId))) isDirty = true; 
    }
    public List<Relation> getRelationsFrom(String sId) { 
        loadIfRequired(); 
        return relations.stream().filter(r -> r.getSourceId().equals(sId)).collect(Collectors.toList()); 
    }
    public Collection<Node> getNodes() { loadIfRequired(); return nodes.values(); }
    public List<Relation> getAllRelations() { loadIfRequired(); return relations; }
}