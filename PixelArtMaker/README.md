# Pixel Art Maker (Java Swing)

Simple Pixel Art Maker written in plain Java (Swing). Features:

- Paint on a pixel grid (default 32x32).
- Change pixel size with slider or settings.
- **Undo/Redo** functionality (full history).
- **Custom palette system** – Edit, add, rename colors. Right-click to edit color properties.
- Preset color palette (Black, White, Red, Green, Blue, Yellow, Orange, Magenta).
- **Adjustable background** color (White, Black, or **Transparent** for PNG export with alpha).
- Save artwork as PNG (with transparency support).
- Load PNG (scaled to current grid).
- Export / Import project file (.pam) that stores grid colors and pixel size.

## Requirements
- Java 8 or newer (JDK)

## Compile & Run (PowerShell)

```powershell
cd "C:\Users\Muhammad Zidane A\Documents\Code\Java\PixelArtMaker\src"
javac PixelArtMaker.java -d ..\out
cd ..\out
java PixelArtMaker
```

## Usage Notes

### Drawing
- **Left-click** to paint with current color.
- **Right-click** to erase (set pixel to background).
- **Drag** to paint multiple pixels.

### Palette System (Right Panel)
- **Colored buttons**: Click to select color for painting.
- **Right-click on color**: Edit color name and/or color value.
- **+ button**: Add new custom color (name + color picker).
- **Color info**: Shows currently selected color name at bottom.

### Tools & Features
- **New**: Clear entire canvas.
- **Undo/Redo**: Navigate through your painting history (buttons auto-enable/disable).
- **Save PNG**: Export canvas as raster image (PNG).
  - **White background** – PNG with white background.
  - **Black background** – PNG with black background.
  - **Transparent background** – PNG with full alpha channel (transparent areas = no pixels).
- **Load PNG**: Import image and resize to current grid.
- **Export Project / Import Project**: Save/load project file (.pam format).
- **Settings**: Change grid columns/rows, pixel size, and **background color** (white/black/transparent).
- **Pixel size slider**: Adjust pixel size on-the-fly (4–64).

### Preset Colors
Quick buttons for: Black, White, Red, Green, Blue, Yellow, Orange, Magenta. Each can be clicked or right-clicked to edit.

### Custom Colors
- Add new colors via **+** button in palette.
- Edit existing colors (right-click) to rename and change color value.
- Custom colors are lost when app closes (can save as project .pam to preserve).

### .pam Format (Export)
Plain-text project format:
```
<cols> <rows> <pixelSize>
<hex color 1> <hex color 2> ... (per column)
...
```

Example:
```
32 32 16
#000000 #ffffff #ff0000 ...
...
```

## Advanced Features

- **Undo/Redo**: Full history stack (every brush stroke saved).
- **Transparent Export**: PNG files with true alpha channel for transparent pixels.
- **Custom Palette**: Build your own color set with custom names.
- **Dynamic Grid Resize**: Change canvas dimensions while preserving existing artwork.

---

Fitur tambahan yang bisa ditambah:
- Brush shapes (circle, square, line tool).
- Layer system.
- Animation preview.
- Spritesheet export.
- Save custom palette to file.
- Build script for executable JAR.

Hubungi jika butuh feature tambahan!
