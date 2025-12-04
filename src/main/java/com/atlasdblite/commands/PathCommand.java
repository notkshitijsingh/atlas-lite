package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

import java.util.List;

public class PathCommand extends AbstractCommand {
    @Override
    public String getName() { return "path"; }

    @Override
    public String getDescription() { return "Finds shortest path. Usage: path <from_search> <to_search>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 2, "path <from_search> <to_search>")) return;

        // Smart Resolve
        System.out.println(" ... Resolving Start Node: '" + args[1] + "'");
        Node start = resolveNode(args[1], engine);
        if (start == null) return;

        System.out.println(" ... Resolving End Node: '" + args[2] + "'");
        Node end = resolveNode(args[2], engine);
        if (end == null) return;

        System.out.println(" ... Calculating shortest path (Max Depth: 10)...");
        List<String> path = engine.findShortestPath(start.getId(), end.getId(), 10);

        if (path.isEmpty()) {
            printError("No path found between '" + start.getLabel() + "' and '" + end.getLabel() + "'");
        } else {
            System.out.println("\n [PATH FOUND] " + (path.size() - 1) + " hops:");
            System.out.println(" ==================================================");
            
            for (int i = 0; i < path.size(); i++) {
                String id = path.get(i);
                Node n = engine.getNode(id);
                
                // n.toString() now includes properties based on your Node model update
                String display = (n != null) ? n.toString() : "[ID: " + id + " (Missing)]";
                
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