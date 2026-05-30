package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class MainMenuController {

    @FXML private Button guestBtn;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Button manageOrdersBtn;
    @FXML private Button themeBtn;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleGuestBooking(ActionEvent event) {
        switchScene(event, "/gui/CreateOrder.fxml", "GoNature - Guest Booking");
    }

    @FXML
    void handleEmployeeLogin(ActionEvent event) {
        switchScene(event, "/gui/Login.fxml", "GoNature - Employee Login");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        switchScene(event, "/gui/Register.fxml", "GoNature - Registration");
    }

    // --- 2. Added the routing method for the Guest Portal ---
    @FXML
    void handleManageOrders(ActionEvent event) {
        switchScene(event, "/gui/GuestPortal.fxml", "GoNature - Manage Orders");
    }

    /**
     * Helper method to swap out the active JavaFX scene cleanly.
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            stage.getScene().setRoot(root); 
            stage.setTitle(title);
            
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not load FXML file -> " + fxmlPath);
            e.printStackTrace();
        }
    }
}