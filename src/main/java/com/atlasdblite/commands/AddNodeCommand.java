package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.UUID;

/**
 * Command to create a new node in the graph.
 * This command supports both providing an explicit ID and auto-generating one.
 * It also parses key-value properties from the command-line arguments.
 */
public class AddNodeCommand extends AbstractCommand {
    @Override
    public String getName() { return "add-node"; }

    @Override
    public String getDescription() { 
        return "Creates a node. Usage: add-node [ID] <LABEL> [key:val]... (ID auto-generated if omitted)"; 
    }

    /**
     * Executes the node creation process.
     * The command intelligently determines whether an ID is provided or needs to be auto-generated
     * based on the position of property arguments (those containing a ':').
     *
     * @param args The command arguments, including the command name, optional ID, label, and properties.
     * @param engine The {@link GraphEngine} instance to which the new node will be persisted.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (args.length < 2) {
            printError("Usage: add-node [id] <label> [prop:val]...");
            return;
        }

        String id;
        String label;
        int propStartIndex;

        // --- Smart ID Detection ---
        // This logic determines if the user has provided an explicit ID or if one should be generated.
        // It assumes if the second argument contains a colon, it's a property, and thus the ID was omitted.
        // It also handles the simple case of just 'add-node <label>'.
        boolean isAutoId = false;
        
        if (args.length == 2) {
            // Case: `add-node Person` (No ID, no properties)
            isAutoId = true;
        } else if (args.length > 2 && args[2].contains(":")) {
            // Case: `add-node Person name:John` (ID omitted, properties start at index 2)
            isAutoId = true;
        }

        if (isAutoId) {
            // Generate a short, unique ID.
            id = UUID.randomUUID().toString().substring(0, 8); 
            label = args[1];
            propStartIndex = 2; // Properties start after the label.
        } else {
            // Case: `add-node p1 Person name:John` (Explicit ID provided)
            id = args[1];
            label = args[2];
            propStartIndex = 3; // Properties start after the ID and label.
        }

        // Create the node object.
        Node node = new Node(id, label);

        // Parse and add any key-value properties.
        for (int i = propStartIndex; i < args.length; i++) {
            String[] prop = args[i].split(":", 2); // Split only on the first colon
            if (prop.length == 2) {
                node.addProperty(prop[0], prop[1]);
            }
        }

        // Persist the newly created node to the database.
        engine.persistNode(node);
        printSuccess("Node created: " + node);
    }
}