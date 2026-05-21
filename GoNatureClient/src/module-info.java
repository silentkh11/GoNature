module GoNatureClient {
    // Requirements
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires java.sql; 

    // Exports & Opens for the GUI
    exports client; 
    exports gui;
    opens gui to javafx.fxml;

    // THE FIX: Allow JavaFX to read the data inside your Order class for the TableView!
    opens entities to javafx.base; 
}