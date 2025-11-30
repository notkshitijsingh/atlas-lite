package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

public class LinkCommand extends AbstractCommand {
    @Override
    public String getName() { return "link"; }

    @Override
    public String getDescription() { return "Connects nodes. Usage: link <from_search> <to_search> <type>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 3, "link <from_search> <to_search> <type>")) return;

        // Use inherited resolveNode()
        System.out.println(" ... Resolving Source: '" + args[1] + "'");
        Node sourceNode = resolveNode(args[1], engine);
        if (sourceNode == null) return; 

        System.out.println(" ... Resolving Target: '" + args[2] + "'");
        Node targetNode = resolveNode(args[2], engine);
        if (targetNode == null) return; 

        try {
            engine.persistRelation(sourceNode.getId(), targetNode.getId(), args[3].toUpperCase());
            printSuccess("Linked: " + sourceNode.getId() + " --[" + args[3].toUpperCase() + "]--> " + targetNode.getId());
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }
}