package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

public class DeleteNodeCommand extends AbstractCommand {
    @Override
    public String getName() { return "delete-node"; }

    @Override
    public String getDescription() { return "Deletes a node. Usage: delete-node <search_term>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "delete-node <search_term>")) return;

        String query = args[1];

        // Use the new shared intelligence
        Node target = resolveNode(query, engine);
        if (target == null) return; // Cancelled or not found

        boolean success = engine.deleteNode(target.getId());
        if (success) {
            printSuccess("Deleted Node [" + target.getId() + "] and all connected edges.");
        } else {
            printError("Failed to delete node.");
        }
    }
}