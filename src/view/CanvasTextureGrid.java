package view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import model.TextureInfo;

/**
 * Componente de grid virtualizado que dibuja todas las texturas en un
 * único Canvas, en lugar de crear un nodo por cada una.
 *
 * Por qué: con miles de texturas, el scene graph de JavaFX (con ~7 nodos
 * por TextureCard) llega a decenas de miles de nodos, lo que vuelve toda
 * la app lenta (CSS pass, layout, pickResult, cache de Prism). Aquí
 * reducimos el grid a 1 solo nodo (el Canvas) y dibujamos directo con
 * GraphicsContext. El scene graph se libera y todos los demás controles
 * (botones, combos) responden al instante.
 *
 * El Canvas tiene tamaño igual al viewport del ScrollPane (no del
 * contenido total), evitando límites de tamaño máximo de Canvas en GPU.
 * Al hacer scroll, el Canvas se reposiciona dentro del Pane padre para
 * "flotar" siempre sobre el área visible, y se redibuja con la franja
 * de cards correspondiente.
 *
 * @author vmonge
 */
public class CanvasTextureGrid extends Pane {

    // ==================== CONSTANTES DE LAYOUT ====================
    
    /** Ancho de cada card. */
    private static final double CARD_W = 150;
    
    /** Alto de cada card. */
    private static final double CARD_H = 180;
    
    /** Espacio horizontal entre cards. */
    private static final double H_GAP = 10;
    
    /** Espacio vertical entre cards. */
    private static final double V_GAP = 10;
    
    /** Padding alrededor del grid. */
    private static final double PADDING = 15;
    
    /** Tamaño visible de la miniatura dentro de la card. */
    private static final double THUMB_SIZE = 100;
    
    /** Alto del "botón" Reducir dentro de la card. */
    private static final double BUTTON_H = 26;
    
    /** Margen interno entre los elementos de la card. */
    private static final double CARD_PAD = 8;
    
    // ==================== COLORES ====================
    
    private static final Color BG_NORMAL = Color.WHITE;
    private static final Color BG_HOVER = Color.web("#f8f8f8");
    private static final Color BG_SELECTED = Color.web("#e8f5e9");
    private static final Color BG_PROCESSED = Color.web("#f0f0f0");
    private static final Color BORDER_NORMAL = Color.web("#dddddd");
    private static final Color BORDER_HOVER = Color.web("#4CAF50");
    private static final Color BORDER_SELECTED = Color.web("#4CAF50");
    private static final Color TEXT_NAME = Color.web("#333333");
    private static final Color TEXT_INFO = Color.web("#666666");
    private static final Color BUTTON_BG = Color.web("#4CAF50");
    private static final Color BUTTON_BG_HOVER = Color.web("#45a049");
    private static final Color BUTTON_TEXT = Color.WHITE;
    private static final Color PLACEHOLDER_BG = Color.web("#eeeeee");
    private static final Color PLACEHOLDER_BORDER = Color.web("#cccccc");
    private static final Color PLACEHOLDER_TEXT = Color.web("#999999");
    
    // ==================== FUENTES ====================
    
    private static final Font FONT_NAME = Font.font("System", 11);
    private static final Font FONT_INFO = Font.font("System", 10);
    private static final Font FONT_BUTTON = Font.font("System", 11);
    private static final Font FONT_PLACEHOLDER = Font.font("System", 24);
    
    // ==================== ESTADO ====================
    
    /** Canvas único donde se dibuja todo el grid. */
    private final Canvas canvas = new Canvas();
    
    /** Lista completa de items (incluye procesados). */
    private List<TextureInfo> items = Collections.emptyList();
    
    /** Lista filtrada (sin procesados) usada para layout y pintado. */
    private List<TextureInfo> displayList = Collections.emptyList();
    
    /** Conjunto de texturas seleccionadas (espejo de TextureInfo.selected). */
    private final Set<TextureInfo> selectedSet = new LinkedHashSet<>();
    
    /** Item bajo el cursor del mouse para feedback visual. */
    private TextureInfo hoveredItem = null;
    
    /** Item cuyo botón "Reducir" está hovered (para color del botón). */
    private int hoveredButtonIndex = -1;
    
    /** Número de columnas actual según el ancho del viewport. */
    private int cols = 1;
    
    /** Alto de fila (CARD_H + V_GAP). */
    private static final double ROW_H = CARD_H + V_GAP;
    
