package org.freemind.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI entry point. Port resolution: first program argument, then the
 * {@code freemind.web.port} system property, then 8080. Port 0 binds an
 * ephemeral port (printed on startup) — useful when 8080 is taken.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = resolvePort(args);
        WebServer server = new WebServer(port);
        try {
            server.start();
            logger.info("\n" +
                    "============================================================\n" +
                    "FreeMind Web is running!\n" +
                    "- Web UI:      http://localhost:{}\n" +
                    "- WebSocket:   ws://localhost:{}" + WebServer.WEBSOCKET_PATH + "\n" +
                    "============================================================\n" +
                    "Press Ctrl+C to stop the server...",
                    server.getPort(), server.getPort());
            server.join();
        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static int resolvePort(String[] args) {
        String value = args.length > 0 ? args[0]
                : System.getProperty("freemind.web.port");
        if (value == null) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid port '{}', using {}", value, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
