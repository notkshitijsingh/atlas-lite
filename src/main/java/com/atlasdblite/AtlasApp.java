package com.atlasdblite;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.List;

public class AtlasApp {
    
    private static final String DB_FILE = "atlas_data.json";

    public static void main(String[] args) {
        printBanner();
        
        // 1. Initialize Engine
        GraphEngine db = new GraphEngine(DB_FILE);

        // 2. Seed Data (Simulating a real workflow)
        System.out.println("\n[INFO] Seeding Graph Data...");
        
        Node admin = new Node("u-001", "User");
        admin.addProperty("username", "j_doe");
        admin.addProperty("role", "Admin");

        Node server = new Node("s-999", "Server");
        server.addProperty("hostname", "prod-db-01");
        server.addProperty("ip", "192.168.1.50");

        Node incident = new Node("inc-204", "Incident");
        incident.addProperty("severity", "High");
        incident.addProperty("status", "Open");

        db.persistNode(admin);
        db.persistNode(server);
        db.persistNode(incident);

        // 3. Create Links
        try {
            db.persistRelation("u-001", "s-999", "MANAGES");
            db.persistRelation("s-999", "inc-204", "HAS_ALERT");
            System.out.println("[INFO] Relationships established.");
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }

        // 

        // 4. Run Queries
        performQueries(db);
    }

    private static void performQueries(GraphEngine db) {
        System.out.println("\n--- Query Execution ---");
        
        String userId = "u-001";
        System.out.println("Query: What servers does User[" + userId + "] manage?");
        List<Node> servers = db.traverse(userId, "MANAGES");
        
        for (Node s : servers) {
            System.out.println("  -> Found: " + s);
            // Nested Query: Check alerts on this server
            List<Node> alerts = db.traverse(s.getId(), "HAS_ALERT");
            for (Node a : alerts) {
                System.out.println("     -> [WARNING] Active Alert: " + a);
            }
        }
    }

    private static void printBanner() {
        System.out.println("    _   _   _            ____  ____  ");
        System.out.println("   / \\ | |_| | __ _ ___ |  _ \\| __ ) ");
        System.out.println("  / _ \\| __| |/ _` / __|| | | |  _ \\ ");
        System.out.println(" / ___ \\ |_| | (_| \\__ \\| |_| | |_) |");
        System.out.println("/_/   \\_\\__|_|\\__,_|___/|____/|____/ ");
        System.out.println("           L I T E   E D I T I O N   ");
        System.out.println("-------------------------------------");
        System.out.println("Status: Online | Storage: Local JSON ");
        System.out.println("-------------------------------------");
    }
}