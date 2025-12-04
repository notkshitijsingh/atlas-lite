package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

/**
 * Command to enable or disable the automatic in-memory indexing feature.
 * When enabled, the graph engine maintains an inverted index for faster lookups (O(1) on average).
 * When disabled, searches revert to a linear scan (O(N)).
 */
public class IndexCommand extends AbstractCommand {
    @Override
    public String getName() { return "index"; }

    @Override
    public String getDescription() { return "Toggles auto-indexing. Usage: index <on|off>"; }

    /**
     * Executes the command to toggle the indexing state.
     *
     * @param args The command arguments, where {@code args[1]} should be "on" or "off".
     * @param engine The {@link GraphEngine} whose indexing state will be modified.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (!validateArgs(args, 1, "index <on|off>")) return;

        String state = args[1].toLowerCase();
        if ("on".equals(state)) {
            engine.setAutoIndexing(true);
            printSuccess("Auto-Indexing ENABLED. Queries will use O(1) lookup map.");
        } else if ("off".equals(state)) {
            engine.setAutoIndexing(false);
            printSuccess("Auto-Indexing DISABLED. Queries will use O(N) scan.");
        } else {
            printError("Invalid argument. Use 'on' or 'off'.");
        }
    }
}