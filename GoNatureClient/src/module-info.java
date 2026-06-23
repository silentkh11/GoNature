module GoNatureClient {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.prefs;
	requires java.sql;
    // --- Allow JavaFX reflection on entity classes (required by PropertyValueFactory) ---
    opens entities to javafx.base;

    // --- Grant JavaFX permission to inject @FXML fields in these packages ---
    opens gui.auth to javafx.fxml;
    opens gui.core to javafx.fxml;
    opens gui.gate to javafx.fxml;
    opens gui.guest to javafx.fxml;
    opens gui.management to javafx.fxml;
    opens gui.service to javafx.fxml;

    // --- Export packages so the JVM can execute them ---
    exports gui.auth;
    exports gui.core;
    exports gui.gate;
    exports gui.guest;
    exports gui.management;
    exports gui.service;
    
    // Keep your core client network package exported!
    exports client;
}