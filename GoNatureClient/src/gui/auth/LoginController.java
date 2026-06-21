package gui.auth;

import client.ChatClient;
import entities.Message;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import gui.gate.ParkEntranceController;
import gui.management.ParkManagerController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

	@FXML private TextField usernameField;
	@FXML private PasswordField passwordField;
	@FXML private Button loginBtn;
	@FXML private Label errorLabel;
	@FXML private Button themeBtn;

	@FXML
	public void initialize() {
		themeBtn.setText(ThemeManager.getInstance().toggleLabel());
		// Connection is attempted lazily on Login click, not here.
		// This lets the user open the screen, start the server, then login
		// without having to navigate away and back.
	}

	@FXML
	void handleToggleTheme(ActionEvent event) {
		javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
		ThemeManager.getInstance().toggle(scene);
		themeBtn.setText(ThemeManager.getInstance().toggleLabel());
	}

	@FXML
	void handleLogin(ActionEvent event) {
		String username = usernameField.getText().trim();
		String password = passwordField.getText().trim();

		if (username.isEmpty() || password.isEmpty()) {
			showError("Please enter both username and password.");
			return;
		}

		errorLabel.setVisible(false);
		loginBtn.setDisable(true);
		loginBtn.setText("Connecting...");

		// Connect (or reconnect) here — handles the case where the server
		// wasn't running when this screen opened.
		try {
			ChatClient.getInstance("127.0.0.1", 5555, this::handleServerResponse);
		} catch (Exception e) {
			showError("Cannot reach server. Please make sure it is running.");
			resetLoginButton();
			return;
		}

		loginBtn.setText("Authenticating...");

		String[] credentials = { username, password };
		try {
			ChatClient.getInstance().handleMessageFromClientUI(new Message("LOGIN_REQUEST", credentials));
		} catch (Exception e) {
			showError("Connection lost while sending. Try again.");
			resetLoginButton();
		}
	}
	
	@FXML
	void handleGoBack(ActionEvent event) {
		try {
			// Swap the root back to the Main Menu cleanly
			javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/guest/MainMenu.fxml"));
			javafx.scene.Parent root = loader.load();
			
			javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
			WindowChrome.setContent(stage, root, "GoNature - Welcome");
			
		} catch (Exception e) {
			System.err.println("Error returning to Main Menu.");
			e.printStackTrace();
		}
	}

	/**
	 * Call this method from your ChatClient when the server responds with either
	 * LOGIN_SUCCESS or LOGIN_FAILED.
	 */
	public void handleServerResponse(Message msg) {
		// We use Platform.runLater because we are changing the JavaFX UI
		// from a background network thread.
		Platform.runLater(() -> {
			resetLoginButton();

			// --- WATCHDOG AUTO-LOGOUT ---
			if (msg.getCommand().equals("SERVER_DISCONNECTED")) {
				javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
					javafx.scene.control.Alert.AlertType.ERROR);
				alert.setTitle("Network Security Alert");
				alert.setHeaderText("Server Connection Lost");
				alert.setContentText("Connection to the server was lost. Returning to the main menu.");
				alert.showAndWait();
				forceUIToMainMenu();
				return;
			}

			if (msg.getCommand().equals("LOGIN_SUCCESS")) {

				// 1. Extract the user object the Server sent us
				entities.Employee user = (entities.Employee) msg.getData();

				String targetFxml = "";
				String windowTitle = "";

				// 2. The Traffic Director (Check the role!)
				switch (user.getRole()) {
				case "ParkManager":
					targetFxml = "/gui/management/ParkManagerDashboard.fxml";
					windowTitle = "GoNature - Park Manager";
					break;
				case "GateWorker":
					targetFxml = "/gui/gate/parkEntrance.fxml";
					windowTitle = "GoNature - Park Gate Scanner";
					break;
				case "DeptManager":
                    targetFxml = "/gui/management/DeptManagerDashboard.fxml";
                    windowTitle = "GoNature - Department Manager";
                    break;
				case "ServiceRep":
                    targetFxml = "/gui/service/ServiceRepDashboard.fxml";
                    windowTitle = "GoNature - Service Representative";
                    break;
				default:
					showError("Error: Unknown employee role.");
					return;
				}

				// 3. Load the designated screen
				try {
					javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(targetFxml));
					javafx.scene.Parent root = loader.load();

					if (user.getRole().equals("ParkManager")) {
						ParkManagerController pmController = loader.getController();
						pmController.setUser(user);
					}
					if (user.getRole().equals("GateWorker")) {
						ParkEntranceController gateController = loader.getController();
						gateController.setUser(user);
					}

					// 4. Grab the current window and swap the ROOT smoothly
					javafx.stage.Stage stage = (javafx.stage.Stage) loginBtn.getScene().getWindow();

                    WindowChrome.setContent(stage, root, windowTitle); // Swap the content without resizing!
                    
                    // Removed stage.centerOnScreen() here as well!

				} catch (Exception e) {
					showError("Error loading " + user.getRole() + " screen.");
					e.printStackTrace();
				}

			} else if (msg.getCommand().equals("LOGIN_FAILED")) {
				String errorMsg = (String) msg.getData();
				showError(errorMsg);
			}
		});
	}

	private void showError(String message) {
		errorLabel.setText(message);
		errorLabel.setVisible(true);
	}

	private void resetLoginButton() {
		loginBtn.setDisable(false);
		loginBtn.setText("Login");
	}

	private void forceUIToMainMenu() {
		try {
			javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
				getClass().getResource("/gui/MainMenu.fxml"));
			javafx.scene.Parent root = loader.load();
			javafx.stage.Stage stage = (javafx.stage.Stage) loginBtn.getScene().getWindow();
			WindowChrome.setContent(stage, root, "GoNature - Welcome");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}