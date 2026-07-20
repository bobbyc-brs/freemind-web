# FreeMind Web Interface

This is a web-based interface for FreeMind, built using modern web technologies. It provides a responsive, interactive mind mapping experience in the browser.

## Features

- Real-time collaboration using WebSockets
- Responsive design that works on desktop and tablet devices
- Interactive mind map with zoom and pan capabilities
- Node creation, editing, and deletion
- Import/Export functionality for FreeMind (.mm) files

## Prerequisites

- Java 8 or later
- Maven 3.6 or later
- Modern web browser (Chrome, Firefox, Safari, Edge)

## Getting Started

### Running the Application

1. Build the project:
   ```bash
   mvn clean package
   ```

2. Deploy the WAR file to your Java application server (e.g., Tomcat, Jetty, WildFly)

   Or run it directly with the Jetty Maven plugin:
   ```bash
   mvn jetty:run
   ```

3. Open your browser and navigate to: `http://localhost:8080`

### Development

To run the application in development mode with automatic reloading:

```bash
mvn jetty:run -Pdev
```

## Project Structure

- `src/main/java/org/freemind/web/` - Java server-side code
  - `WebSocketServer.java` - Handles WebSocket connections
  - `AppServlet.java` - Main servlet for HTTP requests
- `src/main/webapp/` - Frontend resources
  - `index.html` - Main application page
  - `css/` - Stylesheets
  - `js/` - JavaScript files
    - `websocket-client.js` - WebSocket client implementation
    - `mindmap-renderer.js` - Canvas-based mind map rendering
    - `app.js` - Main application logic

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [Java WebSockets](https://javaee.github.io/websocket-spec/)
- Inspired by [FreeMind](http://freemind.sourceforge.net/)
