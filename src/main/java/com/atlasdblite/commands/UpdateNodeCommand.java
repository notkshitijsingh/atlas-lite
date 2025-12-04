package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

/**
 * Command to add or update a property on a node.
 * It uses the interactive node resolver to find the target node.
 */
public class UpdateNodeCommand extends AbstractCommand {
    @Override
    public String getName() { return "update-node"; }

    @Override
    public String getDescription() { return "Updates a node. Usage: update-node <search_term> <key> <value>"; }

    /**
     * Executes the node update process.
     * It resolves the target node, then instructs the engine to set or update a property on it.
     *
     * @param args Command arguments containing the node search term, property key, and property value.
     * @param engine The {@link GraphEngine} instance where the update will be applied.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 3, "update-node <search_term> <key> <value>")) return;

        String query = args[1];
        String key = args[2];
        String value = args[3];

        // Resolve the target node, handling ambiguity if needed.
        Node target = resolveNode(query, engine);
        if (target == null) return; // Halt if node not found or user cancels.

        // The engine handles the logic of finding the node and updating its properties.
        boolean success = engine.updateNode(target.getId(), key, value);
        if (success) {
            printSuccess("Updated Node [" + target.getId() + "]: set " + key + "=" + value);
        } else {
            // This is unlikely if resolveNode succeeded, but provides a safeguard.
            printError("Failed to update node. It may have been deleted by another process.");
        }
    }
}