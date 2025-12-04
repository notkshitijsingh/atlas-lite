package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.List;

/**
 * Command to perform a simple graph traversal.
 * It finds all nodes that are connected to a given starting node by a specific relationship type.
 */
public class QueryCommand extends AbstractCommand {
    @Override
    public String getName() { return "query"; }

    @Override
    public String getDescription() { return "Finds related nodes. Usage: query <id> <relation_type>"; }

    /**
     * Executes the traversal query.
     *
     * @param args Command arguments containing the starting node ID and the relationship type.
     * @param engine The {@link GraphEngine} instance to perform the traversal on.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 2, "query <id> <relation_type>")) return;

        String startId = args[1];
        String relType = args[2];
        
        // The engine's traverse method handles the logic of finding connected nodes.
        List<Node> results = engine.traverse(startId, relType);
        
        System.out.println("Search Results (" + startId + " -[:" + relType + "]-> ? )");
        if (results.isEmpty()) {
            System.out.println(" > No matching nodes found.");
        } else {
            for (Node n : results) {
                System.out.println(" > Found: " + n);
            }
        }
    }
}