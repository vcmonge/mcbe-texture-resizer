package view;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.TextureInfo;
import util.Constants;

/**
 * Componente visual que representa una tarjeta de textura en el mosaico.
 * Muestra:
 * - Miniatura de la textura
 * - Información de resolución con indicadores [M] y [N]
 * - Botón para procesar/reducir la textura
 * - CheckBox para modo de selección múltiple
 * 
 * Implementa binding con TextureInfo para actualización automática.
 * 
 * @author vmonge
 */
public class TextureCard extends VBox {
    
    // ==================== COMPONENTES ====================
    
    /** Vista de imagen para la miniatura */
    private final ImageView imageView;
    
    /** Etiqueta para mostrar resolución e indicadores */
    private final Label infoLabel;
    
    /** Etiqueta para el nombre del archivo */
    private final Label nameLabel;
    
    /** Botón para procesar la textura */
    private final Button processButton;
    
    /** CheckBox para selección múltiple */
    private final CheckBox selectionCheckBox;
    
    /** Contenedor de la imagen con checkbox superpuesto */
    private final StackPane imageContainer;
    
    // ==================== DATOS ====================
    
    /** Información de la textura asociada */
    private final TextureInfo textureInfo;
    
    /** Callback para cuando se presiona el botón de procesar */
    private Consumer<TextureInfo> onProcessCallback;
    
    /** Callback para cuando cambia el estado de selección */
    private Consumer<TextureCard> onSelectionChangedCallback;
    
    /** Propiedad para modo de selección activo */
    private final BooleanProperty selectionModeActive;
    
    /** Propiedad para estado seleccionado */
    private final BooleanProperty selected;
    
    // ==================== ESTILOS ====================
    // Los estilos visuales (normal/hover/selected) se definen en fxmlinicio.css
    // mediante las clases .texture-card y .texture-card:hover, y la pseudo-clase
    // ":selected". Esto evita mutaciones inline de -fx-effect/-fx-background-color
    // por cada hover, que con muchas cards causaba stutter notable.
    
    /** Pseudo-clase CSS para tarjeta seleccionada. */
    private static final PseudoClass PSEUDO_SELECTED = PseudoClass.getPseudoClass("selected");
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Crea una nueva tarjeta de textura.
     * 
     * @param textureInfo Información de la textura a mostrar
     */
    public TextureCard(TextureInfo textureInfo) {
        if (textureInfo == null) {
            throw new IllegalArgumentException("TextureInfo no puede ser null");
        }
        
        this.textureInfo = textureInfo;
        this.selectionModeActive = new SimpleBooleanProperty(false);
        this.selected = new SimpleBooleanProperty(false);
        
        // Crear componentes
        this.imageView = createImageView();
        this.selectionCheckBox = createSelectionCheckBox();
        this.imageContainer = createImageContainer();
        this.nameLabel = createNameLabel();
        this.infoLabel = createInfoLabel();
        this.processButton = createProcessButton();
        
        // Configurar layout
        setupLayout();
        
        // Configurar bindings
        setupBindings();
        
        // Aplicar estilo
        applyStyle();
    }
    
    // ==================== CREACIÓN DE COMPONENTES ====================
    
    /**
     * Crea y configura el ImageView para la miniatura.
     * 
     * @return ImageView configurado
     */
    private ImageView createImageView() {
        ImageView iv = new ImageView();
        
        iv.setFitWidth(Constants.THUMBNAIL_SIZE);
        iv.setFitHeight(Constants.THUMBNAIL_SIZE);
        iv.setPreserveRatio(true);
        iv.setSmooth(false); // Usar nearest neighbor para preview pixelado
        
        if (textureInfo.getThumbnailImage() != null) {
            iv.setImage(textureInfo.getThumbnailImage());
        }
        
        // Cache del bitmap rasterizado para que el scroll solo desplace
        // píxeles ya renderizados, sin recomponer el ImageView en cada tick.
        iv.setCache(true);
        iv.setCacheHint(CacheHint.SPEED);
        
        iv.getStyleClass().add("texture-thumbnail");
        
        return iv;
    }
    
    /**
     * Crea el checkbox para selección múltiple.
     * 
     * @return CheckBox configurado
     */
    private CheckBox createSelectionCheckBox() {
        CheckBox cb = new CheckBox();
        cb.getStyleClass().add("selection-checkbox");
        
        cb.selectedProperty().bindBidirectional(selected);
        
        return cb;
    }
    
