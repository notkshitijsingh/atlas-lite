package com.atlasdblite.commands;

import com.atlasdblite.engine.GraphEngine;
import com.atlasdblite.server.APIServer;

public class ServerCommand extends AbstractCommand {
    private static APIServer serverInstance; 

    @Override
    public String getName() { return "server"; }

    @Override
    public String getDescription() { return "Controls Web API. Usage: server <start|stop> [port]"; }

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
            serverInstance.stop();
            serverInstance = null;
            
        } else {
            printError("Unknown action: " + action);
        }
    }
}