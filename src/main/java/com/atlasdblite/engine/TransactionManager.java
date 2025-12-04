package com.atlasdblite.engine;

import com.atlasdblite.security.CryptoManager;
import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Global Write-Ahead Log (WAL).
 * Ensures ACID properties by recording operations before they are applied to memory.
 */
public class TransactionManager {
    private static final String WAL_FILE = "atlas_db/global.wal";
    private final CryptoManager crypto;
    private final Gson gson;
    private PrintWriter writer;

    public TransactionManager(CryptoManager crypto) {
        this.crypto = crypto;
        this.gson = new Gson();
        initialize();
    }

    private void initialize() {
        try {
            File wal = new File(WAL_FILE);
            if (!wal.exists()) {
                if (wal.getParentFile() != null) wal.getParentFile().mkdirs();
                wal.createNewFile();
            }
            // Append mode, Auto-flush enabled
            this.writer = new PrintWriter(new FileWriter(wal, true), true);
        } catch (IOException e) {
            throw new RuntimeException("CRITICAL: Could not open WAL. " + e.getMessage());
        }
    }

    // --- Logging Primitives ---

    public synchronized void writeEntry(WalEntry entry) {
        try {
            String json = gson.toJson(entry);
            String encrypted = crypto.encrypt(json);
            writer.println(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("WAL Write Failed: " + e.getMessage());
        }
    }

    public synchronized void clearLog() {
        try {
            writer.close();
            // Truncate file
            new FileOutputStream(WAL_FILE).close(); 
            // Re-open
            this.writer = new PrintWriter(new FileWriter(WAL_FILE, true), true);
        } catch (IOException e) {
            System.err.println("Failed to truncate WAL: " + e.getMessage());
        }
    }

    // --- Recovery Logic ---

    public List<WalEntry> readLog() {
        List<WalEntry> entries = new ArrayList<>();
        File wal = new File(WAL_FILE);
        if (!wal.exists()) return entries;

        try (BufferedReader br = new BufferedReader(new FileReader(wal))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String json = crypto.decrypt(line);
                    WalEntry entry = gson.fromJson(json, WalEntry.class);
                    entries.add(entry);
                } catch (Exception e) {
                    System.err.println(" [WAL] Corrupt entry ignored.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;
    }

    // --- DTO for Log Entries ---
    public static class WalEntry {
        public long timestamp;
        public String operation; // ADD_NODE, DELETE_LINK, etc.
        public String payload;   // JSON representation of the object or ID

        public WalEntry(String op, String data) {
            this.timestamp = System.currentTimeMillis();
            this.operation = op;
            this.payload = data;
        }
    }
}
