package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class MainMenuController {

    @FXML
    private Button guestBtn;

    @FXML
    private Button loginBtn;

    @FXML
    private Button registerBtn;

    @FXML
    void handleGuestBooking(ActionEvent event) {
        // Guests bypass login and go straight to creating an order
        switchScene(event, "/gui/CreateOrder.fxml", "GoNature - Guest Booking");
    }

    @FXML
    void handleEmployeeLogin(ActionEvent event) {
        // Employees are routed to the secure login screen
        switchScene(event, "/gui/Login.fxml", "GoNature - Employee Login");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        // Swap the root to the new public registration screen
        switchScene(event, "/gui/Register.fxml", "GoNature - Registration");
    }

    /**
     * Helper method to swap out the active JavaFX scene cleanly.
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // SWAP THE ROOT INSTEAD OF THE SCENE
            stage.getScene().setRoot(root); 
            stage.setTitle(title);
            
            // Remove stage.centerOnScreen() completely!
            
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not load FXML file -> " + fxmlPath);
            e.printStackTrace();
        }
    }
}