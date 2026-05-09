package controller;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import model.TextureInfo;
import service.ImageProcessor;
import service.TextureService;
import util.Constants;
import util.ResizeAlgorithm;
import view.CanvasTextureGrid;

/**
 * Controlador principal de la aplicación Iron.
 * Gestiona la interacción del usuario con la interfaz gráfica.
 * 
 * Responsabilidades:
 * - Manejar selección de carpeta raíz
 * - Coordinar carga de texturas (Blocks, Items, Environment, Entity)
 * - Gestionar eventos de procesamiento
 * - Manejar modo de selección múltiple
 * - Gestionar ordenamiento por resolución
 * - Actualizar estado de la UI
 * 
 * Cada categoría de texturas se muestra en un {@link CanvasTextureGrid}
 * que dibuja todas las miniaturas en un único Canvas (en lugar de un
 * nodo por textura), evitando que el scene graph se vuelva pesado con
 * miles de texturas.
 * 
 * @author vmonge
 */
public class FXMLInicioController implements Initializable {

    // ==================== COMPONENTES FXML ====================
    
    @FXML private TextField txtRootPath;
    @FXML private Button btnSelectFolder;
    @FXML private Button btnLoad;
    @FXML private ComboBox<Integer> cmbScaleFactor;
    @FXML private ComboBox<ResizeAlgorithm> cmbAlgorithm;
    @FXML private TabPane tabPane;
    
    // Scroll panes (reciben un CanvasTextureGrid como content)
    @FXML private ScrollPane scrollBlocks;
    @FXML private ScrollPane scrollItems;
    @FXML private ScrollPane scrollEnvironment;
    @FXML private ScrollPane scrollEntity;
    
    // Controles de modo selección
    @FXML private ToggleButton btnSelectionMode;
    @FXML private Button btnProcessSelected;
    @FXML private Button btnSelectAll;
    @FXML private Button btnDeselectAll;
    
    // Control de ordenamiento
    @FXML private ComboBox<String> cmbSortOrder;
    
    // Etiquetas de estado
    @FXML private Label lblStatus;
    @FXML private Label lblBlocksCount;
    @FXML private Label lblItemsCount;
    @FXML private Label lblEnvironmentCount;
    @FXML private Label lblEntityCount;
    @FXML private ProgressBar progressBar;
    
    // ==================== GRIDS VIRTUALIZADOS ====================
    
    /** Grid del Canvas para texturas de bloques. */
    private CanvasTextureGrid gridBlocks;
    
    /** Grid del Canvas para texturas de items. */
    private CanvasTextureGrid gridItems;
    
    /** Grid del Canvas para texturas de entorno. */
    private CanvasTextureGrid gridEnvironment;
    
    /** Grid del Canvas para texturas de entidades. */
    private CanvasTextureGrid gridEntity;
    
    // ==================== SERVICIOS ====================
    
    /** Servicio para cargar y procesar texturas */
    private TextureService textureService;
    
    // ==================== ESTADO ====================
    
    /** Directorio raíz seleccionado */
    private File rootDirectory;
    
    /** Listas de texturas por categoría */
    private List<TextureInfo> blockTextures;
    private List<TextureInfo> itemTextures;
    private List<TextureInfo> environmentTextures;
    private List<TextureInfo> entityTextures;
    
    /** Contador de texturas procesadas */
    private int processedCount = 0;
    
    /** Total de texturas */
    private int totalTextures = 0;
    
    /**
     * Modo de selección múltiple, observable. Se comparte con todos los
     * grids para que sepan si los clicks alternan selección o no.
     */
    private final BooleanProperty selectionModeActive = new SimpleBooleanProperty(false);
    
    // ==================== CONSTANTES DE ORDENAMIENTO ====================
    
    private static final String SORT_DEFAULT = "Unsorted";
    private static final String SORT_RES_ASC = "Resolution (low \u2192 high)";
    private static final String SORT_RES_DESC = "Resolution (high \u2192 low)";
    
    // ==================== INICIALIZACIÓN ====================
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeScaleFactorCombo();
        initializeAlgorithmCombo();
        
