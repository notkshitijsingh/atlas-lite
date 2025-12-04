package com.atlasdblite.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node (or vertex) in the graph.
 * A node has a unique ID, a label to classify it, and a map of key-value properties.
 * This class includes logic for efficient binary serialization.
 */
public class Node {
    private final String id;
    private final String label;
    private final Map<String, String> properties;

    /**
     * Constructs a new Node.
     * @param id The unique identifier for the node.
     * @param label The type or classification of the node (e.g., "Person", "Company").
     */
    public Node(String id, String label) {
        this.id = id;
        // The 'intern()' method is used to save memory by ensuring that identical strings
        // (like common labels) are stored only once in the JVM's string pool.
        this.label = label.intern(); 
        this.properties = new HashMap<>();
    }

    /**
     * Adds or updates a property on the node.
     * @param key The property key.
     * @param value The property value.
     */
    public void addProperty(String key, String value) {
        // Keys are also interned for memory efficiency, as they are often repeated.
        this.properties.put(key.intern(), value);
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Map<String, String> getProperties() { return properties; }

    // --- Binary Serialization ---

    /**
     * Writes the node's data to a binary output stream for persistence.
     * @param out The {@link DataOutputStream} to write to.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(label);
        out.writeInt(properties.size());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }

    /**
     * Creates a Node instance by reading data from a binary input stream.
     * @param in The {@link DataInputStream} to read from.
     * @return A new {@link Node} instance.
     * @throws IOException If an I/O error occurs or the stream is malformed.
     */
    public static Node readFrom(DataInputStream in) throws IOException {
        String id = in.readUTF();
        String label = in.readUTF();
        Node node = new Node(id, label);
        
        int propCount = in.readInt();
        for (int i = 0; i < propCount; i++) {
            String key = in.readUTF();
            String value = in.readUTF();
            node.addProperty(key, value);
        }
        return node;
    }

    @Override
    public String toString() {
        return String.format("(%s:%s) %s", id, label, properties);
    }
}