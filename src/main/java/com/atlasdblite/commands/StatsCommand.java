package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import java.io.File;

/**
 * Command to display high-level statistics about the database.
 * This includes node count, relation count, and total disk space usage.
 */
public class StatsCommand extends AbstractCommand {
    @Override
    public String getName() { return "stats"; }

    @Override
    public String getDescription() { return "Displays database statistics and storage info."; }

    /**
     * Executes the command to gather and display database statistics.
     * Note: This can be a slow operation on large databases as it requires loading all segments
     * to get accurate node and relation counts.
     *
     * @param args Command-line arguments (not used by this command).
     * @param engine The {@link GraphEngine} instance to gather stats from.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        int nodes = engine.getAllNodes().size();
        int edges = engine.getAllRelations().size();
        
        // This is a simplification. In a real sharded DB, we'd sum the size of all segment files.
        File dbFile = new File("atlas_data.enc"); 
        long size = dbFile.exists() ? dbFile.length() : 0;

        System.out.println("--- AtlasDB-Lite Statistics ---");
        System.out.printf(" Nodes       : %d%n", nodes);
        System.out.printf(" Relations   : %d%n", edges);
        System.out.printf(" Storage     : %d bytes (Encrypted)%n", size);
        System.out.printf(" Shards      : %d%n", engine.getSegmentCount());
        System.out.printf(" Max Active  : %d%n", engine.getMaxActiveSegments());
        System.out.printf(" Indexing    : %s%n", engine.isAutoIndexing() ? "ON" : "OFF");
        System.out.println("-------------------------------");
    }
}