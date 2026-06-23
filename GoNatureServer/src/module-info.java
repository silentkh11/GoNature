module GoNatureServer {
    // Requirements
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires mail;
    requires activation;
    requires java.net.http;
    requires jdk.httpserver;
    // Exporting packages so JavaFX can launch the windows
    exports server;
    exports gui;
    exports database;

    // VERY IMPORTANT: Allow JavaFX to read the @FXML tags in your controller
    opens gui to javafx.fxml;
}