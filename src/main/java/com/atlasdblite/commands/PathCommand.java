package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

import java.util.List;

public class PathCommand extends AbstractCommand {
    @Override
    public String getName() { return "path"; }

    @Override
    public String getDescription() { return "Finds path. Usage: path <from> <to> [weight_prop] [min|max]"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 2, "path <from> <to> [weight_prop] [min|max]")) return;

        // Smart Resolve
        System.out.println(" ... Resolving Start Node: '" + args[1] + "'");
        Node start = resolveNode(args[1], engine);
        if (start == null) return;

        System.out.println(" ... Resolving End Node: '" + args[2] + "'");
        Node end = resolveNode(args[2], engine);
        if (end == null) return;

        List<String> path;
        double cost = 0.0;
        String mode;

        // Weighted Path Logic
        if (args.length > 3) {
            String weightKey = args[3];
            boolean findLowest = true; // Default to Min Cost
            
            // Check for optional "max" flag
            if (args.length > 4 && "max".equalsIgnoreCase(args[4])) {
                findLowest = false;
            }

            mode = (findLowest ? "Lowest" : "Highest") + " Cost (Weighted by '" + weightKey + "')";
            System.out.println(" ... Calculating " + mode + "...");
            
            GraphEngine.PathResult result = engine.findWeightedPath(start.getId(), end.getId(), weightKey, findLowest);
            if (result != null) {
                path = result.path;
                cost = result.totalCost;
            } else {
                path = java.util.Collections.emptyList();
            }
        } 
        // BFS Logic (Default if no weight provided)
        else {
            mode = "BFS (Fewest Hops)";
            System.out.println(" ... Calculating " + mode + "...");
            path = engine.findShortestPath(start.getId(), end.getId(), 10);
            cost = path.size() > 0 ? path.size() - 1 : 0;
        }

        if (path.isEmpty()) {
            printError("No path found between '" + start.getLabel() + "' and '" + end.getLabel() + "'");
        } else {
            System.out.println("\n [PATH FOUND] Mode: " + mode);
            System.out.println(" [TOTAL COST] " + cost);
            System.out.println(" ==================================================");
            
            for (int i = 0; i < path.size(); i++) {
                String id = path.get(i);
                Node n = engine.getNode(id);
                String display = (n != null) ? n.toString() : "[ID: " + id + "]";
                
                if (i == 0) {
                    System.out.println("   (START) " + display);
                } else {
                    System.out.println("      |    ");
                    System.out.println("      v    ");
                    if (i == path.size() - 1) {
                        System.out.println("   (END)   " + display);
                    } else {
                        System.out.println("   (HOP " + i + ")  " + display);
                    }
                }
            }
            System.out.println(" ==================================================\n");
        }
    }
}