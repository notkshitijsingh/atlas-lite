package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

public class ExitCommand extends AbstractCommand {
    @Override
    public String getName() { return "exit"; }

    @Override
    public String getDescription() { return "Saves data and shuts down the shell."; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        System.out.println(" [SHUTDOWN] Saving shards...");
        engine.commit();
        System.out.println(" [SHUTDOWN] Goodbye.");
        System.exit(0);
    }
}