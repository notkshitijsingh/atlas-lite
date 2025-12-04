package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

/**
 * Defines the contract for all executable shell commands in AtlasDB-Lite.
 * Each command must have a name, a description, and an execution method.
 */
public interface Command {
    
    /**
     * Gets the unique name of the command (e.g., "add-node", "search").
     * This name is used to invoke the command from the shell.
     *
     * @return The command's name, which should be a single lowercase word.
     */
    String getName();

    /**
     * Gets a brief, user-friendly description of what the command does.
     * This is displayed in the "help" output.
     *
     * @return A short description of the command's purpose and usage.
     */
    String getDescription();

    /**
     * Executes the primary logic of the command.
     *
     * @param args The full array of arguments passed from the shell,
     *             where {@code args[0]} is the command name itself.
     * @param engine The instance of the {@link GraphEngine} to operate on.
     */
    void execute(String[] args, GraphEngine engine);
}