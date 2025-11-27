package com.atlasdblite.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a generic entity in the graph (e.g., Person, Server, Task).
 */
public class Node {
    private final String id;
    private final String label;
    private final Map<String, String> properties;

    public Node(String id, String label) {
        this.id = id;
        this.label = label;
        this.properties = new HashMap<>();
    }

    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public String getId() { return id; }
    public String getProperty(String key) { return properties.get(key); }
    public String getLabel() { return label; }

    @Override
    public String toString() {
        return String.format("[%s:%s] %s", label, id, properties);
    }
}
