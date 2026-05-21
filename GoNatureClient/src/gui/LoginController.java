package gui;

import client.ChatClient;
import entities.Message;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

	@FXML
	private TextField usernameField;
	@FXML
	private PasswordField passwordField;
	@FXML
	private Button loginBtn;
	@FXML
	private Label errorLabel;

	@FXML
	public void initialize() {
		try {
			// 1. Connect to the server immediately when the login screen opens.
			// (Assuming server is running on the same machine for now: "localhost")
			client.ChatClient.getInstance("127.0.0.1", 5555, (Message msg) -> {
				// We don't need Platform.runLater here because handleServerResponse
				// already has it built-in from our previous code!
				handleServerResponse(msg);
			});
			System.out.println("Connected to GoNature Server.");
		} catch (Exception e) {
			showError("Cannot connect to server at 127.0.0.1:5555");
			loginBtn.setDisable(true);
			e.printStackTrace();
		}
	}

	@FXML
	void handleLogin(ActionEvent event) {
		String username = usernameField.getText().trim();
		String password = passwordField.getText().trim();

		// 1. Basic empty field validation
		if (username.isEmpty() || password.isEmpty()) {
			showError("Please enter both username and password.");
			return;
		}

		// 2. Prepare UI for loading
		errorLabel.setVisible(false);
		loginBtn.setDisable(true);
		loginBtn.setText("Authenticating...");

		// 3. Package and send the network request
		String[] credentials = { username, password };
		Message loginMessage = new Message("LOGIN_REQUEST", credentials);

		try {
			// Assuming your ChatClient has a standard way to send messages to the server
			ChatClient.getInstance().handleMessageFromClientUI(loginMessage);
		} catch (Exception e) {
			showError("Cannot connect to server. Is it running?");
			resetLoginButton();
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

			if (msg.getCommand().equals("LOGIN_SUCCESS")) {

				// 1. Extract the user object the Server sent us
				entities.Employee user = (entities.Employee) msg.getData();

				try {
					// 2. Load the new FXML file
					javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
							getClass().getResource("/gui/ParkManagerDashboard.fxml"));
					javafx.scene.Parent root = loader.load();

					// 3. Pass the user data into the new controller!
					ParkManagerController pmController = loader.getController();
					pmController.setUser(user);

					// 4. Grab the current window and swap the scene
					javafx.stage.Stage stage = (javafx.stage.Stage) loginBtn.getScene().getWindow();
					stage.setScene(new javafx.scene.Scene(root, 800, 600));
					stage.setTitle("GoNature - Park Manager Dashboard");
					stage.centerOnScreen();

				} catch (Exception e) {
					showError("Error loading dashboard screen.");
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
}