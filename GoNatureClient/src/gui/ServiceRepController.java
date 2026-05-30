package gui;

import client.ChatClient;
import entities.Message;
import entities.Subscriber;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ServiceRepController {

    @FXML private TextField txtId;
    @FXML private TextField txtFirst;
    @FXML private TextField txtLast;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtCard;
    @FXML private TextField txtFamilySize;
    @FXML private CheckBox chkGuide;
    @FXML private Label lblStatus;
    @FXML private Button themeBtn;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String id = txtId.getText().trim();
        String email = txtEmail.getText().trim();
        String sizeStr = txtFamilySize.getText().trim();

        if (id.isEmpty() || email.isEmpty()) {
            showStatus("ID and Email are mandatory fields.", "#d63031");
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

            showStatus("Processing registration...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("REGISTER_SUBSCRIBER_REQUEST", newSub));

        } catch (NumberFormatException e) {
            showStatus("Family size must be a valid number.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            if (msg.getCommand().equals("REGISTER_SUCCESS")) {
                showStatus((String) msg.getData(), "#00b894"); // Green success
                
                // Clear the form for the next customer
                txtId.clear(); txtFirst.clear(); txtLast.clear();
                txtEmail.clear(); txtPhone.clear(); txtCard.clear();
                txtFamilySize.clear(); chkGuide.setSelected(false);
                
            } else if (msg.getCommand().equals("REGISTER_FAILED")) {
                showStatus((String) msg.getData(), "#d63031"); // Red error
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}