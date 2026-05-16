@echo off

:: 1. Set paths
set "JAVAFX_LIB=%~dp0G7_Prototype_Server_lib"
set "JAVAFX_BIN=%~dp0G7_Prototype_Server_bin"

:: 2. Run the JAR file silently in the background
start "" javaw --enable-native-access=javafx.graphics -Djava.library.path="%JAVAFX_BIN%" --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -jar "%~dp0G7_Prototype_Server.jar"