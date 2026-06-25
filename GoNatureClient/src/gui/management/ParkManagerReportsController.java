package gui.management;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.Park;
import entities.ReportData;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

/**
 * FXML controller for the Park Manager reports screen.
 * Shows monthly visitor counts (bar chart) and income breakdown (pie chart)
 * for the park the manager is assigned to.
 */
public class ParkManagerReportsController {

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

	@FXML private ComboBox<String> parkCombo;
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
	private final java.util.HashMap<String, Integer> parkNameToId = new java.util.HashMap<>();

	@FXML
	public void initialize() {
		themeBtn.setText(ThemeManager.getInstance().toggleLabel());
		ChatClient.getInstance().setResponseHandler(this::handleServerResponse);

		monthCombo.getItems().addAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
		yearCombo.getItems().addAll("2024", "2025", "2026", "2027");
		usageList.setPlaceholder(new Label("No data — click 'Usage Report' to load."));

		// Load all parks so the dropdown can be populated
		ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
	}

	public void setUser(Employee user) {
		this.currentUser = user;
		// If parks are already loaded by the time setUser is called, pre-select now
		if (!parkNameToId.isEmpty()) preselectAssignedPark();
	}

	/** Selects the manager's assigned park in the combo, if it is present. */
	private void preselectAssignedPark() {
		if (currentUser == null || currentUser.getParkId() == null) return;
		for (Map.Entry<String, Integer> entry : parkNameToId.entrySet()) {
			if (entry.getValue().equals(currentUser.getParkId())) {
				parkCombo.setValue(entry.getKey());
				return;
			}
		}
	}

	/** Returns the park ID currently selected in the combo, or falls back to the user's assigned park. */
	private Integer getSelectedParkId() {
		String selected = parkCombo.getValue();
		if (selected != null && parkNameToId.containsKey(selected))
			return parkNameToId.get(selected);
		// Fallback: use the park assigned to this manager
		return (currentUser != null) ? currentUser.getParkId() : null;
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
		Integer parkId = getSelectedParkId();
		if (parkId == null) {
			showStatus("Please select a park.", "#d63031");
			return;
		}
		String[] requestData = { String.valueOf(parkId), monthCombo.getValue(), yearCombo.getValue() };
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
		Integer parkId = getSelectedParkId();
		if (parkId == null) {
			showStatus("Please select a park.", "#d63031");
			return;
		}
		String[] requestData = { String.valueOf(parkId), monthCombo.getValue(), yearCombo.getValue() };
		showStatus("Generating report from database...", "#0984e3");
		ChatClient.getInstance().handleMessageFromClientUI(new Message("GENERATE_MONTHLY_REPORT", requestData));
	}

	@FXML
	void handleGoBack(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/management/ParkManagerDashboard.fxml"));
			Parent root = loader.load();
			ParkManagerController controller = loader.getController();
			controller.setUser(currentUser);
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			WindowChrome.setContent(stage, root, "GoNature - Park Manager");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {

                case "ALL_PARKS_DATA": {
                    ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                    parkNameToId.clear();
                    for (Park p : parks) {
                        String label = p.getParkId() + " - " + p.getName();
                        parkNameToId.put(label, p.getParkId());
                    }
                    parkCombo.setItems(FXCollections.observableArrayList(parkNameToId.keySet()
                        .stream().sorted().toList()));
                    // Pre-select the manager's assigned park once list is ready
                    preselectAssignedPark();
                    break;
                }

                case "REPORT_DATA_SUCCESS": {
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
                    for (Map.Entry<String, Double> entry : data.getIncomeBreakdown().entrySet()) {
                        XYChart.Data<String, Number> bar = new XYChart.Data<>(entry.getKey(), entry.getValue());
                        series.getData().add(bar);
                        final String color = catColor(entry.getKey(), barIdx[0]++);
                        bar.nodeProperty().addListener((obs, o, node) -> {
                            if (node != null) node.setStyle("-fx-bar-fill: " + color + ";");
                        });
                    }
                    incomeBarChart.getData().add(series);
                    showStatus("Report generated successfully.", "#00b894");
                    break;
                }

                case "REPORT_DATA_FAILED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;

                case "SAVE_REPORT_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894");
                    break;

                case "SAVE_REPORT_FAILED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;

                case "USAGE_REPORT_DATA": {
                    ArrayList<String[]> rows = (ArrayList<String[]>) msg.getData();
                    usageList.getItems().clear();
                    if (rows.isEmpty()) {
                        lblUsageSummary.setText("No activity recorded for this period.");
                        usageList.setPlaceholder(new Label("No visit data found for this park and month."));
                    } else {
                        long fullDays = rows.stream().filter(r -> r.length > 5 && "FULL".equals(r[5])).count();
                        long freeDays = rows.size() - fullDays;
                        for (String[] row : rows) {
                            boolean isFull = row.length > 5 && "FULL".equals(row[5]);
                            String icon = isFull ? "🔴" : "🟢";
                            String label = isFull ? "FULL" : row[3] + " spots free";
                            String line = String.format("%s %s   |   %s / %s visitors  (%s%%)   |   %s",
                                icon, row[0], row[1], row[2], row[4], label);
                            usageList.getItems().add(line);
                        }
                        lblUsageSummary.setText("🔴 " + fullDays + " full day(s)   |   🟢 " + freeDays + " below-capacity day(s)");
                        lblUsageSummary.setStyle("-fx-text-fill: #e6edf3; -fx-font-weight: bold;");
                    }
                    showStatus("Usage report loaded — " + rows.size() + " active day(s).", "#00b894");
                    break;
                }

                case "SERVER_DISCONNECTED": {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Network Security Alert");
                    alert.setHeaderText("Server Connection Lost");
                    alert.setContentText("Connection to the server was lost. For security, you have been logged out.");
                    alert.showAndWait();
                    forceUIToMainMenu();
                    break;
                }

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
