package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ImportCommand extends AbstractCommand {
    @Override
    public String getName() { return "import"; }

    @Override
    public String getDescription() { 
        return "Bulk imports data from CSV. Usage: import <file.csv> --type=<node|link>"; 
    }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 2, "import <file.csv> --type=<node|link>")) return;

        String filePath = args[1];
        String typeArg = args[2].toLowerCase();
        boolean isNode = typeArg.contains("node");
        boolean isLink = typeArg.contains("link");

        if (!isNode && !isLink) {
            printError("Invalid type. Use --type=node or --type=link");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            printError("File not found: " + filePath);
            return;
        }

        System.out.println(" ... Reading CSV: " + file.getName());
        int count = 0;
        int errors = 0;

        // Turn off auto-indexing temporarily for speed if inserting massive data
        boolean wasIndexing = engine.isAutoIndexing();
        if (wasIndexing) {
            System.out.println(" ... Pausing Auto-Index for bulk load performance...");
            engine.setAutoIndexing(false); 
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Read Header: id,label,name,age:int
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.split(",");
            
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = line.split(","); // Note: Simple split, doesn't handle quotes containing commas
                    
                    if (isNode) {
                        importNode(headers, parts, engine);
                    } else {
                        importLink(headers, parts, engine);
                    }
                    count++;
                    if (count % 1000 == 0) System.out.print("."); // Progress bar
                } catch (Exception e) {
                    errors++;
                    // System.out.println("Skipped line: " + e.getMessage());
                }
            }
            System.out.println(); // Newline after progress dots

        } catch (Exception e) {
            printError("Import crashed: " + e.getMessage());
        } finally {
            // Restore indexing (Rebuilds index)
            if (wasIndexing) {
                System.out.println(" ... Rebuilding Search Index (this may take a moment)...");
                engine.setAutoIndexing(true);
            }
        }

        printSuccess("Imported " + count + " items. (" + errors + " skipped)");
    }

    private void importNode(String[] headers, String[] parts, GraphEngine engine) {
        // Expected Header 0=id, 1=label, rest=props
        String id = parts[0].trim();
        String label = parts[1].trim();
        Node node = new Node(id, label);

        for (int i = 2; i < headers.length && i < parts.length; i++) {
            parseAndAddProp(node, headers[i], parts[i]);
        }
        engine.persistNode(node);
    }

    private void importLink(String[] headers, String[] parts, GraphEngine engine) {
        // Expected Header 0=from, 1=to, 2=type, rest=props
        String from = parts[0].trim();
        String to = parts[1].trim();
        String type = parts[2].trim();
        Map<String, Object> props = new HashMap<>();

        for (int i = 3; i < headers.length && i < parts.length; i++) {
            addMapProp(props, headers[i], parts[i]);
        }
        engine.persistRelation(from, to, type, props);
    }

    // Helper to parse "age:int" -> 25
    private void parseAndAddProp(Node node, String header, String value) {
        String[] headerParts = header.split(":");
        String key = headerParts[0].trim();
        String type = headerParts.length > 1 ? headerParts[1].toLowerCase() : "string";
        
        node.addProperty(key, castValue(value, type));
    }

    private void addMapProp(Map<String, Object> map, String header, String value) {
        String[] headerParts = header.split(":");
        String key = headerParts[0].trim();
        String type = headerParts.length > 1 ? headerParts[1].toLowerCase() : "string";
        
        map.put(key, castValue(value, type));
    }

    private Object castValue(String value, String type) {
        try {
            switch (type) {
                case "int": return Integer.parseInt(value);
                case "double": 
                case "float": return Double.parseDouble(value);
                case "bool": 
                case "boolean": return Boolean.parseBoolean(value);
                case "list": return Arrays.asList(value.split(";")); // Semi-colon separated list
                default: return value;
            }
        } catch (Exception e) {
            return value; // Fallback to string on error
        }
    }
}