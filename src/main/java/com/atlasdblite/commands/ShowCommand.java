package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

/**
 * Command to display all nodes currently in the database.
 * This provides a simple way to get a quick overview of the graph's contents.
 */
public class ShowCommand extends AbstractCommand {
    @Override
    public String getName() { return "show"; }

    @Override
    public String getDescription() { return "Lists all nodes in the database."; }

    /**
     * Executes the command to list all nodes.
     * It retrieves all nodes from the graph engine and prints their string representation.
     *
     * @param args Command-line arguments (not used by this command).
     * @param engine The {@link GraphEngine} instance to query for nodes.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        System.out.println("--- Current Nodes ---");
        // This can be a slow operation on a very large database as it loads all segments.
        for (Node n : engine.getAllNodes()) {
            System.out.println(n);
        }
        System.out.println("---------------------");
    }
}