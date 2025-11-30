package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import com.atlasdblite.security.CryptoManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a single partition of the database (Shard).
 * Manages its own subset of Nodes and Relations.
 */
public class DataSegment {
    private final int id;
    private final String filePath;
    private final CryptoManager crypto;
    
    // In-Memory Storage for this bucket
    private final Map<String, Node> nodes = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();
    
    private boolean isLoaded = false;
    private boolean isDirty = false; // Has unsaved changes?

    public DataSegment(int id, String rootDir, CryptoManager crypto) {
        this.id = id;
        this.filePath = rootDir + File.separator + "part_" + id + ".dat";
        this.crypto = crypto;
    }

    public void loadIfRequired() {
        if (isLoaded) return;
        
        File file = new File(filePath);
        if (!file.exists()) {
            isLoaded = true;
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String encryptedData = new String(fileBytes);
            String rawBase64 = crypto.decrypt(encryptedData);
            byte[] binaryData = Base64.getDecoder().decode(rawBase64);

            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(binaryData))) {
                // Header check
                String header = in.readUTF(); 
                if (!"SEG_V1".equals(header)) throw new IOException("Bad Segment Header");

                // Load Nodes
                int nodeCount = in.readInt();
                for (int i = 0; i < nodeCount; i++) {
                    Node n = Node.readFrom(in);
                    nodes.put(n.getId(), n);
                }

                // Load Relations
                int relCount = in.readInt();
                for (int i = 0; i < relCount; i++) {
                    relations.add(Relation.readFrom(in));
                }
            }
            isLoaded = true;
        } catch (Exception e) {
            System.err.println(" [SEG-" + id + "] Load failed: " + e.getMessage());
        }
    }

    public void save() {
        if (!isDirty && isLoaded) return; // Skip if no changes
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeUTF("SEG_V1");
            
            // Nodes
            out.writeInt(nodes.size());
            for (Node n : nodes.values()) n.writeTo(out);

            // Relations
            out.writeInt(relations.size());
            for (Relation r : relations) r.writeTo(out);

            String rawData = Base64.getEncoder().encodeToString(baos.toByteArray());
            String encryptedData = crypto.encrypt(rawData);

            Files.write(Paths.get(filePath), encryptedData.getBytes());
            isDirty = false;
        } catch (Exception e) {
            System.err.println(" [SEG-" + id + "] Save failed: " + e.getMessage());
        }
    }

    // --- CRUD ---

    public void putNode(Node node) {
        loadIfRequired();
        nodes.put(node.getId(), node);
        isDirty = true;
    }

    public Node getNode(String id) {
        loadIfRequired();
        return nodes.get(id);
    }

    public boolean removeNode(String id) {
        loadIfRequired();
        boolean removed = nodes.remove(id) != null;
        if (removed) {
            // Remove relations starting from this node in this bucket
            relations.removeIf(r -> r.getSourceId().equals(id));
            isDirty = true;
        }
        return removed;
    }

    public void addRelation(Relation r) {
        loadIfRequired();
        relations.add(r);
        isDirty = true;
    }
    
    // Remove relations pointing to a specific node (used for cascade delete)
    public void removeRelationsTo(String targetId) {
        loadIfRequired();
        boolean changed = relations.removeIf(r -> r.getTargetId().equals(targetId));
        if (changed) isDirty = true;
    }

    public List<Relation> getRelationsFrom(String sourceId) {
        loadIfRequired();
        return relations.stream()
                .filter(r -> r.getSourceId().equals(sourceId))
                .collect(Collectors.toList());
    }

    public Collection<Node> getNodes() {
        loadIfRequired();
        return nodes.values();
    }
    
    public List<Relation> getAllRelations() {
        loadIfRequired();
        return relations;
    }

    public int getNodeCount() { return isLoaded ? nodes.size() : 0; }
}