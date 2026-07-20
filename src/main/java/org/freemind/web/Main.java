package org.freemind.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.freemind.web.websocket.MindMapWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PORT = 8080;
    private static final String CONTEXT_PATH = "/";
    private static final String WEBSOCKET_PATH = "/ws/mindmap";

    public static void main(String[] args) {
        // Create server
        Server server = new Server(PORT);
        
        // Create context for static content
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(CONTEXT_PATH);
        
        // Add servlet for static content
        ServletHolder servletHolder = new ServletHolder("appServlet", new AppServlet());
        context.addServlet(servletHolder, "/*");
        
        // Create WebSocket handler
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                // Register our WebSocket class
                factory.register(MindMapWebSocket.class);
            }
        };
        
        // Set up the context for WebSocket
        ContextHandler wsContext = new ContextHandler();
        wsContext.setContextPath(WEBSOCKET_PATH);
        wsContext.setHandler(wsHandler);
        
        // Add all handlers to the server
        HandlerList handlers = new HandlerList();
        handlers.addHandler(wsContext);
        handlers.addHandler(context);
        server.setHandler(handlers);
        
        // Configure server
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);
        
        try {
            // Start the server
            server.start();
            
            logger.info("\n" +
                    "============================================================\n" +
                    "FreeMind Web is running!\n" +
                    "- Web UI:      http://localhost:{}\n" +
                    "- WebSocket:   ws://localhost:{}" + WEBSOCKET_PATH + "\n" +
                    "============================================================\n" +
                    "Press Ctrl+C to stop the server...",
                    PORT, PORT);
            
            // Keep the main thread alive
            server.join();
            
        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            try {
                if (server != null) {
                    server.stop();
                }
            } catch (Exception e) {
                logger.error("Error during server shutdown: {}", e.getMessage(), e);
            }
        }
    }
}
