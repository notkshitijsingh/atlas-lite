package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.models.Node;
import com.atlasdblite.models.Relation;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Command to export the entire graph to a DOT file.
 * The DOT format is a graph description language that can be used by tools like Graphviz
 * to generate visual representations of the graph.
 */
public class ExportCommand extends AbstractCommand {
    @Override
    public String getName() { return "export"; }

    @Override
    public String getDescription() { return "Exports graph to DOT format. Usage: export <filename.dot>"; }

    /**
     * Executes the export process.
     * It iterates through all nodes and relations in the graph and writes them
     * to the specified file in DOT syntax.
     *
     * @param args The command arguments, where {@code args[1]} is the output filename.
     * @param engine The {@link GraphEngine} instance containing the graph data to be exported.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "export <filename.dot>")) return;

        String filename = args[1];
        // Use a try-with-resources block to ensure the FileWriter is closed automatically.
        try (FileWriter fw = new FileWriter(filename)) {
            // Start the DOT graph definition.
            fw.write("digraph G {\n");
            
            // Define all nodes in the graph.
            for (Node n : engine.getAllNodes()) {
                // Format: "nodeId" [label="nodeId:NodeLabel"];
                fw.write(String.format("  \"%s\" [label=\"%s:%s\"];\n", 
                    n.getId(), n.getId(), n.getLabel()));
            }

            // Define all edges (relations) in the graph.
            for (Relation r : engine.getAllRelations()) {
                // Format: "sourceId" -> "targetId" [label="RELATION_TYPE"];
                fw.write(String.format("  \"%s\" -> \"%s\" [label=\"%s\"];\n", 
                    r.getSourceId(), r.getTargetId(), r.getType()));
            }

            // Close the graph definition.
            fw.write("}\n");
            printSuccess("Exported graph to " + filename);
        } catch (IOException e) {
            printError("Export failed: " + e.getMessage());
        }
    }
}