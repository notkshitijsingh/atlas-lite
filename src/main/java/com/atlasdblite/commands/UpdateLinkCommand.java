package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

/**
 * Command to update the type of an existing relationship between two nodes.
 * This is effectively a "rename" operation for a link.
 */
public class UpdateLinkCommand extends AbstractCommand {
    @Override
    public String getName() { return "update-link"; }

    @Override
    public String getDescription() { return "Renames link type. Usage: update-link <from> <to> <old_type> <new_type>"; }

    /**
     * Executes the link update process.
     * It resolves the source and target nodes, then instructs the engine to perform the update.
     *
     * @param args Command arguments containing search terms for source and target, the old type, and the new type.
     * @param engine The {@link GraphEngine} instance where the update will occur.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 4, "update-link <from> <to> <old_type> <new_type>")) return;

        // Resolve the source and target nodes involved in the relationship.
        System.out.println(" ... Resolving Source: '" + args[1] + "'");
        Node source = resolveNode(args[1], engine);
        if (source == null) return;

        System.out.println(" ... Resolving Target: '" + args[2] + "'");
        Node target = resolveNode(args[2], engine);
        if (target == null) return;

        String oldType = args[3].toUpperCase();
        String newType = args[4].toUpperCase();

        // The engine handles the atomic deletion of the old link and creation of the new one.
        boolean success = engine.updateRelation(source.getId(), target.getId(), oldType, newType);
        if (success) {
            printSuccess("Updated link: " + source.getId() + " --[" + newType + "]--> " + target.getId());
        } else {
            printError("Original link not found.");
        }
    }
}