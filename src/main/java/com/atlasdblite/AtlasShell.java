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
        registry.register(new ShowCommand());
        registry.register(new QueryCommand());
        registry.register(new SearchCommand());
        registry.register(new StatsCommand());
        registry.register(new BackupCommand());
        registry.register(new ExportCommand());
        registry.register(new NukeCommand());
        registry.register(new ServerCommand());
        
        // NEW COMMANDS
        registry.register(new IndexCommand());
        registry.register(new ExitCommand());

        Scanner scanner = new Scanner(System.in);
        printBanner();

        while (true) {
            System.out.print("atlas-sharded> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            // Explicit help check restored
            if (input.equalsIgnoreCase("help")) {
                registry.printHelp();
                continue;
            }
            
            // Note: ExitCommand now handles "exit" logic, but we keep this check 
            // to allow 'exit' typed directly to find the command in the registry
            // or just break if not using execute() path.
            
            String[] tokens = input.split("\\s+");
            Command cmd = registry.get(tokens[0]);
            
            if (cmd != null) {
                try {
                    cmd.execute(tokens, engine);
                } catch (Exception e) {
                    System.out.println(" [CRASH] " + e.getMessage());
                }
            } else {
                System.out.println(" Unknown command. Type 'help'.");
            }
            scanner.close();
        }
    }

    private static void printBanner() {
        System.out.println("    _   _   _            ____  ____  ");
        System.out.println("   / \\ | |_| | __ _ ___ |  _ \\| __ ) ");
        System.out.println("  / _ \\| __| |/ _` / __|| | | |  _ \\ ");
        System.out.println(" / ___ \\ |_| | (_| \\__ \\| |_| | |_) |");
        System.out.println("/_/   \\_\\__|_|\\__,_|___/|____/|____/ ");
        System.out.println("      SCALABLE SHELL v3.0 | SHARDED  ");
    }
}