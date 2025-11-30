package com.atlasdblite;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.commands.*;
import java.util.Scanner;

public class AtlasShell {
    // NOW A DIRECTORY
    private static final String DB_DIR = "atlas_db";

    public static void main(String[] args) {
        GraphEngine engine = new GraphEngine(DB_DIR);
        CommandRegistry registry = new CommandRegistry();

        // Register Commands (Same as before)
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

        Scanner scanner = new Scanner(System.in);
        printBanner();

        while (true) {
            System.out.print("atlas-sharded> "); // Updated Prompt
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("exit")) break;
            if (input.equalsIgnoreCase("clear")) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
                continue;
            }
            if (input.equalsIgnoreCase("help")) {
                registry.printHelp();
                continue;
            }

            String[] tokens = input.split("\\s+");
            String commandName = tokens[0];

            Command cmd = registry.get(commandName);
            if (cmd != null) {
                try {
                    cmd.execute(tokens, engine);
                } catch (Exception e) {
                    System.out.println(" [CRASH] Command failed: " + e.getMessage());
                }
            } else {
                System.out.println(" Unknown command. Type 'help'.");
            }
        }
        
        new ServerCommand().execute(new String[]{"server", "stop"}, engine);
        System.out.println("Session closed. Shards saved.");
        System.exit(0);
        scanner.close();
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