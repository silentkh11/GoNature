package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for the GoNature server.
 * Loads the {@code ServerPort.fxml} configuration screen where the operator
 * enters the MySQL password and starts the OCSF TCP server.
 */
public class ServerUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Loads the visual design from the gui package
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));

        Scene scene = gui.WindowChrome.install(primaryStage, root, 850, 550, "GoNature Server Configuration");
        gui.ThemeManager.getInstance().applyTo(scene);

        // --- ADD THIS BLOCK ---
        // This forces the entire application and all background threads to close when the X is clicked.
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Shutting down GoNature server...");
            Platform.exit(); // Closes the JavaFX platform
            System.exit(0);  // Forcibly terminates the Java Virtual Machine and all background threads
        });
        // ----------------------

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}