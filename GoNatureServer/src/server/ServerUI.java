package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Loads the visual design from the gui package
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));
        
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("GoNature Server Configuration");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}