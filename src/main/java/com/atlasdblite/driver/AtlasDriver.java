package com.atlasdblite.driver;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * AtlasDriver provides a programmatic interface to a running AtlasDB-Lite server.
 * It manages the socket connection and command protocol.
 */
public class AtlasDriver implements AutoCloseable {

    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;

    public AtlasDriver(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to the AtlasDB server.
     * Consumes the initial banner/welcome message.
     */
    public void connect() throws IOException {
        if (isConnected) return;

        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.isConnected = true;

        // Consume the initial banner and prompt (Read until "atlas> ")
        readResponse(); 
    }

    /**
     * Sends a raw string command to the database and returns the response.
     * @param command The command string (e.g., "add-node Person Alice")
     * @return The server's response text (excluding the prompt).
     */
    public String execute(String command) throws IOException {
        if (!isConnected) throw new IOException("Not connected to AtlasDB server.");

        out.println(command);
        return readResponse();
    }

    // --- Convenience Methods for Common Operations ---

    public String addNode(String label, String name) throws IOException {
        return execute(String.format("add-node %s %s", label, name));
    }

    public String link(String node1, String node2, String relation) throws IOException {
        return execute(String.format("link %s %s %s", node1, node2, relation));
    }

    public String query(String node) throws IOException {
        return execute(String.format("query %s", node));
    }

    public String path(String startNode, String endNode) throws IOException {
        return execute(String.format("path %s %s", startNode, endNode));
    }
    
    public String stats() throws IOException {
        return execute("stats");
    }

    /**
     * Reads the server stream line-by-line until the prompt "atlas> " is detected.
     */
    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        int charRead;
        StringBuilder buffer = new StringBuilder();

        // Read character by character to detect the prompt without waiting for a newline after it
        while ((charRead = in.read()) != -1) {
            char c = (char) charRead;
            buffer.append(c);
            
            // Check if the buffer ends with the prompt
            if (buffer.toString().endsWith("atlas> ")) {
                // Remove the prompt from the returned data
                String cleanOutput = buffer.substring(0, buffer.length() - "atlas> ".length());
                return cleanOutput.trim();
            }
        }
        return buffer.toString().trim();
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            // Try to exit gracefully
            try { out.println("exit"); } catch (Exception e) {} 
            socket.close();
        }
        isConnected = false;
    }
}