        initializeSortOrderCombo();
        
        // Crear servicio con factor de escala por defecto
        createTextureService();
        
        // Crear grids virtualizados y conectarlos a sus ScrollPanes
        gridBlocks = createGrid(scrollBlocks);
        gridItems = createGrid(scrollItems);
        gridEnvironment = createGrid(scrollEnvironment);
        gridEntity = createGrid(scrollEntity);
        
        updateStatus("Ready \u2014 Select a resource pack folder");
    }
    
    /**
     * Crea un CanvasTextureGrid, lo conecta al ScrollPane correspondiente
     * y le pasa los callbacks de procesamiento y cambio de selección.
     */
    private CanvasTextureGrid createGrid(ScrollPane scroll) {
        CanvasTextureGrid grid = new CanvasTextureGrid();
        scroll.setContent(grid);
        grid.attachToScrollPane(scroll);
        grid.setSelectionModeActiveProperty(selectionModeActive);
        grid.setOnProcessCallback(this::processTexture);
        grid.setOnSelectionChangedCallback(this::updateSelectedCount);
        return grid;
    }
    
    private void initializeScaleFactorCombo() {
        cmbScaleFactor.setItems(FXCollections.observableArrayList(2, 4, 8, 16));
        cmbScaleFactor.setValue(Constants.DEFAULT_SCALE_FACTOR);
        
        cmbScaleFactor.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                createTextureService();
                updateStatus("Scale factor changed to " + newVal);
            }
        });
    }
    
    private void initializeAlgorithmCombo() {
        cmbAlgorithm.setItems(FXCollections.observableArrayList(ResizeAlgorithm.values()));
        cmbAlgorithm.setValue(Constants.DEFAULT_ALGORITHM);
        
        cmbAlgorithm.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                createTextureService();
                updateStatus("Algorithm changed to " + newVal.getDisplayName());
            }
        });
    }
    
    private void initializeSortOrderCombo() {
        cmbSortOrder.setItems(FXCollections.observableArrayList(
                SORT_DEFAULT, SORT_RES_ASC, SORT_RES_DESC));
        cmbSortOrder.setValue(SORT_DEFAULT);
        
        cmbSortOrder.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applySorting();
                updateStatus("Sort applied: " + newVal);
            }
        });
    }
    
    /**
     * Crea o recrea el servicio de texturas con la configuración actual.
     */
    private void createTextureService() {
        if (textureService != null) {
            textureService.shutdown();
        }
        
        int scaleFactor = cmbScaleFactor.getValue() != null ? 
                cmbScaleFactor.getValue() : Constants.DEFAULT_SCALE_FACTOR;
        
        ResizeAlgorithm algo = cmbAlgorithm != null && cmbAlgorithm.getValue() != null ?
                cmbAlgorithm.getValue() : Constants.DEFAULT_ALGORITHM;
        
        ImageProcessor processor = new ImageProcessor(scaleFactor, algo,
                Constants.TGA_INDEPENDENT_ALPHA_PROCESSING);
        textureService = new TextureService(processor);
    }
    
    // ==================== MANEJADORES DE EVENTOS ====================
    
    @FXML
    private void handleSelectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Resource Pack Folder");
        
        if (rootDirectory != null && rootDirectory.exists()) {
            chooser.setInitialDirectory(rootDirectory);
        }
        
        Stage stage = (Stage) btnSelectFolder.getScene().getWindow();
        File selected = chooser.showDialog(stage);
        
        if (selected != null) {
            rootDirectory = selected;
            txtRootPath.setText(selected.getAbsolutePath());
            
            if (textureService.hasValidTextureDirectories(selected)) {
                btnLoad.setDisable(false);
                updateStatus("Valid folder \u2014 Click 'Load Textures'");
            } else {
                btnLoad.setDisable(true);
                updateStatus("Warning: No texture folders found");
                
                showWarning("Structure Not Found", 
                        "Expected folders were not found:\n" +
                        "- " + Constants.BLOCKS_PATH + "\n" +
                        "- " + Constants.ITEMS_PATH + "\n" +
                        "- " + Constants.ENVIRONMENT_PATH + "\n" +
                        "- " + Constants.ENTITY_PATH + "\n\n" +
                        "Verify that the selected folder is the resource pack root.");
            }
        }
    }
    
    @FXML
    private void handleLoadTextures() {
        if (rootDirectory == null || !rootDirectory.exists()) {
            showError("Error", "Select a valid folder first.");
            return;
        }
        
        clearAllGrids();
        
        if (selectionModeActive.get()) {
            handleToggleSelectionMode();
        }
        
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Loading block textures...");
                blockTextures = textureService.loadBlockTextures(rootDirectory, 
                        msg -> Platform.runLater(() -> updateStatus(msg)));
                
                updateMessage("Loading item textures...");
                itemTextures = textureService.loadItemTextures(rootDirectory,
                        msg -> Platform.runLater(() -> updateStatus(msg)));
                
                updateMessage("Loading environment textures...");
                environmentTextures = textureService.loadEnvironmentTextures(rootDirectory,
                        msg -> Platform.runLater(() -> updateStatus(msg)));
                
                updateMessage("Loading entity textures...");
                entityTextures = textureService.loadEntityTextures(rootDirectory,
                        msg -> Platform.runLater(() -> updateStatus(msg)));
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                // Asignar a cada grid: como el grid renderiza por Canvas, esto es O(N)
                // pero no crea nodos: instantáneo incluso con miles de texturas.
                displayTextures();
                
                // Aplicar ordenamiento si está seleccionado
                applySorting();
                
                setControlsEnabled(true);
                progressBar.setVisible(false);
                
                totalTextures = 
                        (blockTextures != null ? blockTextures.size() : 0) + 
                        (itemTextures != null ? itemTextures.size() : 0) +
                        (environmentTextures != null ? environmentTextures.size() : 0) +
                        (entityTextures != null ? entityTextures.size() : 0);
                processedCount = 0;
                
                updateCounters();
                updateStatus(String.format("Load complete \u2014 %d textures found", totalTextures));
            }
            
            @Override
            protected void failed() {
                setControlsEnabled(true);
                progressBar.setVisible(false);
                
                Throwable e = getException();
                updateStatus("Error during load: " + e.getMessage());
                showError("Load Error", "An error occurred while loading textures:\n" + e.getMessage());
            }
        };
        
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    // ==================== MODO SELECCIÓN ====================
    
    @FXML
    private void handleToggleSelectionMode() {
        boolean newMode = btnSelectionMode.isSelected();
        selectionModeActive.set(newMode);
        
        if (newMode) {
            btnSelectionMode.setText("Disable Selection");
            btnSelectionMode.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
            
            btnProcessSelected.setVisible(true);
            btnProcessSelected.setManaged(true);
            btnSelectAll.setVisible(true);
            btnSelectAll.setManaged(true);
            btnDeselectAll.setVisible(true);
            btnDeselectAll.setManaged(true);
            
        } else {
            btnSelectionMode.setText("Enable Selection");
            btnSelectionMode.setStyle("");
            
            btnProcessSelected.setVisible(false);
            btnProcessSelected.setManaged(false);
            btnSelectAll.setVisible(false);
            btnSelectAll.setManaged(false);
            btnDeselectAll.setVisible(false);
            btnDeselectAll.setManaged(false);
            
            // Los grids ya escuchan selectionModeActive y vacían su selección.
            updateSelectedCount();
        }
    }
    
    @FXML
    private void handleSelectAll() {
        CanvasTextureGrid active = getActiveGrid();
        if (active != null) {
            active.selectAllVisible(true);
        }
    }
    
    @FXML
    private void handleDeselectAll() {
        CanvasTextureGrid active = getActiveGrid();
        if (active != null) {
            active.selectAllVisible(false);
        }
    }
    
    /**
     * @return Grid de la pestaña actualmente activa, o null
     */
    private CanvasTextureGrid getActiveGrid() {
        int idx = tabPane.getSelectionModel().getSelectedIndex();
        switch (idx) {
            case 0: return gridBlocks;
            case 1: return gridItems;
            case 2: return gridEnvironment;
            case 3: return gridEntity;
            default: return null;
        }
    }
    
    /**
     * @return Iterable con todos los grids del controller
     */
    private List<CanvasTextureGrid> allGrids() {
        return Arrays.asList(gridBlocks, gridItems, gridEnvironment, gridEntity);
    }
    
    @FXML
    private void handleProcessSelected() {
        Set<TextureInfo> selected = getAllSelectedTextures();
        
        if (selected.isEmpty()) {
            showWarning("No Selection", "No textures selected for processing.");
            return;
        }
        
        updateStatus(String.format("Processing %d selected textures...", selected.size()));
        btnProcessSelected.setDisable(true);
        
        for (TextureInfo t : selected) {
            processTexture(t);
        }
    }
    
    /**
     * Recolecta todas las texturas seleccionadas de todos los grids.
     */
    private Set<TextureInfo> getAllSelectedTextures() {
        Set<TextureInfo> all = new LinkedHashSet<>();
        for (CanvasTextureGrid g : allGrids()) {
            if (g != null) all.addAll(g.getSelectedItems());
        }
        return all;
    }
    
    /**
     * Recalcula el contador de seleccionados y actualiza el botón.
     */
    private void updateSelectedCount() {
        int count = 0;
        for (CanvasTextureGrid g : allGrids()) {
            if (g != null) count += g.getSelectedCount();
        }
        btnProcessSelected.setText(String.format("Process Selected (%d)", count));
        btnProcessSelected.setDisable(count == 0);
    }
    
    // ==================== ORDENAMIENTO ====================
    
    /**
     * Ordena las listas de TextureInfo según el criterio del combo
     * y reasigna a sus grids.
     */
    private void applySorting() {
        String sortOrder = cmbSortOrder.getValue();
        if (sortOrder == null) return;
        
        Comparator<TextureInfo> comparator = createComparator(sortOrder);
        
        sortAndAssign(blockTextures, gridBlocks, comparator);
        sortAndAssign(itemTextures, gridItems, comparator);
        sortAndAssign(environmentTextures, gridEnvironment, comparator);
        sortAndAssign(entityTextures, gridEntity, comparator);
    }
    
    /**
     * Si el comparador no es null, ordena la lista in-place y la
     * reasigna al grid (esto fuerza un repintado con el nuevo orden).
     */
    private void sortAndAssign(List<TextureInfo> list, CanvasTextureGrid grid,
            Comparator<TextureInfo> comparator) {
        if (list == null || grid == null) return;
        if (comparator != null) {
            Collections.sort(list, comparator);
        }
        grid.setItems(list);
    }
    
    /**
     * Crea un comparador de TextureInfo según el criterio. Devuelve null
     * para "Sin ordenar".
     */
    private Comparator<TextureInfo> createComparator(String sortOrder) {
        if (sortOrder.equals(SORT_RES_ASC)) {
            return (a, b) -> Integer.compare(
                    a.getWidth() * a.getHeight(),
                    b.getWidth() * b.getHeight());
        } else if (sortOrder.equals(SORT_RES_DESC)) {
            return (a, b) -> Integer.compare(
                    b.getWidth() * b.getHeight(),
                    a.getWidth() * a.getHeight());
        }
        return null;
    }
    
    // ==================== VISUALIZACIÓN ====================
    
    /**
     * Asigna las listas de texturas a sus grids correspondientes.
     * Como el grid usa Canvas, esto NO crea nodos: el coste es solo
     * O(N) para construir la displayList y un repaint.
     */
    private void displayTextures() {
        gridBlocks.setItems(blockTextures);
        gridItems.setItems(itemTextures);
        gridEnvironment.setItems(environmentTextures);
        gridEntity.setItems(entityTextures);
    }
    
    /**
     * Limpia el contenido de todos los grids y las listas en memoria.
     */
    private void clearAllGrids() {
        gridBlocks.setItems(Collections.emptyList());
        gridItems.setItems(Collections.emptyList());
        gridEnvironment.setItems(Collections.emptyList());
        gridEntity.setItems(Collections.emptyList());
        
        blockTextures = null;
        itemTextures = null;
        environmentTextures = null;
        entityTextures = null;
    }
    
    // ==================== PROCESAMIENTO ====================
    
    /**
     * Procesa una textura individual (reduce su resolución).
     */
    private void processTexture(TextureInfo textureInfo) {
        textureService.processTextureAsync(textureInfo,
            () -> {
                processedCount++;
                // Buscar el grid que contiene la textura y marcar como procesada
                // (esto la oculta del display y repinta).
                CanvasTextureGrid owner = findOwningGrid(textureInfo);
                if (owner != null) {
                    owner.markProcessed(textureInfo);
                }
                updateCounters();
                updateSelectedCount();
                updateStatus(String.format("Processed: %s [%d/%d]", 
                        textureInfo.getBaseFile().getName(), 
                        processedCount, totalTextures));
            },
            (Exception e) -> {
                showError("Processing Error", 
                        "Error processing " + textureInfo.getBaseFile().getName() + 
                        ":\n" + e.getMessage());
                updateStatus("Error processing: " + textureInfo.getBaseFile().getName());
            }
        );
    }
    
    /**
     * Encuentra el grid que contiene una TextureInfo según las listas
     * fuente del controller.
     */
    private CanvasTextureGrid findOwningGrid(TextureInfo t) {
        if (blockTextures != null && blockTextures.contains(t)) return gridBlocks;
        if (itemTextures != null && itemTextures.contains(t)) return gridItems;
        if (environmentTextures != null && environmentTextures.contains(t)) return gridEnvironment;
        if (entityTextures != null && entityTextures.contains(t)) return gridEntity;
        return null;
    }
    
    // ==================== ACTUALIZACIÓN DE UI ====================
    
    private void updateStatus(String message) {
        if (Platform.isFxApplicationThread()) {
            lblStatus.setText(message);
        } else {
            Platform.runLater(() -> lblStatus.setText(message));
        }
    }
    
    private void updateCounters() {
        int blocksTotal = blockTextures != null ? blockTextures.size() : 0;
        int itemsTotal = itemTextures != null ? itemTextures.size() : 0;
        int environmentTotal = environmentTextures != null ? environmentTextures.size() : 0;
        int entityTotal = entityTextures != null ? entityTextures.size() : 0;
        
        int blocksProcessed = countProcessed(blockTextures);
        int itemsProcessed = countProcessed(itemTextures);
        int environmentProcessed = countProcessed(environmentTextures);
        int entityProcessed = countProcessed(entityTextures);
        
        lblBlocksCount.setText(String.format("Blocks: %d/%d", blocksProcessed, blocksTotal));
        lblItemsCount.setText(String.format("Items: %d/%d", itemsProcessed, itemsTotal));
        lblEnvironmentCount.setText(String.format("Environment: %d/%d", environmentProcessed, environmentTotal));
        lblEntityCount.setText(String.format("Entity: %d/%d", entityProcessed, entityTotal));
    }
    
    private int countProcessed(List<TextureInfo> textures) {
        if (textures == null) return 0;
        int count = 0;
        for (TextureInfo t : textures) {
            if (t.isProcessed()) count++;
        }
        return count;
    }
    
    private void setControlsEnabled(boolean enabled) {
        btnSelectFolder.setDisable(!enabled);
        btnLoad.setDisable(!enabled);
        cmbScaleFactor.setDisable(!enabled);
        cmbAlgorithm.setDisable(!enabled);
        cmbSortOrder.setDisable(!enabled);
        btnSelectionMode.setDisable(!enabled);
    }
    
    // ==================== DIÁLOGOS ====================
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==================== LIMPIEZA ====================
    
    /**
     * Libera recursos al cerrar la aplicación.
     * Debe llamarse desde la clase principal.
     */
    public void shutdown() {
        if (textureService != null) {
            textureService.shutdown();
        }
    }
}
