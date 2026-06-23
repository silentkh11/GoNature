package gui.service;

import client.ChatClient;
import entities.Message;
import entities.Subscriber;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ServiceRepController {

    // --- Register section ---
    @FXML private TextField txtId;
    @FXML private TextField txtFirst;
    @FXML private TextField txtLast;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtCard;
    @FXML private TextField txtFamilySize;
    @FXML private CheckBox chkGuide;
    @FXML private Label lblStatus;
    @FXML private Label lblEmployeeName;
    @FXML private Button themeBtn;

    // --- Lookup / update section ---
    @FXML private TextField txtLookupId;
    @FXML private TextField txtEditFirst;
    @FXML private TextField txtEditLast;
    @FXML private TextField txtEditEmail;
    @FXML private TextField txtEditPhone;
    @FXML private TextField txtEditCard;
    @FXML private TextField txtEditFamilySize;
    @FXML private Label lblLookupStatus;

    private String currentLookedUpId = null;
    private entities.Employee currentUser = null;

    public void setUser(entities.Employee user) {
        this.currentUser = user;
        if (lblEmployeeName != null)
            lblEmployeeName.setText("👤 " + user.getFirstName() + " " + user.getLastName());
    }

    @FXML
    void handleViewMyProfile(javafx.event.ActionEvent event) {
        if (currentUser == null) return;
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("My Profile");
        alert.setHeaderText("Employee Information");
        alert.setContentText(
            "Name:        " + currentUser.getFirstName() + " " + currentUser.getLastName() + "\n" +
            "Employee ID:  " + currentUser.getEmployeeId() + "\n" +
            "Email:        " + (currentUser.getEmail() != null ? currentUser.getEmail() : "—") + "\n" +
            "Role:         " + currentUser.getRole()
        );
        alert.showAndWait();
    }

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
    void handleLogout(ActionEvent event) {
        try {
            ChatClient.getInstance().handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
        } catch (Exception ignored) {}
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/guest/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @FXML
    void handleLookup(ActionEvent event) {
        String id = txtLookupId.getText().trim();
        if (id.isEmpty()) {
            showLookupStatus("Please enter a subscriber ID to look up.", "#d63031");
            return;
        }
        showLookupStatus("Searching...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_SUBSCRIBER_REQUEST", id));
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        if (currentLookedUpId == null) {
            showLookupStatus("Look up a subscriber first before updating.", "#d63031");
            return;
        }
        String sizeStr = txtEditFamilySize.getText().trim();
        try {
            int familySize = sizeStr.isEmpty() ? 1 : Integer.parseInt(sizeStr);
            Subscriber updated = new Subscriber(
                currentLookedUpId,
                txtEditFirst.getText().trim(),
                txtEditLast.getText().trim(),
                txtEditEmail.getText().trim(),
                txtEditPhone.getText().trim(),
                familySize,
                txtEditCard.getText().trim(),
                false
            );
            showLookupStatus("Saving changes...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("UPDATE_SUBSCRIBER_REQUEST", updated));
        } catch (NumberFormatException e) {
            showLookupStatus("Family size must be a valid number.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                case "REGISTER_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894");
                    txtId.clear(); txtFirst.clear(); txtLast.clear();
                    txtEmail.clear(); txtPhone.clear(); txtCard.clear();
                    txtFamilySize.clear(); chkGuide.setSelected(false);
                    break;

                case "REGISTER_FAILED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;

                case "SUBSCRIBER_DATA": {
                    Subscriber sub = (Subscriber) msg.getData();
                    currentLookedUpId = sub.getVisitorId();
                    txtEditFirst.setText(sub.getFirstName());
                    txtEditLast.setText(sub.getLastName());
                    txtEditEmail.setText(sub.getEmail());
                    txtEditPhone.setText(sub.getPhone());
                    txtEditCard.setText(sub.getCreditCard() != null ? sub.getCreditCard() : "");
                    txtEditFamilySize.setText(String.valueOf(sub.getFamilySize()));
                    showLookupStatus("Subscriber found: " + sub.getFirstName() + " " + sub.getLastName()
                        + " | Family size: " + sub.getFamilySize()
                        + (sub.isGuide() ? " | Certified Guide" : ""), "#00b894");
                    break;
                }

                case "SUBSCRIBER_NOT_FOUND":
                    currentLookedUpId = null;
                    showLookupStatus((String) msg.getData(), "#d63031");
                    break;

                case "UPDATE_SUBSCRIBER_SUCCESS":
                    showLookupStatus((String) msg.getData(), "#00b894");
                    break;

                case "UPDATE_SUBSCRIBER_FAILED":
                    showLookupStatus((String) msg.getData(), "#d63031");
                    break;

                case "SERVER_DISCONNECTED":
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Network Security Alert");
                    alert.setHeaderText("Server Connection Lost");
                    alert.setContentText("Connection to the server was lost. For security, you have been logged out.");
                    alert.showAndWait();
                    forceUIToMainMenu();
                    break;

                case "KICKED":
                    forceUIToMainMenu();
                    break;
            }
        });
    }

    private void forceUIToMainMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/guest/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) lblStatus.getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }

    private void showLookupStatus(String message, String hexColor) {
        if (lblLookupStatus != null) {
            lblLookupStatus.setText(message);
            lblLookupStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
        }
    }
}
