package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;

/**
 * Manually flushes memory to disk and truncates the WAL.
 */
public class CheckpointCommand extends AbstractCommand {
    @Override
    public String getName() { return "checkpoint"; }

    @Override
    public String getDescription() { return "Flushes WAL to disk shards (checkpoint)."; }

    @Override
    public void execute(String[] args, GraphEngine engine) {
        long start = System.nanoTime();
        engine.checkpoint();
        long end = System.nanoTime();
        System.out.printf(" [OK] Checkpoint finished in %.2f ms%n", (end-start)/1e6);
    }
}