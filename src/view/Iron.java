package view;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controller.FXMLInicioController;

/**
 * Clase principal de la aplicación Iron - Reductor de Texturas.
 * 
 * Esta aplicación permite reducir la resolución de texturas de resource packs
 * de videojuegos de manera manual, textura por textura.
 * 
 * Características:
 * - Carga texturas desde carpetas textures/blocks y textures/items
 * - Soporta formatos PNG y TGA (con canal alpha)
 * - Detecta texturas asociadas (_mer, _normal)
 * - Usa algoritmo nearest neighbor para preservar pixel art
 * - Interfaz organizada en pestañas por tipo de textura
 * 
 * @author vmonge
 * @version 1.0
 */
public class Iron extends Application {
    
    /** Controlador principal para gestionar cierre */
    private FXMLInicioController controller;
    
    /**
     * Punto de entrada de la aplicación JavaFX.
     * Configura y muestra la ventana principal.
     * 
     * @param stage Stage principal de la aplicación
     */
    @Override
    public void start(Stage stage) {
        try {
            // Cargar FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FXMLInicio.fxml"));
            Parent vista = loader.load();
            
            // Obtener referencia al controlador
            controller = loader.getController();
            
            // Configurar escena
            Scene escena = new Scene(vista);
            
            // Configurar stage
            stage.setScene(escena);
            stage.setTitle("Iron \u2014 Game Texture Resizer");
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setWidth(1280);
            stage.setHeight(800);
            
            // Manejar cierre de aplicación
            stage.setOnCloseRequest(event -> {
                if (controller != null) {
                    controller.shutdown();
                }
            });
            
            // Mostrar ventana
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading the interface: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Método llamado al cerrar la aplicación.
     * Libera recursos del controlador.
     */
    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    /**
     * Punto de entrada principal de la aplicación.
     * 
     * @param args Argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
