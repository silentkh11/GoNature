package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/MainMenu.fxml"));

        // Set a fixed window size here (e.g., 850x650) so the window never changes size!
        Scene scene = gui.WindowChrome.install(primaryStage, root, 850, 650, "GoNature - Welcome");
        gui.ThemeManager.getInstance().applyTo(scene);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Optional: Prevents the user from dragging the window size
        
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing client application...");
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
    }
}