package gui;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.ReportData;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Map;

public class ParkManagerReportsController {

	// Fixed color palette keyed by category name so both pie and bar charts
	// always render the same color for the same visitor type.
	private static final java.util.Map<String, String> CAT_COLORS = new java.util.LinkedHashMap<>();
	static {
		CAT_COLORS.put("Solo",       "#41a9c9");
		CAT_COLORS.put("Group",      "#57b757");
		CAT_COLORS.put("Walk-in",    "#f3622d");
		CAT_COLORS.put("Regular",    "#fba71b");
		CAT_COLORS.put("Individual", "#9a5fb5");
		CAT_COLORS.put("Guide",      "#bf3829");
	}
	private static final String[] FALLBACK = {"#4db6ac","#81c784","#e57373","#ffb74d","#64b5f6","#ce93d8"};

	private String catColor(String name, int idx) {
		return CAT_COLORS.getOrDefault(name, FALLBACK[idx % FALLBACK.length]);
	}

	@FXML private ComboBox<String> monthCombo;
	@FXML private ComboBox<String> yearCombo;
	@FXML private PieChart visitorPieChart;
	@FXML private BarChart<String, Number> incomeBarChart;
	@FXML private Label lblTotalVisitors;
	@FXML private Label lblTotalIncome;
	@FXML private Label lblStatus;
	@FXML private Button themeBtn;
	@FXML private javafx.scene.control.ListView<String> usageList;
	@FXML private Label lblUsageSummary;

	private Employee currentUser;
	private ReportData currentLoadedReport;

	@FXML
	public void initialize() {
		themeBtn.setText(ThemeManager.getInstance().toggleLabel());
		ChatClient.getInstance().setResponseHandler(this::handleServerResponse);

		monthCombo.getItems().addAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
		yearCombo.getItems().addAll("2024", "2025", "2026", "2027");
		usageList.setPlaceholder(new Label("No data — click '📉 Usage Report' to load."));
	}

	// Safely carries the logged-in manager over from the previous screen
	public void setUser(Employee user) {
		this.currentUser = user;
	}

	@FXML
	void handleToggleTheme(ActionEvent event) {
		javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
		ThemeManager.getInstance().toggle(scene);
		themeBtn.setText(ThemeManager.getInstance().toggleLabel());
	}

	@FXML
	void handleGenerateUsageReport(ActionEvent event) {
		if (monthCombo.getValue() == null || yearCombo.getValue() == null) {
			showStatus("Please select a month and year first.", "#d63031");
			return;
		}
		if (currentUser == null || currentUser.getParkId() == null) {
			showStatus("Error: No park assigned.", "#d63031");
			return;
		}
		String[] requestData = { String.valueOf(currentUser.getParkId()), monthCombo.getValue(), yearCombo.getValue() };
		showStatus("Generating usage report...", "#0984e3");
		usageList.getItems().clear();
		lblUsageSummary.setText("Loading...");
		ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_USAGE_REPORT", requestData));
	}

	@FXML
	void handleGenerateReport(ActionEvent event) {
		if (monthCombo.getValue() == null || yearCombo.getValue() == null) {
			showStatus("Please select both a month and a year.", "#d63031");
			return;
		}
		if (currentUser == null || currentUser.getParkId() == null) {
			showStatus("Error: No park assigned.", "#d63031");
			return;
		}

		String[] requestData = { String.valueOf(currentUser.getParkId()), monthCombo.getValue(), yearCombo.getValue() };

		showStatus("Generating report from database...", "#0984e3");
		ChatClient.getInstance().handleMessageFromClientUI(new Message("GENERATE_MONTHLY_REPORT", requestData));
	}

