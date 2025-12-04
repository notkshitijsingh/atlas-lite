package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.List;
import java.util.stream.Collectors;

public class SelectCommand extends AbstractCommand {
    @Override
    public String getName() { return "select"; }

    @Override
    public String getDescription() { 
        return "Runs AQL queries. Usage: select <label> where <key> <op> <val>"; 
    }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        
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

        List<Node> results = engine.getAllNodes().stream()
            .filter(n -> n.getLabel().equalsIgnoreCase(targetLabel))
            .filter(n -> checkCondition(n, key, op, val))
            .collect(Collectors.toList());

        printTable(results);
    }

    private boolean checkCondition(Node n, String key, String op, String expectedVal) {
        String actualVal = n.getProperties().get(key);
        if (actualVal == null) return false; 

        switch (op.toLowerCase()) {
            case "=": return actualVal.equalsIgnoreCase(expectedVal);
            case "!=": return !actualVal.equalsIgnoreCase(expectedVal);
            case "contains": return actualVal.toLowerCase().contains(expectedVal.toLowerCase());
            case ">": 
            case "<":
                try {
                    double actualNum = Double.parseDouble(actualVal);
                    double expectedNum = Double.parseDouble(expectedVal);
                    return op.equals(">") ? actualNum > expectedNum : actualNum < expectedNum;
                } catch (NumberFormatException e) {
                    return false; 
                }
            default:
                return false;
        }
    }

    private void printTable(List<Node> nodes) {
        if (nodes.isEmpty()) {
            System.out.println(" > No results found.");
            return;
        }

        System.out.println(String.format("+-%-8s-+-%-15s-+-%-30s-+", "--------", "---------------", "------------------------------"));
        System.out.println(String.format("| %-8s | %-15s | %-30s |", "ID", "LABEL", "PROPERTIES"));
        System.out.println(String.format("+-%-8s-+-%-15s-+-%-30s-+", "--------", "---------------", "------------------------------"));

        for (Node n : nodes) {
            String props = n.getProperties().toString();
            if (props.length() > 30) props = props.substring(0, 27) + "...";
            
            System.out.println(String.format("| %-8s | %-15s | %-30s |", 
                n.getId(), n.getLabel(), props));
        }
        System.out.println(String.format("+-%-8s-+-%-15s-+-%-30s-+", "--------", "---------------", "------------------------------"));
        System.out.println(" > Found " + nodes.size() + " records.");
    }
}