package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

/**
 * Command to create a new node in the graph.
 * Supports auto-ID generation and parsing of list-based properties.
 */
public class AddNodeCommand extends AbstractCommand {
    
    @Override
    public String getName() { return "add-node"; }

    @Override
    public String getDescription() { 
        return "Creates a node. Usage: add-node [ID] <LABEL> [key:val]... (Supports lists: tags:[a,b])"; 
    }

    /**
     * Executes the add-node logic.
     * Detects optional ID, parses key-value pairs, and handles list syntax (brackets).
     * @param args The arguments passed from the shell.
     * @param engine The core graph engine.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (args.length < 2) {
            printError("Usage: add-node [id] <label> [prop:val]...");
            return;
        }

        // Support a flag --vectorize (or -v). We remove flags from the
        // argument list so the rest of the parsing keeps the same index
        // expectations (args[0] is the command name).
        boolean vectorize = false;
        List<String> filtered = new ArrayList<>();
        filtered.add(args[0]);
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if ("--vectorize".equalsIgnoreCase(a) || "-v".equalsIgnoreCase(a)) {
                vectorize = true;
            } else {
                filtered.add(a);
            }
        }

        String[] fargs = filtered.toArray(new String[0]);

        String id;
        String label;
        int propStartIndex;

        // Smart Detection: Check if args[2] is a property (has :) to determine if ID was provided
        boolean isAutoId = false;
        if (args.length == 2) isAutoId = true;
        else if (args.length > 2 && args[2].contains(":")) isAutoId = true;

        if (isAutoId) {
            id = UUID.randomUUID().toString().substring(0, 8);
            label = args[1];
            propStartIndex = 2;
        } else {
            id = args[1];
            label = args[2];
            propStartIndex = 3;
        }

        Node node = new Node(id, label);

        for (int i = propStartIndex; i < args.length; i++) {
            String rawArg = args[i];
            
            // Split only on the first colon to separate Key from Value
            int splitIndex = rawArg.indexOf(":");
            if (splitIndex > 0) {
                String key = rawArg.substring(0, splitIndex);
                String value = rawArg.substring(splitIndex + 1);

                if (value.startsWith("[") && value.endsWith("]")) {
                    // Parse List: tags:[Java,Python] -> List<String>
                    String content = value.substring(1, value.length() - 1);
                    List<String> listValue = Arrays.stream(content.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    node.addProperty(key, listValue);
                } else {
                    // Standard String
                    node.addProperty(key, value);
                }
            }
        }

        if (vectorize) {
            node.addProperty("__vectorize", "true");
        }

        engine.persistNode(node);
        printSuccess("Node created: " + node + (vectorize ? " [vectorize]" : ""));
    }
}