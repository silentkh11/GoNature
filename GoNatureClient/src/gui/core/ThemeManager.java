package gui.core;

import javafx.scene.Scene;
import java.util.prefs.Preferences;


/**
 * Singleton that manages dark/light theme switching across all JavaFX scenes.
 * The user's preference is persisted in the Java {@link java.util.prefs.Preferences} store
 * so the same theme is applied on the next application launch.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private boolean darkMode;
    private final Preferences prefs;

    private static final String LIGHT_CSS = "/gui/assets/light-theme.css";
    private static final String DARK_CSS  = "/gui/assets/dark-theme.css";
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

    /** Returns the label the toggle button should show for the NEXT action. */
    public String toggleLabel() {
        return darkMode ? "☀ Light Mode" : "🌙 Dark Mode";
    }

    public void applyTo(Scene scene) {
        scene.getStylesheets().clear();
        String path = darkMode ? DARK_CSS : LIGHT_CSS;
        String url = getClass().getResource(path).toExternalForm();
        scene.getStylesheets().add(url);
    }

    public void toggle(Scene scene) {
        darkMode = !darkMode;
        prefs.putBoolean(PREF_KEY, darkMode);
        applyTo(scene);
    }
}
