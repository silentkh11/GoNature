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

/**
 * FXML controller for the login screen.
 * Sends a {@code LOGIN_REQUEST} to the server with the entered credentials;
 * on success it navigates to the appropriate role-specific dashboard.
 */
public class LoginController {

	@FXML private TextField usernameField;
	@FXML private PasswordField passwordField;
	@FXML private Button loginBtn;
	@FXML private Label errorLabel;
	@FXML private Button themeBtn;

	@FXML
	public void initialize() {
		themeBtn.setText(ThemeManager.getInstance().toggleLabel());

		// If an employee already has an active session (they used "← Main Menu"
		// without logging out), skip the form and go straight to their dashboard.
		entities.Employee active = ChatClient.getLoggedInEmployee();
		if (active != null) {
			javafx.application.Platform.runLater(() -> navigateToDashboard(active));
			return;
		}

		javafx.application.Platform.runLater(() -> usernameField.requestFocus());
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
			ChatClient.getInstance(client.ClientConfig.getHost(), client.ClientConfig.getPort(), this::handleServerResponse);
		} catch (Exception e) {
			showError("Cannot reach server at " + client.ClientConfig.getHost() + ":" + client.ClientConfig.getPort() + ". Please make sure it is running.");
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
				entities.Employee user = (entities.Employee) msg.getData();
				ChatClient.setLoggedInEmployee(user);
				navigateToDashboard(user);

			} else if (msg.getCommand().equals("LOGIN_FAILED")) {
				String errorMsg = (String) msg.getData();
				showError(errorMsg);
			}
		});
	}

	private void navigateToDashboard(entities.Employee user) {
		String targetFxml;
		String windowTitle;
		switch (user.getRole()) {
			case "ParkManager":
				targetFxml = "/gui/management/ParkManagerDashboard.fxml";
				windowTitle = "GoNature - Park Manager";
				break;
			case "GateWorker":
				targetFxml = "/gui/gate/ParkEntrance.fxml";
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
				if (errorLabel != null) showError("Error: Unknown employee role.");
				return;
		}
		try {
			javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(targetFxml));
			javafx.scene.Parent root = loader.load();

			switch (user.getRole()) {
				case "ParkManager":
					((ParkManagerController) loader.getController()).setUser(user);
					break;
				case "GateWorker":
					((ParkEntranceController) loader.getController()).setUser(user);
					break;
				case "DeptManager":
					((gui.management.DeptManagerController) loader.getController()).setUser(user);
					break;
				case "ServiceRep":
					((gui.service.ServiceRepController) loader.getController()).setUser(user);
					break;
			}

			// Find the stage: prefer loginBtn's scene (login form path),
			// fall back to any visible window (session-restore path from initialize()).
			javafx.stage.Stage stage = null;
			if (loginBtn != null && loginBtn.getScene() != null)
				stage = (javafx.stage.Stage) loginBtn.getScene().getWindow();
			if (stage == null)
				stage = (javafx.stage.Stage) javafx.stage.Window.getWindows()
						.stream().filter(javafx.stage.Window::isShowing).findFirst().orElse(null);
			if (stage != null)
				WindowChrome.setContent(stage, root, windowTitle);
		} catch (Exception e) {
			if (errorLabel != null) showError("Error loading " + user.getRole() + " screen.");
			e.printStackTrace();
		}
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
				getClass().getResource("/gui/guest/MainMenu.fxml"));
			javafx.scene.Parent root = loader.load();
			javafx.stage.Stage stage = (javafx.stage.Stage) loginBtn.getScene().getWindow();
			WindowChrome.setContent(stage, root, "GoNature - Welcome");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}