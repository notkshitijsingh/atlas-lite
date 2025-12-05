package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import java.util.Map;

public class AnalyzeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return "analyze";
    }

    @Override
    public String getDescription() {
        return "Runs graph algorithms. Usage: analyze pagerank [iterations]";
    }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "analyze pagerank [iterations]"))
            return;

        String algo = args[1].toLowerCase();

        if ("pagerank".equals(algo)) {
            int iterations = 20; // Default
            if (args.length > 2) {
                try {
                    iterations = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    printError("Invalid iterations");
                    return;
                }
            }

            System.out.println(" ... Calculating PageRank (" + iterations + " iterations)...");
            long start = System.currentTimeMillis();

            Map<String, Double> scores = engine.calculatePageRank(iterations, 0.85);

            long end = System.currentTimeMillis();
            System.out.println(" [DONE] Calculation took " + (end - start) + "ms");

            // Print Top 10
            System.out.println("\n === TOP 10 INFLUENTIAL NODES ===");
            scores.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort Descending
                    .limit(10)
                    .forEach(e -> {
                        String label = engine.getNode(e.getKey()).getLabel();
                        System.out.printf("  #%-4s %-15s (Score: %.2f)%n",
                                e.getKey(), label, e.getValue());
                    });
            System.out.println(" ================================\n");
        } else {
            printError("Unknown algorithm. Supported: pagerank");
        }
    }
}