package util;

/**
 * Enumeración de algoritmos de redimensionamiento disponibles.
 * 
 * Para añadir un nuevo algoritmo:
 * 1. Agregar la entrada aquí con su nombre de display
 * 2. Implementar la lógica en una clase separada (ver {@link service.AreaResampler})
 * 3. Añadir el case correspondiente en {@link service.ImageProcessor#resize}
 * 
 * @author vmonge
 */
public enum ResizeAlgorithm {
    
    /**
     * Area Resampling (Pixel Area Relation).
     * Trata cada píxel como un área 1×1 y calcula la intersección exacta.
     * Usa alfa premultiplicado para mezclar correctamente transparencia.
     * Ideal para reducción de texturas con bordes semitransparentes.
     */
    AREA("Area Resampling");
    
    // ==================== CAMPOS ====================
    
    /** Nombre legible para mostrar en la UI */
    private final String displayName;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * @param displayName Nombre para mostrar en la interfaz
     */
    ResizeAlgorithm(String displayName) {
        this.displayName = displayName;
    }
    
    // ==================== MÉTODOS ====================
    
    /**
     * @return Nombre legible para la interfaz
     */
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
