package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

/**
 * A destructive command that completely wipes all data from the database.
 * For safety, this command requires a `--confirm` flag to be explicitly provided.
 */
public class NukeCommand extends AbstractCommand {
    @Override
    public String getName() { return "nuke"; }

    @Override
    public String getDescription() { return "Wipes the entire database. Usage: nuke --confirm"; }

    /**
     * Executes the database wipe process.
     * It checks for the presence of the `--confirm` flag before proceeding with the deletion.
     *
     * @param args The command arguments, which must include `--confirm`.
     * @param engine The {@link GraphEngine} instance whose data will be wiped.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        // Safety check to prevent accidental data loss.
        if (args.length < 2 || !args[1].equals("--confirm")) {
            printError("DANGER: This operation is irreversible.");
            printError("To proceed, type: nuke --confirm");
            return;
        }

        // Instruct the engine to wipe all data segments.
        engine.wipeDatabase();
        printSuccess("DATABASE WIPED. All nodes and relations have been destroyed.");
    }
}