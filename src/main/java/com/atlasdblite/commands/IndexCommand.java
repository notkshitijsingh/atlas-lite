package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

public class IndexCommand extends AbstractCommand {
    @Override
    public String getName() { return "index"; }

    @Override
    public String getDescription() { return "Toggles auto-indexing. Usage: index <on|off>"; }

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