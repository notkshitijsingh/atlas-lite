package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

public class NukeCommand extends AbstractCommand {
    @Override
    public String getName() { return "nuke"; }

    @Override
    public String getDescription() { return "Wipes the entire database. Usage: nuke --confirm"; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (args.length < 2 || !args[1].equals("--confirm")) {
            printError("DANGER: This operation is irreversible.");
            printError("To proceed, type: nuke --confirm");
            return;
        }

        engine.wipeDatabase();
        printSuccess("DATABASE WIPED. All nodes and relations have been destroyed.");
    }
}