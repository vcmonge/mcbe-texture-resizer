package util;

/**
 * Clase de constantes para la aplicación de reducción de texturas.
 * Centraliza la configuración para facilitar cambios futuros.
 * 
 * @author vmonge
 */
public final class Constants {
    
    // ==================== CONFIGURACIÓN DE ALGORITMO ====================
    
    /**
     * Algoritmo de interpolación por defecto para redimensionar imágenes.
     * 
     * @see ResizeAlgorithm
     */
    public static final ResizeAlgorithm DEFAULT_ALGORITHM = ResizeAlgorithm.AREA;
    
    /**
     * Factor de reducción por defecto (divide la resolución entre este valor).
     * Por ejemplo: 2 reduce 128x128 a 64x64
     */
    public static final int DEFAULT_SCALE_FACTOR = 2;
    
    /**
     * Si es true, al procesar archivos {@code .tga} con algoritmo AREA se usa
     * {@link service.AreaResampler#resizeIndependentAlpha} (RGB y alpha como
     * canales independientes), que es el comportamiento deseado para la mayoría
     * de TGA en resource packs. La opción ya no se expone en la UI: siempre
     * se aplica este modo para TGA.
     */
    public static final boolean TGA_INDEPENDENT_ALPHA_PROCESSING = true;
    
    // ==================== RUTAS DE TEXTURAS ====================
    
    /**
     * Subdirectorio para texturas de bloques.
     */
    public static final String BLOCKS_PATH = "textures/blocks";
    
    /**
     * Subdirectorio para texturas de items.
     */
    public static final String ITEMS_PATH = "textures/items";
    
    /**
     * Subdirectorio para texturas de entorno (cielo, nubes, etc.).
     */
    public static final String ENVIRONMENT_PATH = "textures/environment";
    
    /**
     * Subdirectorio para texturas de entidades (mobs, jugadores, etc.).
     */
    public static final String ENTITY_PATH = "textures/entity";
    
    // ==================== SUFIJOS DE ARCHIVOS ====================
    
    /**
     * Sufijo para mapas de metallic/emissive/roughness (MER).
     */
    public static final String MER_SUFFIX = "_mer";
    
    /**
     * Sufijo para mapas de normales.
     */
    public static final String NORMAL_SUFFIX = "_normal";
    
    // ==================== EXTENSIONES SOPORTADAS ====================
    
    /**
     * Extensiones de archivo soportadas para texturas.
     */
    public static final String[] SUPPORTED_EXTENSIONS = {".png", ".tga"};
    
    /**
     * Extensión PNG.
     */
    public static final String PNG_EXTENSION = ".png";
    
    /**
     * Extensión TGA.
     */
    public static final String TGA_EXTENSION = ".tga";
    
    // ==================== CONFIGURACIÓN DE UI ====================
    
    /**
     * Ancho del contenedor de textura en el mosaico.
     */
    public static final double TEXTURE_CARD_WIDTH = 150;
    
    /**
     * Alto del contenedor de textura en el mosaico.
     */
    public static final double TEXTURE_CARD_HEIGHT = 180;
    
    /**
     * Tamaño de la miniatura de preview (en el ImageView, en píxeles lógicos).
     */
    public static final double THUMBNAIL_SIZE = 100;
    
    /**
     * Tamaño al que se decodifica/rasteriza la miniatura en memoria.
     * Se usa 2x el tamaño visible para mantener nitidez en pantallas HiDPI
     * sin cargar la imagen completa. Reduce drásticamente el uso de heap
     * cuando hay muchas texturas grandes (1024+).
     */
    public static final int THUMBNAIL_RENDER_SIZE = 200;
    
    /**
     * Número de columnas en el mosaico (0 = auto-ajuste).
     */
    public static final int MOSAIC_COLUMNS = 0;
    
    /**
     * Espaciado horizontal entre tarjetas.
     */
    public static final double CARD_SPACING_H = 10;
    
    /**
     * Espaciado vertical entre tarjetas.
     */
    public static final double CARD_SPACING_V = 10;
    
    // ==================== CONFIGURACIÓN DE HILOS ====================
    
    /**
     * Número de hilos para carga paralela de imágenes.
     * Se recomienda usar el número de núcleos del procesador.
     */
    public static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    
    // ==================== CONSTRUCTOR PRIVADO ====================
    
    /**
     * Constructor privado para evitar instanciación.
     */
    private Constants() {
        throw new UnsupportedOperationException("Clase de constantes - no instanciar");
    }
}
