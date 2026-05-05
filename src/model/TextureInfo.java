package model;

import java.io.File;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * Modelo que representa una textura y su información asociada.
 * Incluye referencias a texturas adicionales (_mer, _normal) si existen.
 * 
 * Implementa propiedades observables de JavaFX para binding con la UI.
 * 
 * @author vmonge
 */
public class TextureInfo {
    
    // ==================== ARCHIVOS ====================
    
    /** Archivo de la textura base */
    private final File baseFile;
    
    /** Archivo de la textura MER (metallic/emissive/roughness), puede ser null */
    private File merFile;
    
    /** Archivo de la textura de normales, puede ser null */
    private File normalFile;
    
    // ==================== PROPIEDADES DE IMAGEN ====================
    
    /** Ancho de la imagen en píxeles */
    private final int width;
    
    /** Alto de la imagen en píxeles */
    private final int height;
    
    /** Imagen cargada para preview (miniatura) */
    private Image thumbnailImage;
    
    // ==================== PROPIEDADES OBSERVABLES ====================
    
    /** Propiedad observable para el texto de resolución */
    private final StringProperty resolutionText;
    
    /** Propiedad observable para indicar si ya fue procesada */
    private final BooleanProperty processed;
    
    /**
     * Propiedad observable de selección (modo selección múltiple).
     * Vive en el modelo (no en la vista) para que sobreviva al
     * reciclado/repintado del CanvasTextureGrid.
     */
    private final BooleanProperty selected;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Crea una nueva instancia de TextureInfo.
     * 
     * @param baseFile Archivo de la textura base (no null)
     * @param width Ancho de la imagen en píxeles
     * @param height Alto de la imagen en píxeles
     */
    public TextureInfo(File baseFile, int width, int height) {
        if (baseFile == null) {
            throw new IllegalArgumentException("El archivo base no puede ser null");
        }
        
        this.baseFile = baseFile;
        this.width = width;
        this.height = height;
        this.merFile = null;
        this.normalFile = null;
        this.thumbnailImage = null;
        
        // Inicializar propiedades observables
        this.resolutionText = new SimpleStringProperty(buildResolutionText());
        this.processed = new SimpleBooleanProperty(false);
        this.selected = new SimpleBooleanProperty(false);
    }
    
    // ==================== MÉTODOS DE CONSTRUCCIÓN DE TEXTO ====================
    
    /**
     * Construye el texto de resolución incluyendo indicadores de texturas adicionales.
     * Formato: "WIDTHxHEIGHT [M][N]"
     * 
     * @return Texto formateado con la resolución y marcadores
     */
    private String buildResolutionText() {
        StringBuilder sb = new StringBuilder();
        sb.append(width).append("x").append(height);
        
        if (merFile != null) {
            sb.append(" [M]");
        }
        if (normalFile != null) {
            sb.append(" [N]");
        }
        
        return sb.toString();
    }
    
    /**
     * Actualiza el texto de resolución (llamar después de cambiar merFile o normalFile).
     */
    public void updateResolutionText() {
        resolutionText.set(buildResolutionText());
    }
    
    // ==================== GETTERS Y SETTERS ====================
    
    /**
     * @return Archivo de la textura base
     */
    public File getBaseFile() {
        return baseFile;
    }
    
    /**
     * @return Nombre del archivo sin extensión
     */
    public String getBaseName() {
        String name = baseFile.getName();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }
    
    /**
     * @return Extensión del archivo incluyendo el punto (ej: ".png")
     */
    public String getExtension() {
        String name = baseFile.getName();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(dotIndex).toLowerCase() : "";
    }
    
    /**
     * @return Archivo de la textura MER o null si no existe
     */
    public File getMerFile() {
        return merFile;
    }
    
    /**
     * Establece el archivo de textura MER.
     * 
     * @param merFile Archivo MER o null
     */
    public void setMerFile(File merFile) {
        this.merFile = merFile;
        updateResolutionText();
    }
    
    /**
     * @return true si tiene textura MER asociada
     */
    public boolean hasMer() {
        return merFile != null;
    }
    
    /**
     * @return Archivo de la textura de normales o null si no existe
     */
    public File getNormalFile() {
        return normalFile;
    }
    
    /**
     * Establece el archivo de textura de normales.
     * 
     * @param normalFile Archivo de normales o null
     */
    public void setNormalFile(File normalFile) {
        this.normalFile = normalFile;
        updateResolutionText();
    }
    
    /**
     * @return true si tiene textura de normales asociada
     */
    public boolean hasNormal() {
        return normalFile != null;
    }
    
    /**
     * @return Ancho de la imagen en píxeles
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * @return Alto de la imagen en píxeles
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * @return Imagen miniatura para preview
     */
    public Image getThumbnailImage() {
        return thumbnailImage;
    }
    
    /**
     * Establece la imagen miniatura.
     * 
     * @param thumbnailImage Imagen de preview
     */
    public void setThumbnailImage(Image thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
    }
    
    // ==================== PROPIEDADES OBSERVABLES ====================
    
    /**
     * @return Propiedad de texto de resolución para binding
     */
    public StringProperty resolutionTextProperty() {
        return resolutionText;
    }
    
    /**
     * @return Texto de resolución actual
     */
    public String getResolutionText() {
        return resolutionText.get();
    }
    
    /**
     * @return Propiedad de estado procesado para binding
     */
    public BooleanProperty processedProperty() {
        return processed;
    }
    
    /**
     * @return true si la textura ya fue procesada
     */
    public boolean isProcessed() {
        return processed.get();
    }
    
    /**
     * Marca la textura como procesada.
     * 
     * @param processed Estado de procesamiento
     */
    public void setProcessed(boolean processed) {
        this.processed.set(processed);
    }
    
    /**
     * @return Propiedad de selección para binding
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    /**
     * @return true si la textura está seleccionada (modo selección múltiple)
     */
    public boolean isSelected() {
        return selected.get();
    }
    
    /**
     * Cambia el estado de selección de la textura.
     * 
     * @param selected true para seleccionar, false para deseleccionar
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * @return Directorio padre del archivo base
     */
    public File getParentDirectory() {
        return baseFile.getParentFile();
    }
    
    /**
     * @return Ruta completa del archivo base
     */
    public String getFullPath() {
        return baseFile.getAbsolutePath();
    }
    
    @Override
    public String toString() {
        return String.format("TextureInfo[%s, %dx%d, MER=%b, Normal=%b]",
                baseFile.getName(), width, height, hasMer(), hasNormal());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TextureInfo that = (TextureInfo) obj;
        return baseFile.equals(that.baseFile);
    }
    
    @Override
    public int hashCode() {
        return baseFile.hashCode();
    }
}
