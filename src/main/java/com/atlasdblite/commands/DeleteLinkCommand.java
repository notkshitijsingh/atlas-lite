package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

public class DeleteLinkCommand extends AbstractCommand {
    @Override
    public String getName() { return "delete-link"; }

    @Override
    public String getDescription() { return "Removes a link. Usage: delete-link <from_search> <to_search> <type>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 3, "delete-link <from_search> <to_search> <type>")) return;

        System.out.println(" ... Resolving Source: '" + args[1] + "'");
        Node source = resolveNode(args[1], engine);
        if (source == null) return;

        System.out.println(" ... Resolving Target: '" + args[2] + "'");
        Node target = resolveNode(args[2], engine);
        if (target == null) return;

        String type = args[3].toUpperCase();

        boolean success = engine.deleteRelation(source.getId(), target.getId(), type);
        if (success) {
            printSuccess("Deleted link: " + source.getId() + " --[" + type + "]--X " + target.getId());
        } else {
            printError("Link not found: " + source.getId() + " -[" + type + "]-> " + target.getId());
        }
    }
}