    /**
     * Crea el contenedor de imagen con checkbox superpuesto.
     * 
     * @return StackPane configurado
     */
    private StackPane createImageContainer() {
        StackPane container = new StackPane();
        container.getChildren().addAll(imageView, selectionCheckBox);
        
        // Posicionar checkbox en esquina superior izquierda
        StackPane.setAlignment(selectionCheckBox, Pos.TOP_LEFT);
        StackPane.setMargin(selectionCheckBox, new Insets(2));
        
        // El checkbox solo es visible en modo selección
        selectionCheckBox.visibleProperty().bind(selectionModeActive);
        selectionCheckBox.managedProperty().bind(selectionModeActive);
        
        return container;
    }
    
    /**
     * Crea la etiqueta del nombre de archivo.
     * 
     * @return Label configurado
     */
    private Label createNameLabel() {
        Label label = new Label(truncateName(textureInfo.getBaseName(), 18));
        label.getStyleClass().add("texture-card-name");
        label.setMaxWidth(Constants.TEXTURE_CARD_WIDTH - 10);
        
        installLazyTooltip(label, () -> textureInfo.getBaseFile().getName());
        
        return label;
    }
    
    /**
     * Crea la etiqueta de información de resolución.
     * 
     * @return Label configurado
     */
    private Label createInfoLabel() {
        Label label = new Label(textureInfo.getResolutionText());
        label.getStyleClass().add("texture-card-info");
        
        installLazyTooltip(label, this::buildInfoTooltip);
        
        return label;
    }
    
    /**
     * Construye el texto del tooltip de información (solo se llama al primer hover).
     */
    private String buildInfoTooltip() {
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append("Resolución: ").append(textureInfo.getWidth())
                   .append("x").append(textureInfo.getHeight());
        if (textureInfo.hasMer()) {
            tooltipText.append("\nTextura MER: ").append(textureInfo.getMerFile().getName());
        }
        if (textureInfo.hasNormal()) {
            tooltipText.append("\nTextura Normal: ").append(textureInfo.getNormalFile().getName());
        }
        return tooltipText.toString();
    }
    
    /**
     * Crea el botón de procesamiento.
     * 
     * @return Button configurado
     */
    private Button createProcessButton() {
        Button button = new Button("Reducir");
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("texture-card-button");
        
        installLazyTooltip(button, () -> {
            int newWidth = Math.max(1, textureInfo.getWidth() / Constants.DEFAULT_SCALE_FACTOR);
            int newHeight = Math.max(1, textureInfo.getHeight() / Constants.DEFAULT_SCALE_FACTOR);
            return String.format("Reducir a %dx%d (factor %d)",
                    newWidth, newHeight, Constants.DEFAULT_SCALE_FACTOR);
        });
        
        button.setOnAction(e -> handleProcessClick());
        
        return button;
    }
    
