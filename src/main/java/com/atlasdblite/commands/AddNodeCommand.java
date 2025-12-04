package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import java.util.UUID;

public class AddNodeCommand extends AbstractCommand {
    @Override
    public String getName() { return "add-node"; }

    @Override
    public String getDescription() { 
        return "Creates a node. Usage: add-node [ID] <LABEL> [key:val]... (ID auto-generated if omitted)"; 
    }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (args.length < 2) {
            printError("Usage: add-node [id] <label> [prop:val]...");
            return;
        }

        String id;
        String label;
        int propStartIndex;

        boolean isAutoId = false;
        
        if (args.length == 2) {
            isAutoId = true;
        } else if (args.length > 2 && args[2].contains(":")) {
            isAutoId = true;
        }

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
            String[] prop = args[i].split(":");
            if (prop.length == 2) {
                node.addProperty(prop[0], prop[1]);
            }
        }

        engine.persistNode(node);
        printSuccess("Node created: " + node);
    }
}