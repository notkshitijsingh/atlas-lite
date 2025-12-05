package com.atlasdblite;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.commands.*;
import java.util.Scanner;

public class AtlasShell {
    private static final String DB_DIR = "atlas_db";

    public static void main(String[] args) {
        GraphEngine engine = new GraphEngine(DB_DIR);
        CommandRegistry registry = new CommandRegistry();

        registry.register(new AddNodeCommand());
        registry.register(new UpdateNodeCommand());
        registry.register(new DeleteNodeCommand());
        registry.register(new LinkCommand());
        registry.register(new DeleteLinkCommand());
        registry.register(new UpdateLinkCommand());
        
        registry.register(new ShowCommand());
        registry.register(new SelectCommand());
        registry.register(new QueryCommand());
        registry.register(new SearchCommand());
        registry.register(new PathCommand()); 
        
        registry.register(new StatsCommand());
        registry.register(new BackupCommand());
        registry.register(new CheckpointCommand());
        registry.register(new ImportCommand());
        registry.register(new ExportCommand());
        registry.register(new NukeCommand());
        registry.register(new ServerCommand());
        registry.register(new IndexCommand());
        registry.register(new ExitCommand());
        registry.register(new ClearCommand());
        registry.register(new AnalyzeCommand());

        Scanner scanner = new Scanner(System.in);
        
        System.out.print("\033[H\033[2J");
        System.out.flush();
        printBanner();

        while (true) {
            System.out.print("atlas> ");
            System.out.flush();
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("help")) { registry.printHelp(); continue; }
            
            String[] tokens = input.split("\\s+");
            Command cmd = registry.get(tokens[0]);
            
            if (cmd != null) {
                try {
                    long startTime = System.nanoTime();
                    cmd.execute(tokens, engine);
                    long endTime = System.nanoTime();
                    
                    double durationMs = (endTime - startTime) / 1_000_000.0;
                    System.out.printf(" [TIME] %.2f ms%n", durationMs);
                    
                } catch (Exception e) {
                    System.out.println(" [CRASH] " + e.getMessage());
                }
            } else {
                System.out.println(" Unknown command. Type 'help'.");
            }
        }
    }

    /**
     * Prints the application banner and version information to the console.
     */
    private static void printBanner() {
        System.out.println("    _   _   _            ____  ____  ");
        System.out.println("   / \\ | |_| | __ _ ___ |  _ \\| __ ) ");
        System.out.println("  / _ \\| __| |/ _` / __|| | | |  _ \\ ");
        System.out.println(" / ___ \\ |_| | (_| \\__ \\| |_| | |_) |");
        System.out.println("/_/   \\_\\__|_|\\__,_|___/|____/|____/ ");
        System.out.println("      ATLASDB-LITE v4.0      ");
        System.out.println("Encrypted | Dashboard | 'help' for commands ");
    }
}