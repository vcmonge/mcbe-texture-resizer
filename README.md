# Iron — Texture Resizer for Minecraft Bedrock

A lightweight JavaFX desktop tool for batch-resizing textures in **Minecraft Bedrock Edition** resource packs. Point it at a resource pack folder, preview every texture on a fast canvas-based grid, and downscale individual or bulk-selected textures with a single click.

## How It Works

1. **Select** the resource pack's root folder using the **Browse…** button.
2. The app scans the standard `textures/` subdirectories and displays every texture in a tabbed mosaic (Blocks, Items, Environment, Entity).
3. **Resize** textures one by one via the per-card **Resize** button, or enable **Selection Mode** to batch-process multiple textures at once.

> **⚠ Warning:** Processed images overwrite the originals in place — back up your resource pack before running the tool.

## Features

| Feature | Details |
|---------|---------|
| **Supported Formats** | PNG and TGA (including alpha channel) |
| **Scanned Paths** | `textures/blocks`, `textures/items`, `textures/environment`, `textures/entity` |
| **Related Textures** | Automatically detects and resizes companion `_mer` and `_normal` maps alongside the base texture |
| **Default Algorithm** | *Area Resampling* (pixel-area relation) — produces cleaner results on semi-transparent edges than naive downscaling |
| **TGA Handling** | RGB and alpha channels are processed independently (fixed behavior, suited for most resource-pack TGAs) |
| **Scale Factors** | Divide dimensions by 2, 4, 8, or 16 (selectable in the toolbar) |
| **Batch Selection** | Toggle Selection Mode to pick multiple textures in the active tab and process them all at once |
| **Sorting** | Sort the grid by resolution (ascending or descending) |
| **Performance** | Each tab renders its grid on a single `Canvas` node — the UI stays responsive even with thousands of textures |

## Requirements

- **JDK 8** with **JavaFX** (NetBeans JavaFX project; main class: `view.Iron`).

## Running

1. Open the project in **Apache NetBeans**.
2. **Clean and Build**, then **Run**.

## Project Structure

| Path | Contents |
|------|----------|
| `src/view/` | UI — `Iron.java`, `FXMLInicio.fxml`, `CanvasTextureGrid.java`, stylesheets |
| `src/controller/` | `FXMLInicioController.java` |
| `src/service/` | Texture loading & processing (`TextureService`, `ImageProcessor`, `AreaResampler`) |
| `src/model/` | `TextureInfo` |
| `src/util/` | Constants, TGA utilities, algorithm definitions |
