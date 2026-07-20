package org.freemind.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet implementation class AppServlet
 * Handles serving static resources and API endpoints for the FreeMind web application.
 */
public class AppServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AppServlet.class);
    private static final String RESOURCE_PATH = "/META-INF/resources";
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    
    static {
        // Common MIME types
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("json", "application/json; charset=utf-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("txt", "text/plain; charset=utf-8");
    }

    /**
     * Handles API requests
     */
    private void handleApiRequest(String path, HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        
        try {
            if ("/api/status".equals(path)) {
                // Return server status
                response.getWriter().write("{\"status\":\"ok\",\"websocketUrl\":\"/ws/mindmap\"}");
                
            } else if ("/api/save".equals(path)) {
                // Handle save request
                response.getWriter().write("{\"status\":\"success\"}");
                
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            logger.error("Error handling API request {}: {}", path, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"" + 
                e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
    
    /**
     * Checks if the given path points to a static resource that can be cached.
     */
    private boolean isStaticResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Consider these as static resources that can be cached
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".js") ||
               lowerPath.endsWith(".css") ||
               lowerPath.endsWith(".png") ||
               lowerPath.endsWith(".jpg") ||
               lowerPath.endsWith(".jpeg") ||
               lowerPath.endsWith(".gif") ||
               lowerPath.endsWith(".svg") ||
               lowerPath.endsWith(".ico") ||
               lowerPath.endsWith(".woff") ||
               lowerPath.endsWith(".woff2") ||
               lowerPath.endsWith(".ttf") ||
               lowerPath.endsWith(".eot") ||
               lowerPath.endsWith(".html");
    }

    private String getMimeType(String path) {
        if (path == null || path.isEmpty()) {
            return "application/octet-stream";
        }
        
        // Get the file extension
        String extension = "";
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            extension = path.substring(lastDot + 1).toLowerCase();
        }
        
        // Return the MIME type from our map, or default to octet-stream
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Default to index.html for root path
        if ("/".equals(path) || path.isEmpty()) {
            path = "/index.html";
        }
        
        // Don't process WebSocket requests here
        if (path.startsWith("/ws/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "WebSocket endpoint not found");
            return;
        }
        
        // Handle API endpoints
        if (path.startsWith("/api/")) {
            handleApiRequest(path, request, response);
            return;
        }
        
        // Serve static resources from classpath
        String resourcePath = RESOURCE_PATH + path;
        logger.debug("Serving resource: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.warn("Resource not found: {}", resourcePath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
                return;
            }
            
            // Set content type based on file extension
            String mimeType = getMimeType(path);
            response.setContentType(mimeType);
            
            // Set cache control headers for static resources
            if (isStaticResource(path)) {
                long cacheTime = 31556926; // 1 year in seconds
                response.setDateHeader("Expires", System.currentTimeMillis() + cacheTime * 1000L);
                response.setHeader("Cache-Control", "public, max-age=" + cacheTime);
            } else {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setDateHeader("Expires", 0);
            }
            
            // Copy the resource to the response output stream
            try (OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
        } catch (IOException e) {
            logger.error("Error serving resource {}: {}", resourcePath, e.getMessage(), e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Error reading resource: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Handle API requests
        if (path.startsWith("/api/")) {
            handleApiRequest(path, request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Not found\"}");
        }
    }
}
