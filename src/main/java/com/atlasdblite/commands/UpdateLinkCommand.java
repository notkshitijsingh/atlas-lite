package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

public class UpdateLinkCommand extends AbstractCommand {
    @Override
    public String getName() { return "update-link"; }

    @Override
    public String getDescription() { return "Renames link type. Usage: update-link <from> <to> <old_type> <new_type>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 4, "update-link <from> <to> <old_type> <new_type>")) return;

        System.out.println(" ... Resolving Source: '" + args[1] + "'");
        Node source = resolveNode(args[1], engine);
        if (source == null) return;

        System.out.println(" ... Resolving Target: '" + args[2] + "'");
        Node target = resolveNode(args[2], engine);
        if (target == null) return;

        String oldType = args[3].toUpperCase();
        String newType = args[4].toUpperCase();

        boolean success = engine.updateRelation(source.getId(), target.getId(), oldType, newType);
        if (success) {
            printSuccess("Updated link: " + source.getId() + " --[" + newType + "]--> " + target.getId());
        } else {
            printError("Original link not found.");
        }
    }
}