    /** ScrollPane padre, asociado vía attachToScrollPane. */
    private ScrollPane scrollPane;
    
    /** Propiedad observable que indica si el modo selección está activo. */
    private BooleanProperty selectionModeActive = new SimpleBooleanProperty(false);
    
    /** Callback al hacer click en el botón "Reducir" de una card. */
    private Consumer<TextureInfo> onProcessCallback;
    
    /** Callback al cambiar la selección de cualquier card. */
    private Runnable onSelectionChangedCallback;
    
    // ==================== CONSTRUCTOR ====================
    
    public CanvasTextureGrid() {
        getChildren().add(canvas);
        setMinHeight(0);
        
        // Repintar al redimensionar el ancho del Pane: cambia el número de
        // columnas y por tanto la altura total y la disposición.
        widthProperty().addListener((obs, o, n) -> {
            recalcLayout();
            requestRepaint();
        });
        
        canvas.setOnMouseMoved(e -> handleMouseMoved(e.getX(), e.getY()));
        canvas.setOnMouseExited(e -> {
            if (hoveredItem != null || hoveredButtonIndex != -1) {
                hoveredItem = null;
                hoveredButtonIndex = -1;
                requestRepaint();
            }
        });
        canvas.setOnMouseClicked(e -> handleMouseClicked(e.getX(), e.getY()));
    }
    
    // ==================== API PÚBLICA ====================
    
    /**
     * Asocia el grid al ScrollPane padre. Se necesita porque el grid
     * dibuja solo la franja visible, basada en el viewport y el vvalue.
     * 
     * @param sp ScrollPane que contiene este grid como content
     */
    public void attachToScrollPane(ScrollPane sp) {
        this.scrollPane = sp;
        
        sp.viewportBoundsProperty().addListener((obs, o, n) -> {
            recalcLayout();
            requestRepaint();
        });
        sp.vvalueProperty().addListener((obs, o, n) -> requestRepaint());
    }
    
    /**
     * Configura el modo de selección observable. El grid lee esta
     * propiedad para decidir si los clicks alternan selección o no.
     * 
     * @param prop Propiedad booleana del controller
     */
    public void setSelectionModeActiveProperty(BooleanProperty prop) {
        this.selectionModeActive = prop;
        // Si se desactiva el modo selección, vaciamos selección visual.
        prop.addListener((obs, o, n) -> {
            if (!n) {
                clearSelection();
            }
        });
    }
    
    /**
     * @param cb Callback ejecutado al hacer click en el botón "Reducir" de una card
     */
    public void setOnProcessCallback(Consumer<TextureInfo> cb) {
        this.onProcessCallback = cb;
    }
    
    /**
     * @param cb Callback ejecutado al cambiar la selección
     */
    public void setOnSelectionChangedCallback(Runnable cb) {
        this.onSelectionChangedCallback = cb;
    }
    
    /**
     * Asigna la lista de texturas a mostrar. Recalcula el layout y repinta.
     * 
     * @param list Nueva lista de TextureInfo (puede ser null)
     */
    public void setItems(List<TextureInfo> list) {
        this.items = list != null ? list : Collections.emptyList();
        rebuildDisplayList();
        recalcLayout();
        requestRepaint();
    }
    
    /**
     * @return Conjunto inmutable de texturas actualmente seleccionadas
     */
    public Set<TextureInfo> getSelectedItems() {
        return new LinkedHashSet<>(selectedSet);
    }
    
    /**
     * @return Cantidad de items seleccionados
     */
    public int getSelectedCount() {
        return selectedSet.size();
    }
    
    /**
     * Selecciona o deselecciona todos los items visibles (no procesados).
     * 
     * @param selected true para seleccionar todos, false para deseleccionar
     */
    public void selectAllVisible(boolean selected) {
        boolean changed = false;
        if (selected) {
            for (TextureInfo t : displayList) {
                if (!selectedSet.contains(t)) {
                    selectedSet.add(t);
                    t.setSelected(true);
                    changed = true;
                }
            }
        } else {
            if (!selectedSet.isEmpty()) {
                for (TextureInfo t : selectedSet) {
                    t.setSelected(false);
                }
                selectedSet.clear();
                changed = true;
            }
        }
        if (changed) {
            requestRepaint();
            fireSelectionChanged();
        }
    }
    