	@FXML
	void handleGoBack(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ParkManagerDashboard.fxml"));
			Parent root = loader.load();

			// Pass the user back so the dashboard knows who they are!
			ParkManagerController controller = loader.getController();
			controller.setUser(currentUser);

			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			WindowChrome.setContent(stage, root, "GoNature - Park Manager");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            if (msg.getCommand().equals("REPORT_DATA_SUCCESS")) {
                ReportData data = (ReportData) msg.getData();
                this.currentLoadedReport = data;

                lblTotalVisitors.setText(String.valueOf(data.getTotalVisitors()));
                lblTotalIncome.setText(String.format("₪%.2f", data.getTotalIncome()));

                visitorPieChart.getData().clear();
                int[] pieIdx = {0};
                for (Map.Entry<String, Integer> entry : data.getVisitorBreakdown().entrySet()) {
                    PieChart.Data slice = new PieChart.Data(
                        entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
                    final String color = catColor(entry.getKey(), pieIdx[0]++);
                    slice.nodeProperty().addListener((obs, o, node) -> {
                        if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
                    });
                    visitorPieChart.getData().add(slice);
                }
                Platform.runLater(() -> {
                    int fi = 0;
                    for (PieChart.Data s : visitorPieChart.getData()) {
                        String rawName = s.getName().replaceAll(" \\(\\d+\\)", "").trim();
                        if (s.getNode() != null)
                            s.getNode().setStyle("-fx-pie-color: " + catColor(rawName, fi) + ";");
                        fi++;
                    }
                });

                incomeBarChart.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                int[] barIdx = {0};
                java.util.List<Map.Entry<String, Double>> entries =
                    new java.util.ArrayList<>(data.getIncomeBreakdown().entrySet());
                for (Map.Entry<String, Double> entry : entries) {
                    XYChart.Data<String, Number> bar = new XYChart.Data<>(entry.getKey(), entry.getValue());
                    series.getData().add(bar);
                    final String color = catColor(entry.getKey(), barIdx[0]++);
                    bar.nodeProperty().addListener((obs, o, node) -> {
                        if (node != null) node.setStyle("-fx-bar-fill: " + color + ";");
                    });
                }
                incomeBarChart.getData().add(series);

                showStatus("Report generated successfully.", "#00b894");

            } else if (msg.getCommand().equals("REPORT_DATA_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            } else if (msg.getCommand().equals("SAVE_REPORT_SUCCESS")) {
                showStatus((String) msg.getData(), "#00b894");
            } else if (msg.getCommand().equals("SAVE_REPORT_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            } else if (msg.getCommand().equals("USAGE_REPORT_DATA")) {
                @SuppressWarnings("unchecked")
                ArrayList<String[]> rows = (ArrayList<String[]>) msg.getData();
                usageList.getItems().clear();
                if (rows.isEmpty()) {
                    lblUsageSummary.setText("No below-capacity days found for this period.");
                    usageList.setPlaceholder(new Label("The park was at or above capacity on all active days this month."));
                } else {
                    for (String[] row : rows) {
                        String line = String.format("📅 %s   |   %s / %s visitors  (%s%% full)   |   %s spots available",
                            row[0], row[1], row[2], row[4], row[3]);
                        usageList.getItems().add(line);
                    }
                    lblUsageSummary.setText(rows.size() + " day(s) had below-capacity attendance.");
                    lblUsageSummary.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                }
                showStatus("Usage report loaded — " + rows.size() + " day(s) below capacity.", "#00b894");
            } 
            // --- WATCHDOG AUTO-LOGOUT ---
            else if (msg.getCommand().equals("SERVER_DISCONNECTED") || msg.getCommand().equals("KICKED")) {
                if(msg.getCommand().equals("SERVER_DISCONNECTED")) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Network Security Alert");
                    alert.setHeaderText("Server Connection Lost");
                    alert.setContentText("Connection to the server was lost. For security, you have been logged out.");
                    alert.showAndWait();
                }
                forceUIToMainMenu();
            }
        });
    }

    private void forceUIToMainMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) lblStatus.getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

	@FXML
	void handleSubmitReport(ActionEvent event) {
		if (currentLoadedReport == null) {
			showStatus("Please generate a report first before submitting.", "#d63031");
			return;
		}

		showStatus("Submitting report to Department Manager...", "#0984e3");
		ChatClient.getInstance().handleMessageFromClientUI(new Message("SAVE_MONTHLY_REPORT", currentLoadedReport));
	}

	private void showStatus(String message, String hexColor) {
		lblStatus.setText(message);
		lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
	}
}