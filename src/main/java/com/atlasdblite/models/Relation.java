package com.atlasdblite.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents a directed relationship (or edge) between two nodes in the graph.
 * A relation is defined by a source node, a target node, and a type that describes the relationship.
 * This class is immutable.
 */
public class Relation {
    private final String sourceId;
    private final String targetId;
    private final String type;

    /**
     * Constructs a new Relation.
     * @param sourceId The ID of the node where the relation originates.
     * @param targetId The ID of the node where the relation terminates.
     * @param type The type of the relationship (e.g., "KNOWS", "WORKS_FOR").
     */
    public Relation(String sourceId, String targetId, String type) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        // Interning the type string saves memory, as relationship types are often repeated.
        this.type = type.intern(); 
    }

    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public String getType() { return type; }

    // --- Binary Serialization ---

    /**
     * Writes the relation's data to a binary output stream.
     * @param out The {@link DataOutputStream} to write to.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(sourceId);
        out.writeUTF(targetId);
        out.writeUTF(type);
    }

    /**
     * Creates a Relation instance by reading data from a binary input stream.
     * @param in The {@link DataInputStream} to read from.
     * @return A new {@link Relation} instance.
     * @throws IOException If an I/O error occurs.
     */
    public static Relation readFrom(DataInputStream in) throws IOException {
        String src = in.readUTF();
        String tgt = in.readUTF();
        String type = in.readUTF();
        return new Relation(src, tgt, type);
    }

    @Override
    public String toString() {
        return String.format("(%s)-[:%s]->(%s)", sourceId, type, targetId);
    }
}