    /**
     * Marca una textura como procesada y la oculta del grid.
     * 
     * @param t Textura a marcar
     */
    public void markProcessed(TextureInfo t) {
        t.setProcessed(true);
        if (selectedSet.remove(t)) {
            t.setSelected(false);
        }
        rebuildDisplayList();
        recalcLayout();
        requestRepaint();
    }
    
    /**
     * Solicita un repintado del Canvas (p. ej. al cambiar contadores externos).
     */
    public void refresh() {
        requestRepaint();
    }
    
    // ==================== LAYOUT ====================
    
    /**
     * Recalcula columnas, alto preferido del Pane y tamaño del Canvas
     * según el ancho disponible y la cantidad de items visibles.
     */
    private void recalcLayout() {
        double width = getWidth();
        if (width <= 0 && scrollPane != null) {
            Bounds vb = scrollPane.getViewportBounds();
            if (vb != null) width = vb.getWidth();
        }
        if (width <= 0) {
            cols = 1;
            setPrefHeight(0);
            return;
        }
        
        double inner = Math.max(0, width - 2 * PADDING);
        // Calcula cuántas cards caben con el gap entre ellas.
        cols = Math.max(1, (int) Math.floor((inner + H_GAP) / (CARD_W + H_GAP)));
        
        int n = displayList.size();
        int rows = (n + cols - 1) / cols;
        double totalH = (rows == 0) ? 0 : (PADDING * 2 + rows * CARD_H + (rows - 1) * V_GAP);
        setPrefHeight(totalH);
        setMinHeight(totalH);
        
        // Canvas del tamaño del viewport (no del total): se mueve con el scroll.
        double viewportW = width;
        double viewportH = (scrollPane != null && scrollPane.getViewportBounds() != null)
                ? scrollPane.getViewportBounds().getHeight()
                : getHeight();
        if (viewportH <= 0) viewportH = totalH;
        
        canvas.setWidth(viewportW);
        canvas.setHeight(Math.max(1, viewportH));
    }
    
    @Override
    protected void layoutChildren() {
        // No invocamos super: queremos posicionar el Canvas manualmente.
        positionCanvas();
    }
    
    /**
     * Reposiciona el Canvas dentro del Pane para que su rectángulo
     * coincida con la zona visible del ScrollPane. Como el ScrollPane
     * traslada el contenido por -vvalue*(contentH-viewportH), poner
     * layoutY del Canvas en vvalue*(contentH-viewportH) lo deja
     * siempre visible en el centro del viewport.
     */
    private void positionCanvas() {
        if (scrollPane == null) {
            canvas.setLayoutX(0);
            canvas.setLayoutY(0);
            return;
        }
        Bounds vb = scrollPane.getViewportBounds();
        if (vb == null) return;
        
        double contentH = getPrefHeight();
        double viewportH = vb.getHeight();
        double range = Math.max(0, contentH - viewportH);
        double offsetY = scrollPane.getVvalue() * range;
        
        canvas.setLayoutX(0);
        canvas.setLayoutY(offsetY);
    }
    
    /** Filtra items procesados, manteniendo el orden de la lista original. */
    private void rebuildDisplayList() {
        List<TextureInfo> filtered = new ArrayList<>(items.size());
        for (TextureInfo t : items) {
            if (!t.isProcessed()) filtered.add(t);
        }
        this.displayList = filtered;
        // Limpiar items seleccionados que ya no están visibles.
        selectedSet.retainAll(filtered);
    }
    
    // ==================== REPAINT ====================
    
    /** Estado de petición de repaint para coalescer múltiples cambios en un frame. */
    private boolean repaintRequested = false;
    
    /**
     * Solicita un repaint en el siguiente pulso de animación. Multiples
     * llamadas en el mismo frame se coalescen en una sola.
     */
    private void requestRepaint() {
        if (repaintRequested) return;
        repaintRequested = true;
        javafx.application.Platform.runLater(() -> {
            repaintRequested = false;
            paint();
        });
    }
    
