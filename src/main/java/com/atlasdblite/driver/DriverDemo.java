package com.atlasdblite.driver;

public class DriverDemo {

    public static void main(String[] args) {
        // Ensure your AtlasDB is running in server mode first!
        // e.g., 'java -jar atlasdb.jar' then type 'server 4000' inside the shell.
        
        String host = "localhost";
        int port = 4000; // Match the port your server is listening on

        System.out.println("Starting Driver Demo...");

        try (AtlasDriver db = new AtlasDriver(host, port)) {
            
            System.out.println("Connecting...");
            db.connect();
            System.out.println("Connected!");

            // 1. Clean slate (Optional, be careful!)
            db.execute("nuke"); 

            // 2. Add some Data
            System.out.println("\n--- Adding Nodes ---");
            System.out.println(db.addNode("User", "Neo"));
            System.out.println(db.addNode("User", "Morpheus"));
            System.out.println(db.addNode("Location", "Zion"));

            // 3. Create Relationships
            System.out.println("\n--- Linking ---");
            System.out.println(db.link("Neo", "Morpheus", "KNOWS"));
            System.out.println(db.link("Morpheus", "Zion", "LIVES_IN"));

            // 4. Query Data
            System.out.println("\n--- Querying 'Morpheus' ---");
            String result = db.query("Morpheus");
            System.out.println(result);

            // 5. Shortest Path
            System.out.println("\n--- Path Analysis (Neo -> Zion) ---");
            System.out.println(db.path("Neo", "Zion"));

        } catch (Exception e) {
            System.err.println("Driver Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}