    /**
     * Instala un Tooltip de forma lazy: solo se crea la primera vez que el
     * usuario pasa el mouse sobre el nodo. Esto evita crear cientos de
     * Tooltips eagerly al construir el grid, lo que reduce nodos y listeners
     * internos de JavaFX.
     * 
     * @param node Nodo al que asociar el tooltip
     * @param textSupplier Proveedor del texto (lazy)
     */
    private static void installLazyTooltip(javafx.scene.Node node, Supplier<String> textSupplier) {
        node.setOnMouseEntered(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                Tooltip tooltip = new Tooltip(textSupplier.get());
                Tooltip.install(node, tooltip);
                // Quitar este handler: ya no hace falta, el tooltip está instalado.
                node.setOnMouseEntered(null);
            }
        });
    }
    
    // ==================== CONFIGURACIÓN ====================
    
    /**
     * Configura el layout del VBox.
     */
    private void setupLayout() {
        setAlignment(Pos.CENTER);
        setSpacing(5);
        setPadding(new Insets(10));
        setPrefWidth(Constants.TEXTURE_CARD_WIDTH);
        setPrefHeight(Constants.TEXTURE_CARD_HEIGHT);
        setMinWidth(Constants.TEXTURE_CARD_WIDTH);
        setMinHeight(Constants.TEXTURE_CARD_HEIGHT);
        
        // Bitmap caching del scene graph completo de la card.
        // Hace que el scroll desplace píxeles ya rasterizados en lugar
        // de re-renderizar el VBox + hijos en cada frame.
        setCache(true);
        setCacheHint(CacheHint.SPEED);
        
        getChildren().addAll(imageContainer, nameLabel, infoLabel, processButton);
    }
    
    /**
     * Configura bindings con las propiedades de TextureInfo.
     */
    private void setupBindings() {
        // Binding de texto de resolución
        infoLabel.textProperty().bind(textureInfo.resolutionTextProperty());
        
        // Binding de visibilidad (ocultar cuando se procesa)
        this.visibleProperty().bind(textureInfo.processedProperty().not());
        this.managedProperty().bind(textureInfo.processedProperty().not());
    }
    
    /**
     * Aplica clase de estilo y configura el manejo de selección.
     * Los estilos visuales (normal/hover/selected) se aplican vía CSS,
     * sin listeners de mouse en cada card.
     */
    private void applyStyle() {
        getStyleClass().add("texture-card");
        
        // Sincroniza la pseudo-clase :selected con la propiedad observable.
        // CSS reacciona automáticamente y no requiere setStyle() inline.
        selected.addListener((obs, oldVal, newVal) -> {
            pseudoClassStateChanged(PSEUDO_SELECTED, newVal);
            if (onSelectionChangedCallback != null) {
                onSelectionChangedCallback.accept(this);
            }
        });
        
        // Clic en la tarjeta en modo selección
        setOnMouseClicked(e -> {
            if (selectionModeActive.get()) {
                selected.set(!selected.get());
            }
        });
    }
    
    // ==================== EVENTOS ====================
    
    /**
     * Maneja el clic en el botón de procesar.
     */
    private void handleProcessClick() {
        if (onProcessCallback != null) {
            // Deshabilitar botón mientras procesa
            processButton.setDisable(true);
            processButton.setText("...");
            
            onProcessCallback.accept(textureInfo);
        }
    }
    
    /**
     * Establece el callback para cuando se presiona el botón de procesar.
     * 
     * @param callback Función a ejecutar con la TextureInfo
     */
    public void setOnProcessCallback(Consumer<TextureInfo> callback) {
        this.onProcessCallback = callback;
    }
    
    /**
     * Establece el callback para cuando cambia el estado de selección.
     * 
     * @param callback Función a ejecutar cuando cambia la selección
     */
    public void setOnSelectionChangedCallback(Consumer<TextureCard> callback) {
        this.onSelectionChangedCallback = callback;
    }
    
    // ==================== MODO SELECCIÓN ====================
    
    /**
     * Activa o desactiva el modo de selección.
     * 
     * @param active true para activar, false para desactivar
     */
    public void setSelectionModeActive(boolean active) {
        selectionModeActive.set(active);
        
        // Si se desactiva el modo, deseleccionar (CSS quitará :selected solo).
        if (!active) {
            selected.set(false);
        }
    }
    
    /**
     * @return true si el modo de selección está activo
     */
    public boolean isSelectionModeActive() {
        return selectionModeActive.get();
    }
    
    /**
     * @return Propiedad de modo de selección activo
     */
    public BooleanProperty selectionModeActiveProperty() {
        return selectionModeActive;
    }
    
    /**
     * @return true si la tarjeta está seleccionada
     */
    public boolean isSelected() {
        return selected.get();
    }
    
    /**
     * Establece el estado de selección.
     * 
     * @param selected true para seleccionar, false para deseleccionar
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    /**
     * @return Propiedad de estado seleccionado
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    // ==================== UTILIDADES ====================
    
    /**
     * Trunca un nombre largo agregando "...".
     * 
     * @param name Nombre original
     * @param maxLength Longitud máxima
     * @return Nombre truncado si es necesario
     */
    private String truncateName(String name, int maxLength) {
        if (name.length() <= maxLength) {
            return name;
        }
        return name.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * @return TextureInfo asociada a esta tarjeta
     */
    public TextureInfo getTextureInfo() {
        return textureInfo;
    }
    
    /**
     * @return Resolución total (ancho * alto) para ordenamiento
     */
    public int getTotalResolution() {
        return textureInfo.getWidth() * textureInfo.getHeight();
    }
    
    /**
     * Rehabilita el botón de procesar (en caso de error).
     */
    public void resetButton() {
        processButton.setDisable(false);
        processButton.setText("Reducir");
    }
    
    /**
     * Actualiza el estado visual después de procesar exitosamente.
     */
    public void markAsProcessed() {
        processButton.setText("Listo");
        if (!processButton.getStyleClass().contains("texture-card-button-done")) {
            processButton.getStyleClass().add("texture-card-button-done");
        }
    }
}
