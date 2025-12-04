package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

/**
 * Command to create a directed relationship (link) between two nodes.
 * It intelligently resolves the source and target nodes from user queries before creating the link.
 */
public class LinkCommand extends AbstractCommand {
    @Override
    public String getName() { return "link"; }

    @Override
    public String getDescription() { return "Connects nodes. Usage: link <from_search> <to_search> <type>"; }

    /**
     * Executes the link creation process.
     * It uses the inherited {@code resolveNode} method to find both the source and target nodes,
     * handling any ambiguity through interactive prompts.
     *
     * @param args The command arguments, containing the source/target search terms and the link type.
     * @param engine The {@link GraphEngine} instance where the new relationship will be stored.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 3, "link <from_search> <to_search> <type>")) return;

        // Use the shared, intelligent node resolver to find the source node.
        System.out.println(" ... Resolving Source: '" + args[1] + "'");
        Node sourceNode = resolveNode(args[1], engine);
        if (sourceNode == null) return; // Halt if node not found or user cancelled.

        // Resolve the target node.
        System.out.println(" ... Resolving Target: '" + args[2] + "'");
        Node targetNode = resolveNode(args[2], engine);
        if (targetNode == null) return; // Halt if node not found or user cancelled.

        try {
            // Persist the new relationship to the database.
            String relationType = args[3].toUpperCase();
            engine.persistRelation(sourceNode.getId(), targetNode.getId(), relationType);
            printSuccess("Linked: " + sourceNode.getId() + " --[" + relationType + "]--> " + targetNode.getId());
        } catch (Exception e) {
            // Catch potential errors, such as if a node was deleted between resolution and persistence.
            printError(e.getMessage());
        }
    }
}