# Bedrock Texture Resizer 1.2.0

![Bedrock Texture Resizer 1.2.0 interface](https://ik.imagekit.io/dmNtb25nZQ/Bedrock%20Texture%20Resizer/BedrockTextureResizer1.2.0.webp)

## What's new

- Refreshed the interface with a cleaner visual system and consistent styling across controls, dialogs, and status elements.
- Redesigned texture cards with improved spacing, pill-shaped actions, hover feedback, and clearer selection indicators.
- Added a contextual action bar for multi-selection so batch controls remain readable at compact window sizes.
- Renamed the visible application to **Bedrock Texture Resizer**.
- Clarified that **Binary** is the default alpha processing mode.

## Requirements

- Windows
- Java 25

This build is Windows-only because it bundles the Windows-specific JavaFX native libraries.

## Download

Download `iron-1.2.0-windows.jar` executable JAR for Windows with JavaFX bundled.

## Run

You can launch by double-clicking `iron-1.2.0-windows.jar` if `.jar` files are associated with Java on your system.

Alternatively, open PowerShell in the download directory and run:

```powershell
java --enable-native-access=ALL-UNNAMED -jar .\iron-1.2.0-windows.jar
```

The application can also be started without the native-access option, although Java may display a warning:

```powershell
java -jar .\iron-1.2.0-windows.jar
```

Back up your resource pack before processing: resized textures overwrite the
original files in place.
