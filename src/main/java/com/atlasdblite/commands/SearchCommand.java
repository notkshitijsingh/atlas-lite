package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.List;
import java.util.stream.Collectors;

public class SearchCommand extends AbstractCommand {
    @Override
    public String getName() { return "search"; }

    @Override
    public String getDescription() { return "Fuzzy search for nodes. Usage: search <query>"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "search <text>")) return;

        String query = args[1].toLowerCase();
        
        List<Node> matches = engine.getAllNodes().stream()
            .filter(n -> matchesQuery(n, query))
            .collect(Collectors.toList());

        System.out.println("--- Search Results for '" + args[1] + "' ---");
        if (matches.isEmpty()) {
            System.out.println(" > No matches found.");
        } else {
            matches.forEach(n -> System.out.println(" > " + n));
        }
    }

    private boolean matchesQuery(Node node, String query) {
        if (node.getId().toLowerCase().contains(query)) return true;
        if (node.getLabel().toLowerCase().contains(query)) return true;
        
        return node.getProperties().values().stream()
                .anyMatch(val -> val.toLowerCase().contains(query));
    }
}