package com.atlasdblite.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Relation {
    private final String sourceId;
    private final String targetId;
    private final String type;

    public Relation(String sourceId, String targetId, String type) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type.intern(); // Intern relationship types (e.g. "KNOWS" stored once)
    }

    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public String getType() { return type; }

    // --- Binary Serialization Logic ---

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(sourceId);
        out.writeUTF(targetId);
        out.writeUTF(type);
    }

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