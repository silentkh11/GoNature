package client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads server connection settings from client.properties (same directory as the JAR / project root).
 * If the file is absent, defaults to 127.0.0.1:5555 (same-machine mode).
 *
 * To connect to a server on another computer, set:
 *   server.host=192.168.x.x   (the LAN IP shown in the server's console on startup)
 *   server.port=5555
 */
public class ClientConfig {

    private static String host;
    private static int    port;

    // Default values loaded from client.properties at class-load time.
    // The startup connection dialog may override these before any network call.
    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("client.properties")) {
            props.load(fis);
            System.out.println("ClientConfig: loaded client.properties");
        } catch (IOException e) {
            System.out.println("ClientConfig: client.properties not found — using 127.0.0.1:5555");
        }
        host = props.getProperty("server.host", "127.0.0.1").trim();
        int p = 5555;
        try {
            p = Integer.parseInt(props.getProperty("server.port", "5555").trim());
        } catch (NumberFormatException e) {
            System.err.println("ClientConfig: invalid server.port value — defaulting to 5555");
        }
        port = p;
        System.out.println("ClientConfig: server = " + host + ":" + port);
    }

    public static String getHost() { return host; }
    public static int    getPort() { return port; }

    /** Called by the startup connection dialog to override the values from client.properties. */
    public static void setHost(String h) { host = h; }
    public static void setPort(int   p) { port = p; }
}
