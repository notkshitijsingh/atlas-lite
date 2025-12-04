package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

/**
 * Command to delete a directed relationship (link) between two nodes.
 * The command resolves the source and target nodes from user queries before attempting deletion.
 */
public class DeleteLinkCommand extends AbstractCommand {
    @Override
    public String getName() { return "delete-link"; }

    @Override
    public String getDescription() { return "Removes a link. Usage: delete-link <from_search> <to_search> <type>"; }

    /**
     * Executes the link deletion process.
     * It uses the inherited {@code resolveNode} method to find the source and target nodes
     * based on user-provided search terms.
     *
     * @param args The command arguments, including source/target search terms and the link type.
     * @param engine The {@link GraphEngine} instance from which the link will be removed.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 3, "delete-link <from_search> <to_search> <type>")) return;

        // Resolve the source and target nodes using the intelligent resolver from AbstractCommand.
        System.out.println(" ... Resolving Source: '" + args[1] + "'");
        Node source = resolveNode(args[1], engine);
        if (source == null) return; // Stop if source not found or user cancels.

        System.out.println(" ... Resolving Target: '" + args[2] + "'");
        Node target = resolveNode(args[2], engine);
        if (target == null) return; // Stop if target not found or user cancels.

        String type = args[3].toUpperCase();

        // Attempt to delete the relationship from the graph engine.
        boolean success = engine.deleteRelation(source.getId(), target.getId(), type);
        if (success) {
            printSuccess("Deleted link: " + source.getId() + " --[" + type + "]--X " + target.getId());
        } else {
            printError("Link not found: " + source.getId() + " -[" + type + "]-> " + target.getId());
        }
    }
}