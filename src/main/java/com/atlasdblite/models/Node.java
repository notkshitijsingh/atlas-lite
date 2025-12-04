package com.atlasdblite.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Node {
    private final String id;
    private final String label;
    private final Map<String, String> properties;

    public Node(String id, String label) {
        this.id = id;
        this.label = label.intern(); 
        this.properties = new HashMap<>();
    }

    public void addProperty(String key, String value) {
        this.properties.put(key.intern(), value);
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Map<String, String> getProperties() { return properties; }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(label);
        out.writeInt(properties.size());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }

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