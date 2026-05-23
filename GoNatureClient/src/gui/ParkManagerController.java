package gui;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.Park;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ParkManagerController {

	@FXML
	private Label welcomeLabel;

	// Current Stats Labels
	@FXML
	private Label lblParkName;
	@FXML
	private Label lblMaxCap;
	@FXML
	private Label lblCasualGap;
	@FXML
	private Label lblStayTime;

	// Input Fields
	@FXML
	private TextField txtNewMaxCap;
	@FXML
	private TextField txtNewCasualGap;
	@FXML
	private TextField txtNewStayTime;

	@FXML
	private Label statusLabel;
	@FXML
	private Button btnSubmit;

	private Employee currentUser;
	private Park currentPark;

	/**
	 * Called automatically by the Login screen right before swapping scenes.
	 */
	public void setUser(Employee user) {
		this.currentUser = user;
		welcomeLabel.setText("Manager: " + user.getFirstName() + " " + user.getLastName() + " | Authenticated");

		// As soon as we know who the user is, ask the server for their park's details!
		fetchParkData();
	}

	private void fetchParkData() {
		if (currentUser.getParkId() != null) {
			try {
				// Update the network callback specifically for this screen
				ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
				ChatClient.getInstance()
						.handleMessageFromClientUI(new Message("FETCH_PARK_INFO", currentUser.getParkId()));
			} catch (Exception e) {
				showStatus("Error communicating with server.", "#d63031");
			}
		}
	}

	@FXML
	void submitParameters(ActionEvent event) {
		// --- STRICT PROFESSIONAL VALIDATION ---
		try {
			int newMax = Integer.parseInt(txtNewMaxCap.getText().trim());
			int newGap = Integer.parseInt(txtNewCasualGap.getText().trim());
			int newTime = Integer.parseInt(txtNewStayTime.getText().trim());

			if (newMax <= 0 || newGap < 0 || newTime <= 0) {
				showStatus("Values must be positive numbers.", "#d63031");
				return;
			}
			if (newGap >= newMax) {
				showStatus("Casual gap cannot be greater than max capacity.", "#d63031");
				return;
			}

			// Create a temporary Park object to send the update
			Park updatedPark = new Park(currentPark.getParkId(), currentPark.getName(), newMax, newGap, newTime,
					currentPark.getCurrentVisitors());

			btnSubmit.setDisable(true);
			showStatus("Sending request...", "#0984e3");

			ChatClient.getInstance().handleMessageFromClientUI(new Message("UPDATE_PARK_PARAMS", updatedPark));

		} catch (NumberFormatException e) {
			showStatus("Please enter valid whole numbers only.", "#d63031");
		} catch (Exception e) {
			showStatus("Network error.", "#d63031");
		}
	}

	/**
	 * Handles incoming messages from the Server while this screen is active.
	 */
	public void handleServerResponse(Message msg) {
		Platform.runLater(() -> {
			switch (msg.getCommand()) {
			case "PARK_INFO_DATA":
				this.currentPark = (Park) msg.getData();
				lblParkName.setText(currentPark.getName());
				lblMaxCap.setText(String.valueOf(currentPark.getMaxCapacity()));
				lblCasualGap.setText(String.valueOf(currentPark.getCasualGap()));
				lblStayTime.setText(String.valueOf(currentPark.getEstimatedStayTime()));
				break;

			case "UPDATE_PARAMS_SUCCESS":
				showStatus("Parameters updated successfully!", "#00b894");
				btnSubmit.setDisable(false);
				// Refresh the left card with the new data
				txtNewMaxCap.clear();
				txtNewCasualGap.clear();
				txtNewStayTime.clear();
				fetchParkData();
				break;

			case "UPDATE_PARAMS_FAILED":
				showStatus((String) msg.getData(), "#d63031");
				btnSubmit.setDisable(false);
				break;
			}
		});
	}

	private void showStatus(String message, String hexColor) {
		statusLabel.setText(message);
		statusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
	}
}