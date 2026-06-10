package view;

/**
 * Entry point for packaged JavaFX fat JARs.
 *
 * The Java launcher treats a main class that extends javafx.application.Application
 * specially and expects JavaFX on the module path. Keeping this class separate
 * lets the shaded JavaFX classes load from the JAR classpath.
 */
public class IronLauncher {
    public static void main(String[] args) {
        Iron.main(args);
    }
}
