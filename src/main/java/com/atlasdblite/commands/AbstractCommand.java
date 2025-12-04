package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;

import java.util.List;
import java.util.Scanner;

/**
 * An abstract base class for shell commands, providing common utility methods
 * for argument validation, printing formatted output, and resolving nodes from user queries.
 * This class simplifies the implementation of new commands by handling repetitive logic.
 */
public abstract class AbstractCommand implements Command {
    
    /** A shared Scanner instance for handling interactive user input. This should not be closed. */
    protected static final Scanner interactiveScanner = new Scanner(System.in);

    /**
     * Validates if the number of provided arguments meets the expected count.
     *
     * @param args The full array of command-line arguments.
     * @param expected The minimum number of arguments required (excluding the command name itself).
     * @param usage A string explaining the correct usage, shown if validation fails.
     * @return {@code true} if arguments are valid, {@code false} otherwise.
     */
    protected boolean validateArgs(String[] args, int expected, String usage) {
        if (args.length - 1 < expected) {
            System.out.println(" > Error: Invalid arguments.");
            System.out.println(" > Usage: " + usage);
            return false;
        }
        return true;
    }

    /**
     * Prints a standardized success message to the console.
     *
     * @param message The message to display.
     */
    protected void printSuccess(String message) {
        System.out.println(" [OK] " + message);
    }
    
    /**
     * Prints a standardized error message to the console.
     *
     * @param message The message to display.
     */
    protected void printError(String message) {
        System.out.println(" [ERR] " + message);
    }

    /**
     * Resolves a user-provided query string to a single {@link Node}.
     * The method follows a specific resolution strategy:
     * <ol>
     *   <li>Attempts to find a node by exact ID match.</li>
     *   <li>If no exact match is found, performs a fuzzy search across all node properties.</li>
     *   <li>If multiple nodes match the fuzzy search, it presents an interactive menu for the user to select the correct one.</li>
     * </ol>
     *
     * @param query The search query (can be an ID or a search term).
     * @param engine The {@link GraphEngine} instance to perform the search on.
     * @return The resolved {@link Node}, or {@code null} if no node is found or the user cancels the selection.
     */
    protected Node resolveNode(String query, GraphEngine engine) {
        // 1. Try for a direct, fast lookup by exact ID.
        Node exactMatch = engine.getNode(query);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 2. If no exact match, perform a broader, slower search.
        List<Node> matches = engine.search(query);

        if (matches.isEmpty()) {
            printError("No node found matching: '" + query + "'");
            return null;
        }

        // 3. If the search returns a single, unambiguous result, use it.
        if (matches.size() == 1) {
            Node found = matches.get(0);
            return found;
        }

        // 4. If the search is ambiguous, require the user to clarify.
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