package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to execute a simple query language (AQL - Atlas Query Language)
 * to filter nodes based on their label and property values.
 * Syntax: `select <Label> where <Key> <Operator> <Value>`
 */
public class SelectCommand extends AbstractCommand {
    @Override
    public String getName() { return "select"; }

    @Override
    public String getDescription() { 
        return "Runs AQL queries. Usage: select <label> where <key> <op> <val>"; 
    }

    /**
     * Executes the AQL query.
     * It parses the label, property key, operator, and value from the arguments,
     * then filters all nodes in the graph to find matches.
     *
     * @param args The command arguments making up the query.
     * @param engine The {@link GraphEngine} to be queried.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        // Example: select Person where age > 18
        //           args[0]  args[1] args[2] args[3] args[4] args[5]
        if (args.length < 6 || !args[2].equalsIgnoreCase("where")) {
            printError("Invalid Syntax. Usage: select <Label> where <Key> <Op> <Value>");
            printError("Operators: = , != , > , < , contains");
            return;
        }

        String targetLabel = args[1];
        String key = args[3];
        String op = args[4];
        String val = args[5];

        System.out.println(" ... Scanning for " + targetLabel + " where " + key + " " + op + " " + val);

        // This is a full-scan query and can be slow on large graphs.
        // A more advanced implementation would use indexes.
        List<Node> results = engine.getAllNodes().stream()
            // 1. Filter by the node's label (case-insensitive).
            .filter(n -> n.getLabel().equalsIgnoreCase(targetLabel))
            // 2. Filter by the property condition.
            .filter(n -> checkCondition(n, key, op, val))
            .collect(Collectors.toList());

        printTable(results);
    }

    /**
     * Checks if a node's property satisfies a given condition.
     *
     * @param n The node to check.
     * @param key The property key to inspect.
     * @param op The comparison operator (e.g., "=", ">", "contains").
     * @param expectedVal The value to compare against.
     * @return {@code true} if the condition is met, {@code false} otherwise.
     */
    private boolean checkCondition(Node n, String key, String op, String expectedVal) {
        String actualVal = n.getProperties().get(key);
        if (actualVal == null) return false; // Property doesn't exist on this node.

        switch (op.toLowerCase()) {
            case "=": return actualVal.equalsIgnoreCase(expectedVal);
            case "!=": return !actualVal.equalsIgnoreCase(expectedVal);
            case "contains": return actualVal.toLowerCase().contains(expectedVal.toLowerCase());
            case ">": 
            case "<":
                // For numeric comparisons, attempt to parse both values as doubles.
                try {
                    double actualNum = Double.parseDouble(actualVal);
                    double expectedNum = Double.parseDouble(expectedVal);
                    return op.equals(">") ? actualNum > expectedNum : actualNum < expectedNum;
                } catch (NumberFormatException e) {
                    return false; // Cannot compare non-numeric values.
                }
            default:
                return false;
        }
    }

    /**
     * Prints a list of nodes in a formatted ASCII table.
     *
     * @param nodes The list of nodes to print.
     */
    private void printTable(List<Node> nodes) {
        if (nodes.isEmpty()) {
            System.out.println(" > No results found.");
            return;
        }

        // Define table headers and format strings
        System.out.println(String.format("+-%-8s-+-%-15s-+-%-30s-+", "--------", "---------------", "------------------------------"));
        System.out.println(String.format("| %-8s | %-15s | %-30s |", "ID", "LABEL", "PROPERTIES"));
        System.out.println(String.format("+-%-8s-+-%-15s-+-%-30s-+", "--------", "---------------", "------------------------------"));

        for (Node n : nodes) {
            String props = n.getProperties().toString();
            // Truncate long property strings to fit the table.
            if (props.length() > 30) props = props.substring(0, 27) + "...";
            
            System.out.println(String.format("| %-8s | %-15s | %-30s |", 
                n.getId(), n.getLabel(), props));
        }
        System.out.println(String.format("+-%-8s-+-%-15s-+-%-30s-+", "--------", "---------------", "------------------------------"));
        System.out.println(" > Found " + nodes.size() + " records.");
    }
}