package gui;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static ThemeManager instance;
    private boolean darkMode;
    private final Preferences prefs;

    private static final String LIGHT_CSS = "/gui/light-theme.css";
    private static final String DARK_CSS  = "/gui/dark-theme.css";
    private static final String PREF_KEY  = "darkMode";

    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);
        darkMode = prefs.getBoolean(PREF_KEY, true);
    }

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public String toggleLabel() {
        return darkMode ? "☀ Light Mode" : "🌙 Dark Mode";
    }

    public void applyTo(Scene scene) {
        scene.getStylesheets().clear();
        String path = darkMode ? DARK_CSS : LIGHT_CSS;
        scene.getStylesheets().add(getClass().getResource(path).toExternalForm());
    }

    public void toggle(Scene scene) {
        darkMode = !darkMode;
        prefs.putBoolean(PREF_KEY, darkMode);
        applyTo(scene);
    }
}
