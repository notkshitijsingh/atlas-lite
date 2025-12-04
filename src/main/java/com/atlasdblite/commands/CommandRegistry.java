package com.atlasdblite.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the registration and retrieval of all available shell commands.
 * This class acts as a central repository for {@link Command} implementations,
 * allowing the shell to look up commands by their string name.
 */
public class CommandRegistry {
    /** A map storing commands, keyed by their unique string name. */
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * Registers a new command, making it available to be executed from the shell.
     *
     * @param cmd The command instance to register.
     */
    public void register(Command cmd) {
        commands.put(cmd.getName(), cmd);
    }

    /**
     * Retrieves a command by its registered name.
     *
     * @param name The name of the command to look up.
     * @return The {@link Command} instance, or {@code null} if no command with that name is registered.
     */
    public Command get(String name) {
        return commands.get(name);
    }

    /**
     * Prints a formatted list of all registered commands and their descriptions to the console.
     * This serves as the primary help utility for the interactive shell.
     */
    public void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.printf("%-15s %s%n", "COMMAND", "DESCRIPTION");
        System.out.println("------------------------------------------------");
        // Sort commands by name for consistent output
        commands.values().stream()
            .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
            .forEach(cmd -> 
                System.out.printf("%-15s %s%n", cmd.getName(), cmd.getDescription())
            );
        System.out.println("------------------------------------------------");
    }
}