# Herramienta para redimensionar paquetes de texturas para Minecraft Bedrock.

## Funcionamiento

1. Se elige la **carpeta raíz** del resource pack.
2. La aplicación escanea subcarpetas y muestra las texturas en un mosaico organizado por pestañas.
3. Cada imagen del mosaico tiene un botón **Reducir** en cada entrada para aplicar la reducción con el factor y el algoritmo configurados. Las miniaturas y datos provienen de los archivos reales del disco.

Las salidas sobrescriben los archivos de imagen en las rutas correspondientes (hacer un respaldo antes de procesar).

## Características

| Aspecto | Detalle |
|--------|---------|
| **Formatos** | PNG y TGA (incluye canal alpha) |
| **Rutas escaneadas** | `textures/blocks`, `textures/items`, `textures/environment`, `textures/entity` |
| **Texturas relacionadas** | Detecta y procesa junto a la base los sufijos `_mer` y `_normal` cuando existen |
| **Algoritmo** | Por defecto *Area Resampling* (relación de áreas entre píxeles), pensado para bajar resolución con mejor comportamiento en alpha que el escalado trivial |
| **TGA** | Los `.tga` se procesan con **RGB y alpha como canales independientes** (comportamiento fijo del proyecto, adecuado a la mayoría de TGA en resource packs) |
| **Factores de escala** | Dividir tamaño entre 2, 4, 8 o 16 (configurable en la barra superior) |
| **Selección múltiple** | Modo para elegir varias texturas en la pestaña activa y disparar la reducción en conjunto |
| **Ordenación** | Por resolución (ascendente/descendente) según el combo “Ordenar por” |
| **Interfaz** | Vista tipo cuadrícula dibujada con **Canvas** por pestaña para mantener la aplicación fluida con miles de texturas |

## Requisitos

- **JDK 8** con **JavaFX** (proyecto NetBeans tipo JavaFX, clase principal declarada como `view.Iron` en la configuración del proyecto).

## Cómo ejecutar

- Abrir el proyecto en **Apache NetBeans** y ejecuta con **Run** sobre la aplicación JavaFX (por ejemplo **Clean and Build** y luego **Run**).

## Estructura del código (resumen)

- `src/view/` — UI (`Iron.java`, `FXMLInicio.fxml`, `CanvasTextureGrid.java`, estilos)
- `src/controller/` — `FXMLInicioController.java`
- `src/service/` — Carga y proceso (`TextureService`, `ImageProcessor`, `AreaResampler`)
- `src/model/` — `TextureInfo`
- `src/util/` — Constantes, TGA, algoritmos

