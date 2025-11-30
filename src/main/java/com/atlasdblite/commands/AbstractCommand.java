package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

import java.util.List;
import java.util.Scanner;

/**
 * Base class for all commands.
 * Implements common validation and interactive node resolution logic.
 */
public abstract class AbstractCommand implements Command {
    
    // Shared scanner for interactive menus (do not close this!)
    protected static final Scanner interactiveScanner = new Scanner(System.in);

    protected boolean validateArgs(String[] args, int expected, String usage) {
        if (args.length - 1 < expected) {
            System.out.println(" > Error: Invalid arguments.");
            System.out.println(" > Usage: " + usage);
            return false;
        }
        return true;
    }

    protected void printSuccess(String message) {
        System.out.println(" [OK] " + message);
    }
    
    protected void printError(String message) {
        System.out.println(" [ERR] " + message);
    }

    /**
     * Resolves a query string to a single Node.
     * 1. Checks for Exact ID match.
     * 2. Checks for Fuzzy Search match.
     * 3. If ambiguous, asks user to select from list.
     */
    protected Node resolveNode(String query, GraphEngine engine) {
        // 1. Try Exact ID Match first (Fastest/Safest)
        Node exactMatch = engine.getNode(query);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 2. Perform Fuzzy Search
        List<Node> matches = engine.search(query);

        if (matches.isEmpty()) {
            printError("No node found matching: '" + query + "'");
            return null;
        }

        // 3. Auto-Select if Unique
        if (matches.size() == 1) {
            Node found = matches.get(0);
            // Optional: print confirmation if it wasn't an ID match
            // System.out.println("   -> Resolved '" + query + "' to " + found.getId());
            return found;
        }

        // 4. Handle Ambiguity (Interactive Menu)
        System.out.println(" [?] Multiple matches found for '" + query + "'. Please select:");
        for (int i = 0; i < matches.size(); i++) {
            System.out.println("   [" + i + "] " + matches.get(i));
        }
        
        while (true) {
            System.out.print("   > Enter number (0-" + (matches.size() - 1) + ") or 'c' to cancel: ");
            String input = interactiveScanner.nextLine().trim();

            if (input.equalsIgnoreCase("c")) {
                System.out.println("   -> Operation cancelled.");
                return null;
            }

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 0 && choice < matches.size()) {
                    return matches.get(choice);
                }
                printError("Invalid number.");
            } catch (NumberFormatException e) {
                printError("Invalid input.");
            }
        }
    }
}