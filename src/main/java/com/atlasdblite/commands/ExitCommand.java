package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

/**
 * Command to gracefully shut down the interactive shell.
 * It ensures that all in-memory changes are persisted to disk before exiting.
 */
public class ExitCommand extends AbstractCommand {
    @Override
    public String getName() { return "exit"; }

    @Override
    public String getDescription() { return "Saves data and shuts down the shell."; }

    /**
     * Executes the shutdown sequence.
     * This involves committing all dirty data segments to disk and then terminating the Java process.
     *
     * @param args Command-line arguments (not used by this command).
     * @param engine The {@link GraphEngine} instance containing the data to be saved.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        System.out.println(" [SHUTDOWN] Saving shards...");
        // Ensure all changes held in memory are written to their respective segment files.
        engine.commit();
        System.out.println(" [SHUTDOWN] Goodbye.");
        // Terminate the application.
        System.exit(0);
    }
}