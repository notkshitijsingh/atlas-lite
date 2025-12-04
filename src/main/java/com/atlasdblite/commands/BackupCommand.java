package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Command to create a timestamped backup of the primary database file.
 * This provides a simple snapshot capability for disaster recovery.
 */
public class BackupCommand extends AbstractCommand {
    @Override
    public String getName() { return "backup"; }

    @Override
    public String getDescription() { return "Creates a snapshot of the encrypted DB."; }

    /**
     * Executes the backup process.
     * It copies the main database file (`atlas_data.enc`) to a new file
     * with a timestamp in its name (e.g., `backup_20231027_123000.enc`).
     *
     * @param args Command-line arguments (not used by this command).
     * @param engine The {@link GraphEngine} instance (not directly used, but required by the interface).
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        // In a sharded world, this command would need to be updated to handle multiple files.
        // For now, it assumes a single-file database for simplicity.
        File source = new File("atlas_data.enc");
        if (!source.exists()) {
            printError("No database file found to backup.");
            return;
        }

        // Generate a unique, timestamped filename for the backup.
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dest = new File("backup_" + timestamp + ".enc");

        try {
            // Perform the file copy.
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            printSuccess("Backup created: " + dest.getName());
        } catch (Exception e) {
            printError("Backup failed: " + e.getMessage());
        }
    }
}