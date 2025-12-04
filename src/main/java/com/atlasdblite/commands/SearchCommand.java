package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to perform a "fuzzy" or "contains" search for nodes in the graph.
 * It searches the node's ID, label, and all of its property values for the given query term.
 */
public class SearchCommand extends AbstractCommand {
    @Override
    public String getName() { return "search"; }

    @Override
    public String getDescription() { return "Fuzzy search for nodes. Usage: search <query>"; }

    /**
     * Executes the search operation.
     * It iterates through all nodes in the graph and uses the {@code matchesQuery} helper
     * to determine if a node should be included in the results.
     *
     * @param args The command arguments, containing the search term.
     * @param engine The {@link GraphEngine} instance to search within.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "search <text>")) return;

        String query = args[1].toLowerCase();
        
        // The core search logic is delegated to the GraphEngine now.
        List<Node> matches = engine.search(query);

        System.out.println("--- Search Results for '" + args[1] + "' ---");
        if (matches.isEmpty()) {
            System.out.println(" > No matches found.");
        } else {
            matches.forEach(n -> System.out.println(" > " + n));
        }
    }

    /**
     * Checks if a node matches the given search query.
     * The match is case-insensitive and checks for containment within the node's ID, label,
     * and any of its property values.
     * <p>
     * Note: This method is kept for clarity but the primary search logic
     * has been moved into the {@link GraphEngine} for better performance,
     * especially when indexing is enabled.
     *
     * @param node The node to check.
     * @param query The search term.
     * @return {@code true} if the node is a match, {@code false} otherwise.
     */
    private boolean matchesQuery(Node node, String query) {
        if (node.getId().toLowerCase().contains(query)) return true;
        if (node.getLabel().toLowerCase().contains(query)) return true;
        
        // Check all property values for a match.
        return node.getProperties().values().stream()
                .anyMatch(val -> val.toLowerCase().contains(query));
    }
}