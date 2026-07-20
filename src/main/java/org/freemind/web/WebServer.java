package org.freemind.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.freemind.web.websocket.MindMapHub;
import org.freemind.web.websocket.MindMapWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embeddable web server: static content + API via {@link AppServlet},
 * the collaboration WebSocket at {@value #WEBSOCKET_PATH}.
 *
 * Instance-scoped so tests can run several servers side by side:
 * each server owns its own {@link MindMapHub}, and port 0 binds an
 * ephemeral port reported by {@link #getPort()}.
 */
public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    public static final String WEBSOCKET_PATH = "/ws/mindmap";

    private final Server server;
    private final MindMapHub hub = new MindMapHub();

    public WebServer(int port) {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder("appServlet", new AppServlet()), "/*");

        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator((req, resp) -> new MindMapWebSocket(hub));
            }
        };
        ContextHandler wsContext = new ContextHandler(WEBSOCKET_PATH);
        wsContext.setHandler(wsHandler);

        HandlerList handlers = new HandlerList();
        handlers.addHandler(wsContext);
        handlers.addHandler(context);
        server.setHandler(handlers);

        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);
    }

    public void start() throws Exception {
        server.start();
        logger.info("FreeMind Web listening on port {}", getPort());
    }

    public void stop() throws Exception {
        server.stop();
    }

    /** Actual bound port — meaningful after start() when constructed with port 0. */
    public int getPort() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    /** Blocks until the server stops (Ctrl+C in interactive use). */
    public void join() throws InterruptedException {
        server.join();
    }
}
