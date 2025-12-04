package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.server.APIServer;

/**
 * Command to control the embedded web API server.
 * It allows the user to start and stop the server from the interactive shell.
 */
public class ServerCommand extends AbstractCommand {
    /** A static reference to the server instance to ensure only one is running at a time. */
    private static APIServer serverInstance; 

    @Override
    public String getName() { return "server"; }

    @Override
    public String getDescription() { return "Controls Web API. Usage: server <start|stop> [port]"; }

    /**
     * Executes the server start or stop action.
     *
     * @param args Command arguments, expecting "start" or "stop" and an optional port number.
     * @param engine The {@link GraphEngine} instance that the server will use to query data.
     */
    @Override
    public void execute(String[] args, GraphEngine engine) {
        if (args.length < 2) {
            printError("Usage: server <start|stop> [port]");
            return;
        }

        String action = args[1];

        if ("start".equalsIgnoreCase(action)) {
            if (serverInstance != null) {
                printError("Server is already running.");
                return;
            }
            // Use default port 8080 unless a custom one is provided.
            int port = 8080; 
            if (args.length > 2) {
                try {
                    port = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    printError("Invalid port number.");
                    return;
                }
            }
            
            try {
                // Create and start the server instance.
                serverInstance = new APIServer(engine);
                serverInstance.start(port);
            } catch (Exception e) {
                printError("Failed to start server: " + e.getMessage());
            }

        } else if ("stop".equalsIgnoreCase(action)) {
            if (serverInstance == null) {
                printError("Server is not running.");
                return;
            }
            // Stop the server and clear the instance reference.
            serverInstance.stop();
            serverInstance = null;
            printSuccess("Server stopped.");
            
        } else {
            printError("Unknown action: " + action + ". Use 'start' or 'stop'.");
        }
    }
}