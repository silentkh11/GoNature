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

    private static final String HOST;
    private static final int    PORT;

    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("client.properties")) {
            props.load(fis);
            System.out.println("ClientConfig: loaded client.properties");
        } catch (IOException e) {
            System.out.println("ClientConfig: client.properties not found — using 127.0.0.1:5555");
        }
        HOST = props.getProperty("server.host", "127.0.0.1").trim();
        int port = 5555;
        try {
            port = Integer.parseInt(props.getProperty("server.port", "5555").trim());
        } catch (NumberFormatException e) {
            System.err.println("ClientConfig: invalid server.port value — defaulting to 5555");
        }
        PORT = port;
        System.out.println("ClientConfig: server = " + HOST + ":" + PORT);
    }

    public static String getHost() { return HOST; }
    public static int    getPort() { return PORT; }
}
