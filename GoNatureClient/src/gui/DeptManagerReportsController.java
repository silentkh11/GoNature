package gui;

import client.ChatClient;
import entities.Message;
import entities.Park;
import entities.ReportData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Map;

public class DeptManagerReportsController {

    @FXML private ComboBox<String> cmbPark;
    @FXML private ComboBox<String> cmbMonth;
    @FXML private ComboBox<String> cmbYear;
    @FXML private PieChart visitorPieChart;
    @FXML private BarChart<String, Number> incomeBarChart;
    @FXML private Label lblTotalVisitors;
    @FXML private Label lblTotalIncome;
    @FXML private Label lblStatus;
    @FXML private Button themeBtn;

    // Map display name → parkId
    private java.util.HashMap<String, Integer> parkNameToId = new java.util.HashMap<>();
    private String pendingAction = "";

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);

        cmbMonth.getItems().addAll("1","2","3","4","5","6","7","8","9","10","11","12");
        cmbYear.getItems().addAll("2024","2025","2026","2027");

        // Load park list from server
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
        showStatus("Loading park list...", "#0984e3");
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/gui/DeptManagerDashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("GoNature - Department Manager");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleViewSubmittedReport(ActionEvent event) {
        if (!validateSelections()) return;
        pendingAction = "submitted";
        String[] data = { String.valueOf(getSelectedParkId()), cmbMonth.getValue(), cmbYear.getValue() };
        showStatus("Fetching submitted report...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_DEPT_REPORTS", data));
    }

    @FXML
    void handleCancellationsReport(ActionEvent event) {
        if (cmbMonth.getValue() == null || cmbYear.getValue() == null) {
            showStatus("Please select a month and year.", "#d63031");
            return;
        }
        pendingAction = "cancellations";
        String[] data = { cmbMonth.getValue(), cmbYear.getValue() };
        showStatus("Generating cancellations report...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CANCELLATIONS_REPORT", data));
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                case "ALL_PARKS_DATA":
                    ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                    ObservableList<String> parkNames = FXCollections.observableArrayList();
                    for (Park p : parks) {
                        String name = p.getParkId() + " - " + p.getName();
                        parkNames.add(name);
                        parkNameToId.put(name, p.getParkId());
                    }
                    cmbPark.setItems(parkNames);
                    showStatus("Parks loaded. Select a report.", "#00b894");
                    break;

                case "DEPT_REPORT_SUCCESS":
                    ReportData submitted = (ReportData) msg.getData();
                    renderCharts(submitted, "Submitted Visitors Report");
                    break;

                case "DEPT_REPORT_NOT_FOUND":
                    showStatus((String) msg.getData(), "#e17055");
                    clearCharts();
                    break;

                case "CANCELLATIONS_REPORT_DATA":
                    ReportData cancellations = (ReportData) msg.getData();
                    renderCharts(cancellations, "Cancellations Report");
                    break;
            }
        });
    }

    private void renderCharts(ReportData data, String title) {
        // Pie chart
        visitorPieChart.getData().clear();
        int total = 0;
        for (Map.Entry<String, Integer> entry : data.getVisitorBreakdown().entrySet()) {
            int val = entry.getValue();
            if (val > 0) {
                visitorPieChart.getData().add(
                    new PieChart.Data(entry.getKey() + " (" + val + ")", val));
                total += val;
            }
        }
        lblTotalVisitors.setText("Total: " + total);

        // Bar chart
        incomeBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);
        for (Map.Entry<String, Double> entry : data.getIncomeBreakdown().entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        // Also add visitor counts as bars if income is empty (cancellations report)
        if (data.getIncomeBreakdown().isEmpty()) {
            for (Map.Entry<String, Integer> entry : data.getVisitorBreakdown().entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }
        incomeBarChart.getData().add(series);

        double totalIncome = data.getTotalIncome();
        lblTotalIncome.setText(totalIncome > 0 ? "Income: ₪" + String.format("%.2f", totalIncome) : "Count view");
        showStatus(title + " loaded successfully.", "#00b894");
    }

    private void clearCharts() {
        visitorPieChart.getData().clear();
        incomeBarChart.getData().clear();
        lblTotalVisitors.setText("Total: —");
        lblTotalIncome.setText("Income: —");
    }

    private boolean validateSelections() {
        if (cmbPark.getValue() == null || cmbMonth.getValue() == null || cmbYear.getValue() == null) {
            showStatus("Please select a park, month, and year.", "#d63031");
            return false;
        }
        return true;
    }

    private int getSelectedParkId() {
        return parkNameToId.getOrDefault(cmbPark.getValue(), -1);
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}
