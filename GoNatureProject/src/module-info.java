module GoNatureProject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    
    // Server Permissions
    opens server to javafx.fxml;
    exports server;
    
    // Client Permissions
    opens client to javafx.fxml;
    exports client;
    
    // Add these lines to let the TableView read your Order class!
    opens firstPackage to javafx.base;
    exports firstPackage;
}