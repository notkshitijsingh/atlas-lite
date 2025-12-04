package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

public class UpdateNodeCommand extends AbstractCommand {
    @Override
    public String getName() { return "update-node"; }

    @Override
    public String getDescription() { return "Updates a node. Usage: update-node <search_term> <key> <value>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 3, "update-node <search_term> <key> <value>")) return;

        String query = args[1];
        String key = args[2];
        String value = args[3];

        Node target = resolveNode(query, engine);
        if (target == null) return; 

        boolean success = engine.updateNode(target.getId(), key, value);
        if (success) {
            printSuccess("Updated Node [" + target.getId() + "]: set " + key + "=" + value);
        } else {
            printError("Failed to update node.");
        }
    }
}