package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

import java.util.List;

/**
 * Command to find the shortest path between two nodes using a Breadth-First Search (BFS) algorithm.
 * The command resolves the start and end nodes from user queries before starting the pathfinding process.
 */
public class PathCommand extends AbstractCommand {
    @Override
    public String getName() { return "path"; }

    @Override
    public String getDescription() { return "Finds shortest path. Usage: path <from_search> <to_search>"; }

    /**
     * Executes the shortest path finding process.
     * It resolves the start and end nodes and then calls the engine's pathfinding algorithm.
     * The resulting path is then printed to the console in a user-friendly format.
     *
     * @param args Command arguments containing the search terms for the start and end nodes.
     * @param engine The {@link GraphEngine} instance used to find the path.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 2, "path <from_search> <to_search>")) return;

        // Resolve start and end nodes from user input.
        System.out.println(" ... Resolving Start Node: '" + args[1] + "'");
        Node start = resolveNode(args[1], engine);
        if (start == null) return;

        System.out.println(" ... Resolving End Node: '" + args[2] + "'");
        Node end = resolveNode(args[2], engine);
        if (end == null) return;

        System.out.println(" ... Calculating shortest path (Max Depth: 10)...");
        // Ask the engine to find the shortest path with a hardcoded max depth to prevent infinite loops.
        List<String> path = engine.findShortestPath(start.getId(), end.getId(), 10);

        if (path.isEmpty()) {
            printError("No path found between '" + start.getLabel() + "' and '" + end.getLabel() + "'");
        } else {
            // Print the resulting path in a readable format.
            System.out.println("\n [PATH FOUND] " + (path.size() - 1) + " hops:");
            System.out.println(" ==================================================");
            
            for (int i = 0; i < path.size(); i++) {
                String id = path.get(i);
                Node n = engine.getNode(id);
                
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