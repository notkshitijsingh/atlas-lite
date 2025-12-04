package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

/**
 * Command to delete a node and all its incident edges from the graph.
 * This command uses the interactive node resolver to ensure the correct node is deleted.
 */
public class DeleteNodeCommand extends AbstractCommand {
    @Override
    public String getName() { return "delete-node"; }

    @Override
    public String getDescription() { return "Deletes a node. Usage: delete-node <search_term>"; }

    /**
     * Executes the node deletion process.
     * It resolves the target node using the shared {@code resolveNode} method and, if successful,
     * instructs the graph engine to delete it.
     *
     * @param args The command arguments, containing the search term for the node to be deleted.
     * @param engine The {@link GraphEngine} instance from which the node will be deleted.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "delete-node <search_term>")) return;

        String query = args[1];

        // Use the shared resolver to find the target node, handling ambiguity if necessary.
        Node target = resolveNode(query, engine);
        if (target == null) return; // Operation was cancelled or node was not found.

        // Instruct the engine to perform the deletion.
        boolean success = engine.deleteNode(target.getId());
        if (success) {
            printSuccess("Deleted Node [" + target.getId() + "] and all connected edges.");
        } else {
            // This case is unlikely if resolveNode succeeded, but is included for robustness.
            printError("Failed to delete node.");
        }
    }
}