    /**
     * Pinta la franja visible del grid sobre el Canvas.
     */
    private void paint() {
        positionCanvas();
        
        double viewportW = canvas.getWidth();
        double viewportH = canvas.getHeight();
        if (viewportW <= 0 || viewportH <= 0) return;
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        // Nota: GraphicsContext.setImageSmoothing(false) solo existe en
        // JavaFX 12+; en JavaFX 8 las miniaturas ya se decodifican con
        // smooth=false en TextureService.generateThumbnail(), preservando
        // el aspecto pixel-art al hacer drawImage.
        gc.clearRect(0, 0, viewportW, viewportH);
        
        if (displayList.isEmpty() || cols <= 0) return;
        
        double offsetY = canvas.getLayoutY();
        
        // Determinar filas que intersectan el viewport.
        int firstRow = Math.max(0, (int) Math.floor((offsetY - PADDING) / ROW_H));
        int lastRow = (int) Math.ceil((offsetY + viewportH - PADDING) / ROW_H);
        
        int n = displayList.size();
        int totalRows = (n + cols - 1) / cols;
        lastRow = Math.min(lastRow, totalRows - 1);
        
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (idx >= n) return;
                
                double absX = PADDING + col * (CARD_W + H_GAP);
                double absY = PADDING + row * (CARD_H + V_GAP);
                
                double localX = absX;
                double localY = absY - offsetY;
                
                drawCard(gc, displayList.get(idx), idx, localX, localY);
            }
        }
    }
    
    /**
     * Dibuja una card individual.
     */
    private void drawCard(GraphicsContext gc, TextureInfo t, int idx, double x, double y) {
        boolean isHover = (t == hoveredItem);
        boolean isSelected = selectedSet.contains(t);
        boolean isButtonHover = (idx == hoveredButtonIndex);
        
        // Fondo
        Color bg;
        if (isSelected) bg = BG_SELECTED;
        else if (isHover) bg = BG_HOVER;
        else bg = BG_NORMAL;
        gc.setFill(bg);
        gc.fillRoundRect(x, y, CARD_W, CARD_H, 8, 8);
        
        // Borde
        Color border;
        double borderW;
        if (isSelected) {
            border = BORDER_SELECTED;
            borderW = 2;
        } else if (isHover) {
            border = BORDER_HOVER;
            borderW = 1;
        } else {
            border = BORDER_NORMAL;
            borderW = 1;
        }
        gc.setStroke(border);
        gc.setLineWidth(borderW);
        gc.strokeRoundRect(x + 0.5, y + 0.5, CARD_W - 1, CARD_H - 1, 8, 8);
        
        // Miniatura centrada
        double thumbX = x + (CARD_W - THUMB_SIZE) / 2.0;
        double thumbY = y + CARD_PAD;
        Image thumb = t.getThumbnailImage();
        if (thumb != null && !thumb.isError()) {
            // preserveRatio: ajustar al cuadrado THUMB_SIZE
            double iw = thumb.getWidth();
            double ih = thumb.getHeight();
            double scale = (iw <= 0 || ih <= 0) ? 1.0 : Math.min(THUMB_SIZE / iw, THUMB_SIZE / ih);
            double dw = iw * scale;
            double dh = ih * scale;
            double dx = thumbX + (THUMB_SIZE - dw) / 2.0;
            double dy = thumbY + (THUMB_SIZE - dh) / 2.0;
            gc.drawImage(thumb, dx, dy, dw, dh);
        } else {
            // Placeholder
            gc.setFill(PLACEHOLDER_BG);
            gc.fillRect(thumbX, thumbY, THUMB_SIZE, THUMB_SIZE);
            gc.setStroke(PLACEHOLDER_BORDER);
            gc.setLineWidth(1);
            gc.strokeRect(thumbX + 0.5, thumbY + 0.5, THUMB_SIZE - 1, THUMB_SIZE - 1);
            gc.setFill(PLACEHOLDER_TEXT);
            gc.setFont(FONT_PLACEHOLDER);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("?", thumbX + THUMB_SIZE / 2.0, thumbY + THUMB_SIZE / 2.0);
        }
        
        // Nombre (truncado)
        gc.setFill(TEXT_NAME);
        gc.setFont(FONT_NAME);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        String name = truncate(t.getBaseName(), 20);
        gc.fillText(name, x + CARD_W / 2.0, thumbY + THUMB_SIZE + 4);
        
        // Resolución
        gc.setFill(TEXT_INFO);
        gc.setFont(FONT_INFO);
        gc.fillText(t.getResolutionText(), x + CARD_W / 2.0, thumbY + THUMB_SIZE + 4 + 14);
        
        // "Botón" Reducir
        double btnX = x + CARD_PAD;
        double btnY = y + CARD_H - CARD_PAD - BUTTON_H;
        double btnW = CARD_W - 2 * CARD_PAD;
        gc.setFill(isButtonHover ? BUTTON_BG_HOVER : BUTTON_BG);
        gc.fillRoundRect(btnX, btnY, btnW, BUTTON_H, 4, 4);
        gc.setFill(BUTTON_TEXT);
        gc.setFont(FONT_BUTTON);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("Reducir", btnX + btnW / 2.0, btnY + BUTTON_H / 2.0);
    }
    
    /**
     * Trunca un nombre largo agregando "..." al final.
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 3)) + "...";
    }
    
    // ==================== HIT TESTING ====================
    
    /**
     * Convierte coordenadas del Canvas (mouse event) en el índice del item bajo el cursor.
     * Devuelve -1 si no hay item bajo esa posición.
     */
    private int indexAt(double canvasX, double canvasY) {
        if (cols <= 0 || displayList.isEmpty()) return -1;
        
        double offsetY = canvas.getLayoutY();
        double absX = canvasX;
        double absY = canvasY + offsetY;
        
        // Quitar padding
        double localX = absX - PADDING;
        double localY = absY - PADDING;
        if (localX < 0 || localY < 0) return -1;
        
        // Rejilla incluyendo gaps
        double colStride = CARD_W + H_GAP;
        double rowStride = CARD_H + V_GAP;
        
        int col = (int) Math.floor(localX / colStride);
        int row = (int) Math.floor(localY / rowStride);
        if (col >= cols) return -1;
        
        // ¿Cae el punto en la card o en el gap?
        double cellX = localX - col * colStride;
        double cellY = localY - row * rowStride;
        if (cellX > CARD_W || cellY > CARD_H) return -1;
        
        int idx = row * cols + col;
        if (idx < 0 || idx >= displayList.size()) return -1;
        return idx;
    }
    
    /**
     * Determina si un punto del Canvas cae sobre la zona del botón
     * "Reducir" de la card en el índice idx.
     */
    private boolean isInButtonZone(double canvasX, double canvasY, int idx) {
        if (idx < 0) return false;
        int row = idx / cols;
        int col = idx % cols;
        
        double offsetY = canvas.getLayoutY();
        double cardX = PADDING + col * (CARD_W + H_GAP);
        double cardY = PADDING + row * (CARD_H + V_GAP) - offsetY;
        
        double btnX = cardX + CARD_PAD;
        double btnY = cardY + CARD_H - CARD_PAD - BUTTON_H;
        double btnW = CARD_W - 2 * CARD_PAD;
        
        return canvasX >= btnX && canvasX <= btnX + btnW
                && canvasY >= btnY && canvasY <= btnY + BUTTON_H;
    }
    
    // ==================== EVENTOS ====================
    
    private void handleMouseMoved(double x, double y) {
        int idx = indexAt(x, y);
        TextureInfo newHover = (idx >= 0) ? displayList.get(idx) : null;
        int newButtonHover = (idx >= 0 && isInButtonZone(x, y, idx)) ? idx : -1;
        
        if (newHover != hoveredItem || newButtonHover != hoveredButtonIndex) {
            hoveredItem = newHover;
            hoveredButtonIndex = newButtonHover;
            requestRepaint();
        }
    }
    
    private void handleMouseClicked(double x, double y) {
        int idx = indexAt(x, y);
        if (idx < 0) return;
        TextureInfo t = displayList.get(idx);
        
        if (isInButtonZone(x, y, idx)) {
            if (onProcessCallback != null) {
                onProcessCallback.accept(t);
            }
            return;
        }
        
        if (selectionModeActive != null && selectionModeActive.get()) {
            toggleSelection(t);
        }
    }
    
    private void toggleSelection(TextureInfo t) {
        boolean newState;
        if (selectedSet.remove(t)) {
            newState = false;
        } else {
            selectedSet.add(t);
            newState = true;
        }
        t.setSelected(newState);
        requestRepaint();
        fireSelectionChanged();
    }
    
    /** Vacía el conjunto de selección y notifica. */
    private void clearSelection() {
        if (selectedSet.isEmpty()) return;
        for (TextureInfo t : selectedSet) {
            t.setSelected(false);
        }
        selectedSet.clear();
        requestRepaint();
        fireSelectionChanged();
    }
    
    private void fireSelectionChanged() {
        if (onSelectionChangedCallback != null) {
            onSelectionChangedCallback.run();
        }
    }
}
