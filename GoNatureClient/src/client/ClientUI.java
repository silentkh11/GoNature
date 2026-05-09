package client;

import javafx.application.Application;
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
        // Loads the visual design from the gui package
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ClientDashboard.fxml"));
        
        Scene scene = new Scene(root, 750, 650);
        primaryStage.setTitle("GoNature Client Management");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}