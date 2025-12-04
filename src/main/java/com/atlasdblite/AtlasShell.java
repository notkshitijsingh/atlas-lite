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
        registry.register(new QueryCommand());
        registry.register(new SearchCommand());
        registry.register(new PathCommand()); // NEW: Pathfinding
        
        registry.register(new StatsCommand());
        registry.register(new BackupCommand());
        registry.register(new ExportCommand());
        registry.register(new NukeCommand());
        registry.register(new ServerCommand());
        registry.register(new IndexCommand());
        registry.register(new ExitCommand());

        Scanner scanner = new Scanner(System.in);
        printBanner();

        while (true) {
            System.out.print("atlas-sharded> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("help")) { registry.printHelp(); continue; }
            
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
        }
    }

    private static void printBanner() {
        System.out.println("    _   _   _            ____  ____  ");
        System.out.println("   / \\ | |_| | __ _ ___ |  _ \\| __ ) ");
        System.out.println("  / _ \\| __| |/ _` / __|| | | |  _ \\ ");
        System.out.println(" / ___ \\ |_| | (_| \\__ \\| |_| | |_) |");
        System.out.println("/_/   \\_\\__|_|\\__,_|___/|____/|____/ ");
        System.out.println("      ATLASDB-LITE v3.2      ");
        System.out.println("   Sharded | Encrypted | 'help' for commands ");
    }
}