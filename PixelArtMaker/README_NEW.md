# Pixel Art Maker (Java Swing)

Simple Pixel Art Maker written in plain Java (Swing). Features:

- Paint on a pixel grid (default 32x32).
- Change pixel size with slider or settings.
- **Undo/Redo** functionality (full history).
- **Custom palette system** – Edit, add, rename colors. Right-click to edit color properties. Grid layout 4x4 on right panel.
- Preset color palette (Black, White, Red, Green, Blue, Yellow, Orange, Magenta).
- **Adjustable background** color (White, Black, or **Transparent** for PNG export with alpha).
- Save artwork as PNG (with transparency support).
- Load PNG (scaled to current grid).
- Export / Import project file (.pam) that stores grid colors and pixel size.
- **Animation Editor** – Create multi-frame animations:
  - Add/remove frames
  - Set delay per frame (ms)
  - **Onion Skin** feature: Preview previous frame with blur for easy frame progression
  - Play animation preview
  - Export animation (GIF coming soon)

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

### Palette System (Right Panel - Grid Layout)
- **Colored buttons (4x4 grid)**: Click to select color for painting.
- **Right-click on color**: Edit color name and/or color value.
- **+ button**: Add new custom color (name + color picker).
- **Color info**: Shows currently selected color name at bottom.

### Tools & Features (Top Bar)
- **New**: Clear entire canvas.
- **Undo/Redo**: Navigate through your painting history (buttons auto-enable/disable).
- **Save PNG**: Export canvas as raster image (PNG).
  - **White background** – PNG with white background.
  - **Black background** – PNG with black background.
  - **Transparent background** – PNG with full alpha channel (transparent areas = no pixels).
- **Load PNG**: Import image and resize to current grid.
- **Export Project / Import Project**: Save/load project file (.pam format).
- **Settings**: Change grid columns/rows, pixel size, and **background color** (white/black/transparent).
- **Animation**: Open animation editor (see below).
- **Pixel size slider**: Adjust pixel size on-the-fly (4–64).

### Animation Editor

Click **Animation** button to open the Animation Editor:

1. **Frame List** – Shows all frames in the animation.
2. **+ Button** – Add new frame (copies current canvas).
3. **- Button** – Remove selected frame.
4. **Frame Delay** – Set delay in milliseconds for each frame.
5. **Onion Skin** – Enable/disable preview of previous frame:
   - When enabled, previous frame shows with transparency/blur effect.
   - Helps guide next frame drawing.
   - **Onion Alpha** – Control opacity of preview (10–255).
6. **Play** – Preview animation in real-time.
7. **Export GIF** – Coming soon!

### Onion Skin Feature
- When drawing a new frame with Onion Skin enabled, the previous frame appears semi-transparently.
- Adjust **Onion Alpha** slider to control how visible the preview is.
- Great for creating smooth animations and transitions.

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
- **Custom Palette**: Build your own color set with custom names (grid 4x4).
- **Dynamic Grid Resize**: Change canvas dimensions while preserving existing artwork.
- **Animation System**: Multi-frame animations with configurable delays.
- **Onion Skin**: Preview previous frames while drawing for smooth animation progression.

---

Fitur tambahan yang bisa ditambah:
- Brush shapes (circle, square, line tool).
- Layer system.
- GIF export.
- Save/load custom palettes.
- Build script for executable JAR.

Hubungi jika butuh feature tambahan!
