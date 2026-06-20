package gui;

import client.ChatClient;
import entities.Message;
import entities.Subscriber;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField txtId;
    @FXML private TextField txtFirst;
    @FXML private TextField txtLast;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtCard;
    @FXML private TextField txtFamilySize;
    @FXML private CheckBox chkGuide;
    @FXML private Label lblStatus;
    @FXML private Button btnRegister;
    @FXML private Button themeBtn;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        try {
            // Safely create the connection if it doesn't exist, and set the listener
            ChatClient.getInstance("127.0.0.1", 5555, this::handleServerResponse);
        } catch (Exception e) {
            showStatus("Error: Cannot connect to the server.", "#d63031");
            btnRegister.setDisable(true);
            e.printStackTrace();
        }
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String id = txtId.getText().trim();
        String email = txtEmail.getText().trim();
        String sizeStr = txtFamilySize.getText().trim();

        if (id.isEmpty() || email.isEmpty() || txtFirst.getText().trim().isEmpty()) {
            showStatus("ID, Name, and Email are required.", "#d63031");
            return;
        }

        try {
            int familySize = sizeStr.isEmpty() ? 1 : Integer.parseInt(sizeStr);
            
            Subscriber newSub = new Subscriber(
                id, 
                txtFirst.getText().trim(), 
                txtLast.getText().trim(), 
                email, 
                txtPhone.getText().trim(), 
                familySize, 
                txtCard.getText().trim(), 
                chkGuide.isSelected()
            );

            btnRegister.setDisable(true);
            showStatus("Submitting registration...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("REGISTER_SUBSCRIBER_REQUEST", newSub));

        } catch (NumberFormatException e) {
            showStatus("Family size must be a valid number.", "#d63031");
        }
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            btnRegister.setDisable(false);
            
            if (msg.getCommand().equals("REGISTER_SUCCESS")) {
                showStatus("Registration complete! You can now book as a subscriber.", "#00b894");
                clearForm();
            } else if (msg.getCommand().equals("REGISTER_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }

    private void clearForm() {
        txtId.clear(); txtFirst.clear(); txtLast.clear();
        txtEmail.clear(); txtPhone.clear(); txtCard.clear();
        txtFamilySize.clear(); chkGuide.setSelected(false);
    }
}