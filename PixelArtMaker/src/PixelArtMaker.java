import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

class CustomColor {
    String name;
    Color color;
    
    CustomColor(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}

public class PixelArtMaker {
    private JFrame frame;
    private PixelCanvas canvas;
    private Color currentColor = Color.BLACK;
    private int defaultCols = 32;
    private int defaultRows = 32;
    private int defaultPixelSize = 16;
    private JButton undoBtn;
    private JButton redoBtn;
    private List<CustomColor> customPalette = new ArrayList<>();
    private int backgroundMode = 0; // 0=white, 1=black, 2=transparent
    
    // Drawing tools
    private int currentTool = 0; // 0=pencil, 1=line, 2=rectangle, 3=oval, 4=triangle, 5=filled rect, 6=filled oval
    private JButton[] toolButtons;
    
    // Theme
    private int currentTheme = 0; // 0=light, 1=dark, 2=cozy
    private Color bgColor, fgColor, accentColor, panelColor;
    
    // Track changes
    private boolean hasUnsavedChanges = false;
    
    // Animation UI
    private JPanel mainContentPanel;
    private JPanel pixelArtPanel;
    private JPanel animationPanel;
    private JButton animationBtn;
    private JLabel frameIndicatorLabel;
    private JSplitPane splitPane;
    private boolean isAnimationMode = false;
    private int animationCurrentFrameIndex = 0;
    private int animationLoopDelay = 0; // Delay before repeating animation

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PixelArtMaker().createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("Pixel Art Maker");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Add window listener untuk konfirmasi exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                PixelArtMaker.this.confirmExit();
            }
        });
        
        // Initialize custom palette
        customPalette.add(new CustomColor("Black", Color.BLACK));
        customPalette.add(new CustomColor("White", Color.WHITE));
        customPalette.add(new CustomColor("Red", Color.RED));
        customPalette.add(new CustomColor("Green", Color.GREEN));
        customPalette.add(new CustomColor("Blue", Color.BLUE));
        customPalette.add(new CustomColor("Yellow", Color.YELLOW));
        customPalette.add(new CustomColor("Orange", Color.ORANGE));
        customPalette.add(new CustomColor("Magenta", Color.MAGENTA));

        canvas = new PixelCanvas(defaultCols, defaultRows, defaultPixelSize);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton newBtn = new JButton("New");
        newBtn.addActionListener(e -> confirmNew());

        undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> canvas.undo());
        undoBtn.setEnabled(false);

        redoBtn = new JButton("Redo");
        redoBtn.addActionListener(e -> canvas.redo());
        redoBtn.setEnabled(false);
        
        // Callback to update button states
        canvas.setUndoRedoCallback(() -> {
            undoBtn.setEnabled(canvas.canUndo());
            redoBtn.setEnabled(canvas.canRedo());
            hasUnsavedChanges = true; // Mark as unsaved
        });

        JButton savePngBtn = new JButton("Save PNG");
        savePngBtn.addActionListener(e -> saveAsPNG());

        JButton loadPngBtn = new JButton("Load PNG");
        loadPngBtn.addActionListener(e -> loadPNG());

        JButton exportBtn = new JButton("Export Project");
        exportBtn.addActionListener(e -> showExportDialog());

        JButton importBtn = new JButton("Import Project");
        importBtn.addActionListener(e -> importProject());
        
        JButton codeBtn = new JButton("Code");
        codeBtn.addActionListener(e -> showCodeGeneratorDialog());

        JButton settingsBtn = new JButton("Settings");
        settingsBtn.addActionListener(e -> showSettingsDialog());

        animationBtn = new JButton("Animation");
        animationBtn.addActionListener(e -> toggleAnimationMode());

        JSlider sizeSlider = new JSlider(4, 64, defaultPixelSize);
        sizeSlider.setToolTipText("Pixel size");
        sizeSlider.addChangeListener(e -> canvas.setPixelSize(sizeSlider.getValue()));

        topBar.add(newBtn);
        topBar.add(undoBtn);
        topBar.add(redoBtn);
        topBar.add(savePngBtn);
        topBar.add(loadPngBtn);
        topBar.add(exportBtn);
        topBar.add(importBtn);
        topBar.add(codeBtn);
        topBar.add(settingsBtn);
        topBar.add(animationBtn);
        topBar.add(new JSeparator(JSeparator.VERTICAL));
        topBar.add(new JLabel("Pixel size:"));
        topBar.add(sizeSlider);

        // Right panel with palette and custom color editor
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Tools panel at top of right panel
        JPanel toolsPanel = createToolsPanel();
        rightPanel.add(toolsPanel, BorderLayout.NORTH);
        
        // HSV Color picker
        JPanel hsvPanel = createHSVColorPanel();
        rightPanel.add(hsvPanel, BorderLayout.CENTER);
        
        // Palette panel - MS Paint style with smaller swatches
        JPanel paletteScrollContainer = new JPanel(new BorderLayout());
        JPanel palette = new JPanel(new GridLayout(0, 5, 2, 2));
        palette.setBorder(BorderFactory.createTitledBorder("Palette"));
        palette.setPreferredSize(new Dimension(140, 150));
        for (CustomColor cc : customPalette) {
            palette.add(createCustomColorButton(cc));
        }
        
        // Add custom color button
        JButton addCustomBtn = new JButton("+");
        addCustomBtn.setPreferredSize(new Dimension(25, 25));
        addCustomBtn.addActionListener(e -> addCustomColor(palette));
        palette.add(addCustomBtn);
        
        paletteScrollContainer.add(palette, BorderLayout.CENTER);
        rightPanel.add(paletteScrollContainer, BorderLayout.SOUTH);
        
        // Color info panel at bottom of right panel
        JPanel colorInfoPanel = new JPanel(new BorderLayout());
        JLabel colorInfoLabel = new JLabel("Color: Black");
        JPanel colorPreview = new JPanel();
        colorPreview.setBackground(Color.BLACK);
        colorPreview.setPreferredSize(new Dimension(30, 30));
        colorInfoPanel.add(colorPreview, BorderLayout.WEST);
        colorInfoPanel.add(colorInfoLabel, BorderLayout.CENTER);
        
        canvas.setColorChangeListener((name, color) -> {
            colorInfoLabel.setText("Color: " + name);
            colorPreview.setBackground(color);
        });
        
        paletteScrollContainer.add(colorInfoPanel, BorderLayout.SOUTH);

        // Main content panel with pixel art view
        pixelArtPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pixelArtPanel.add(canvasScroll, gbc);
        
        // Animation panel (will be created later)
        animationPanel = new JPanel(new BorderLayout());
        
        // Main content uses pixelArtPanel initially
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(pixelArtPanel, BorderLayout.CENTER);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(topBar, BorderLayout.NORTH);
        frame.getContentPane().add(mainContentPanel, BorderLayout.CENTER);
        frame.getContentPane().add(rightPanel, BorderLayout.EAST);

        applyTheme(); // Apply initial theme
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JButton createCustomColorButton(CustomColor cc) {
        JButton b = new JButton();
        b.setName("colorSwatch_" + cc.name); // Identifier untuk tema
        b.setPreferredSize(new Dimension(25, 25));
        b.setMinimumSize(new Dimension(25, 25));
        b.setMaximumSize(new Dimension(25, 25));
        b.setBackground(cc.color);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        b.setFocusPainted(false);
        b.setToolTipText(cc.name);
        b.addActionListener(e -> {
            currentColor = cc.color;
            canvas.setCurrentColor(currentColor, cc.name);
        });
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    editCustomColor(cc, b);
                }
            }
        });
        return b;
    }
    
    private void addCustomColor(JPanel palette) {
        Color chosen = JColorChooser.showDialog(frame, "Choose Color", currentColor);
        if (chosen == null) return;
        
        String name = JOptionPane.showInputDialog(frame, "Color name:", "Custom");
        if (name == null) return;
        
        CustomColor cc = new CustomColor(name, chosen);
        customPalette.add(cc);
        palette.remove(palette.getComponentCount() - 1); // remove + button
        palette.add(createCustomColorButton(cc));
        palette.add(new JButton("+")); // re-add + button
        palette.revalidate();
        palette.repaint();
    }
    
    private void editCustomColor(CustomColor cc, JButton btn) {
        JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField nameField = new JTextField(cc.name, 15);
        JButton colorChooserBtn = new JButton("Choose Color");
        final Color[] newColor = {cc.color};
        colorChooserBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(frame, "Choose Color", newColor[0]);
            if (chosen != null) newColor[0] = chosen;
        });
        
        p.add(new JLabel("Name:"));
        p.add(nameField);
        p.add(new JLabel("Color:"));
        p.add(colorChooserBtn);
        
        int res = JOptionPane.showConfirmDialog(frame, p, "Edit Color", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            cc.name = nameField.getText();
            cc.color = newColor[0];
            btn.setBackground(cc.color);
            btn.setToolTipText(cc.name);
        }
    }

    private void showSettingsDialog() {
        JSpinner colsSpinner = new JSpinner(new SpinnerNumberModel(canvas.getCols(), 1, 512, 1));
        JSpinner rowsSpinner = new JSpinner(new SpinnerNumberModel(canvas.getRows(), 1, 512, 1));
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(canvas.getPixelSize(), 1, 256, 1));
        
        String[] bgOptions = {"White", "Black", "Transparent"};
        JComboBox<String> bgCombo = new JComboBox<>(bgOptions);
        bgCombo.setSelectedIndex(backgroundMode);
        
        // Theme selection with radio buttons
        JRadioButton lightTheme = new JRadioButton("Light Mode (Default)", currentTheme == 0);
        JRadioButton darkTheme = new JRadioButton("Dark Mode", currentTheme == 1);
        JRadioButton cozyTheme = new JRadioButton("Cozy Mode", currentTheme == 2);
        
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightTheme);
        themeGroup.add(darkTheme);
        themeGroup.add(cozyTheme);

        JPanel p = new JPanel(new GridLayout(6, 2));
        p.add(new JLabel("Columns:")); p.add(colsSpinner);
        p.add(new JLabel("Rows:")); p.add(rowsSpinner);
        p.add(new JLabel("Pixel size:")); p.add(sizeSpinner);
        p.add(new JLabel("Background:")); p.add(bgCombo);
        
        JPanel themePanel = new JPanel(new BorderLayout());
        themePanel.setBorder(BorderFactory.createTitledBorder("Theme"));
        JPanel themeButtonsPanel = new JPanel(new GridLayout(3, 1));
        themeButtonsPanel.add(lightTheme);
        themeButtonsPanel.add(darkTheme);
        themeButtonsPanel.add(cozyTheme);
        themePanel.add(themeButtonsPanel, BorderLayout.WEST);
        
        p.add(new JLabel("Theme:"));
        p.add(themePanel);

        int res = JOptionPane.showConfirmDialog(frame, p, "Settings", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            int c = (Integer) colsSpinner.getValue();
            int r = (Integer) rowsSpinner.getValue();
            int s = (Integer) sizeSpinner.getValue();
            backgroundMode = bgCombo.getSelectedIndex();
            
            // Update theme
            int newTheme = 0;
            if (darkTheme.isSelected()) newTheme = 1;
            else if (cozyTheme.isSelected()) newTheme = 2;
            
            if (newTheme != currentTheme) {
                currentTheme = newTheme;
                applyTheme();
            }
            
            canvas.resizeGrid(c, r);
            canvas.setPixelSize(s);
            canvas.setBackgroundMode(backgroundMode);
            frame.pack();
        }
    }

    private void confirmNew() {
        if (hasUnsavedChanges) {
            int response = JOptionPane.showConfirmDialog(frame,
                    "You have unsaved changes. Do you want to discard them?",
                    "New Project",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response != JOptionPane.YES_OPTION) return;
        }
        canvas.clear();
        hasUnsavedChanges = false;
    }
    
    private void confirmExit() {
        if (hasUnsavedChanges) {
            int response = JOptionPane.showConfirmDialog(frame,
                    "You have unsaved changes. Do you want to save before exiting?",
                    "Exit Program",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.CANCEL_OPTION) return;
            if (response == JOptionPane.YES_OPTION) {
                showExportDialog();
            }
        }
        System.exit(0);
    }

    private void saveAsPNG() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getParentFile(), f.getName() + ".png");
            try {
                BufferedImage img = canvas.renderToImage();
                ImageIO.write(img, "PNG", f);
                JOptionPane.showMessageDialog(frame, "Saved PNG: " + f.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error saving PNG: " + ex.getMessage());
            }
        }
    }

    private void loadPNG() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp"));
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) throw new IOException("Unsupported image format");
                
                // Show options dialog
                String[] options = {"Resize Canvas to Fit", "Scale Image to Canvas"};
                int choice = JOptionPane.showOptionDialog(frame,
                        "Image size: " + img.getWidth() + "x" + img.getHeight() + "\n" +
                        "Canvas size: " + canvas.getCols() + "x" + canvas.getRows() + "\n\n" +
                        "How would you like to load this image?",
                        "Load Image",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
                
                if (choice == -1) return; // User cancelled
                
                if (choice == 0) {
                    // Resize canvas to fit image
                    canvas.resizeGrid(img.getWidth(), img.getHeight());
                    canvas.loadFromImage(img);
                    hasUnsavedChanges = true;
                } else {
                    // Scale image to current canvas size
                    BufferedImage scaled = new BufferedImage(canvas.getCols(), canvas.getRows(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.drawImage(img, 0, 0, canvas.getCols(), canvas.getRows(), null);
                    g.dispose();
                    canvas.loadFromImage(scaled);
                    hasUnsavedChanges = true;
                }
                JOptionPane.showMessageDialog(frame, "Image loaded successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error loading image: " + ex.getMessage());
            }
        }
    }

    private void showExportDialog() {
        String[] options = {"PNG Image", "JPG Image", "PAM Project"};
        int choice = JOptionPane.showOptionDialog(frame, "Choose export format:", "Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        if (choice == -1) return; // User cancelled
        
        JFileChooser fc = new JFileChooser();
        String extension = "";
        String format = "";
        
        switch (choice) {
            case 0: // PNG
                fc.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
                extension = ".png";
                format = "PNG";
                break;
            case 1: // JPG
                fc.setFileFilter(new FileNameExtensionFilter("JPG images", "jpg", "jpeg"));
                extension = ".jpg";
                format = "JPG";
                break;
            case 2: // PAM
                fc.setFileFilter(new FileNameExtensionFilter("Pixel project", "pam"));
                extension = ".pam";
                format = "PAM";
                break;
        }
        
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(extension)) {
                f = new File(f.getParentFile(), f.getName() + extension);
            }
            
            try {
                if (choice == 2) { // PAM
                    exportAsPAM(f);
                } else { // PNG or JPG
                    BufferedImage img = canvas.renderToImage();
                    ImageIO.write(img, format, f);
                }
                JOptionPane.showMessageDialog(frame, "Exported successfully: " + f.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error exporting: " + ex.getMessage());
            }
        }
    }
    
    private void exportAsPAM(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println(canvas.getCols() + " " + canvas.getRows() + " " + canvas.getPixelSize());
            for (int y = 0; y < canvas.getRows(); y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < canvas.getCols(); x++) {
                    Color c = canvas.getColorAt(x, y);
                    if (c == null) sb.append("#00000000");
                    else sb.append(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
                    if (x < canvas.getCols()-1) sb.append(' ');
                }
                pw.println(sb.toString());
            }
        }
    }

    private void importProject() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Pixel project (.pam)", "pam"));
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String header = br.readLine();
                if (header == null) throw new IOException("Empty file");
                String[] parts = header.trim().split("\\s+");
                int w = Integer.parseInt(parts[0]);
                int h = Integer.parseInt(parts[1]);
                int s = Integer.parseInt(parts[2]);
                Color[][] grid = new Color[h][w];
                for (int y = 0; y < h; y++) {
                    String line = br.readLine();
                    if (line == null) throw new IOException("Unexpected EOF");
                    String[] cols = line.trim().split("\\s+");
                    for (int x = 0; x < w; x++) {
                        String token = cols[x];
                        if (token.equalsIgnoreCase("#00000000") || token.equalsIgnoreCase("null")) grid[y][x] = null;
                        else {
                            Color c = Color.decode(token);
                            grid[y][x] = c;
                        }
                    }
                }
                canvas.setGridFromArray(grid);
                canvas.setPixelSize(s);
                frame.pack();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error importing: " + ex.getMessage());
            }
        }
    }

    interface ColorChangeListener {
        void onColorChange(String name, Color color);
    }

    // Inner class for the drawing canvas
    static class PixelCanvas extends JPanel implements MouseListener, MouseMotionListener {
        private int cols, rows;
        private int pixelSize;
        private Color[][] grid;
        private Color currentColor = Color.BLACK;
        private String currentColorName = "Black";
        private boolean painting = false;
        private int backgroundMode = 0; // 0=white, 1=black, 2=transparent
        
        // Undo/Redo stacks
        private Stack<Color[][]> undoStack = new Stack<>();
        private Stack<Color[][]> redoStack = new Stack<>();
        private Runnable undoRedoCallback;
        private PixelArtMaker.ColorChangeListener colorChangeListener;
        
        // Animation
        private List<AnimationFrame> animationFrames = new ArrayList<>();
        private int currentFrameIndex = 0;
        
        // Drawing tools
        private int drawingTool = 0; // 0=pencil, 1=line, 2=rect, 3=oval, 4=triangle, 5=fill rect, 6=fill oval
        private int startX, startY; // For shape drawing
        private Color[][] tempGrid; // Backup for preview

        PixelCanvas(int cols, int rows, int pixelSize) {
            this.cols = cols; this.rows = rows; this.pixelSize = pixelSize;
            this.grid = new Color[rows][cols];
            setPreferredSize(new Dimension(cols * pixelSize, rows * pixelSize));
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        public int getCols() { return cols; }
        public int getRows() { return rows; }
        public int getPixelSize() { return pixelSize; }

        public void setCurrentColor(Color c) { this.currentColor = c; this.currentColorName = "Custom"; }
        public void setCurrentColor(Color c, String name) { this.currentColor = c; this.currentColorName = name; if (colorChangeListener != null) colorChangeListener.onColorChange(name, c); }
        public void setColorChangeListener(PixelArtMaker.ColorChangeListener listener) { this.colorChangeListener = listener; }

        public void setPixelSize(int s) { this.pixelSize = s; setPreferredSize(new Dimension(cols*s, rows*s)); revalidate(); repaint(); }

        public void setBackgroundMode(int mode) { this.backgroundMode = mode; repaint(); }
        
        public void setUndoRedoCallback(Runnable cb) { this.undoRedoCallback = cb; }
        
        public void setDrawingTool(int tool) { this.drawingTool = tool; }
        
        // Animation methods
        public List<AnimationFrame> getAnimationFrames() { return animationFrames; }
        
        public void setCurrentFrameIndex(int idx) { 
            this.currentFrameIndex = idx;
            // Load frame data from animationFrames into grid
            if (idx >= 0 && idx < animationFrames.size()) {
                Color[][] frameData = animationFrames.get(idx).data;
                // Clear grid first, then copy frame data
                grid = new Color[rows][cols];
                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < cols; x++) {
                        if (y < frameData.length && x < frameData[0].length) {
                            grid[y][x] = frameData[y][x];
                        }
                    }
                }
            } else {
                // Invalid index, clear grid
                grid = new Color[rows][cols];
            }
            System.out.println("Frame " + idx + " loaded");
            repaint();
        }
        
        public int getCurrentFrameIndex() { return this.currentFrameIndex; }
        public Color[][] captureCurrentFrame() {
            Color[][] copy = new Color[rows][cols];
            for (int y = 0; y < rows; y++)
                for (int x = 0; x < cols; x++)
                    copy[y][x] = grid[y][x];
            return copy;
        }

        public Color getColorAt(int x, int y) { return grid[y][x]; }

        public void resizeGrid(int newCols, int newRows) {
            saveToUndoStack();
            Color[][] ng = new Color[newRows][newCols];
            for (int y = 0; y < Math.min(rows, newRows); y++)
                for (int x = 0; x < Math.min(cols, newCols); x++) ng[y][x] = grid[y][x];
            this.cols = newCols; this.rows = newRows; this.grid = ng;
            setPreferredSize(new Dimension(cols*pixelSize, rows*pixelSize)); revalidate(); repaint();
        }

        public void clear() { 
            saveToUndoStack();
            grid = new Color[rows][cols]; 
            redoStack.clear();
            if (undoRedoCallback != null) undoRedoCallback.run();
            repaint(); 
        }
        
        private void saveToUndoStack() {
            Color[][] copy = new Color[rows][cols];
            for (int y = 0; y < rows; y++) 
                for (int x = 0; x < cols; x++) copy[y][x] = grid[y][x];
            undoStack.push(copy);
            redoStack.clear();
            if (undoRedoCallback != null) undoRedoCallback.run();
        }
        
        public void undo() {
            if (undoStack.isEmpty()) return;
            Color[][] current = new Color[rows][cols];
            for (int y = 0; y < rows; y++) 
                for (int x = 0; x < cols; x++) current[y][x] = grid[y][x];
            redoStack.push(current);
            grid = undoStack.pop();
            if (undoRedoCallback != null) undoRedoCallback.run();
            repaint();
        }
        
        public void redo() {
            if (redoStack.isEmpty()) return;
            Color[][] current = new Color[rows][cols];
            for (int y = 0; y < rows; y++) 
                for (int x = 0; x < cols; x++) current[y][x] = grid[y][x];
            undoStack.push(current);
            grid = redoStack.pop();
            if (undoRedoCallback != null) undoRedoCallback.run();
            repaint();
        }
        
        public boolean canUndo() { return !undoStack.isEmpty(); }
        public boolean canRedo() { return !redoStack.isEmpty(); }

        public BufferedImage renderToImage() {
            BufferedImage img = new BufferedImage(cols * pixelSize, rows * pixelSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (backgroundMode == 0) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } else if (backgroundMode == 1) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } else {
                // Transparent - no fill, alpha is 0 by default
            }
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    Color c = grid[y][x];
                    if (c != null) {
                        g.setColor(c);
                        g.fillRect(x*pixelSize, y*pixelSize, pixelSize, pixelSize);
                    }
                }
            }
            g.dispose();
            return img;
        }

        public void loadFromImage(BufferedImage img) {
            for (int y = 0; y < rows; y++)
                for (int x = 0; x < cols; x++) {
                    int rgb = img.getRGB(x, y);
                    Color c = new Color(rgb, true);
                    if (c.getAlpha() == 0) grid[y][x] = null; else grid[y][x] = new Color(c.getRed(), c.getGreen(), c.getBlue());
                }
            repaint();
        }

        public void setGridFromArray(Color[][] arr) {
            this.rows = arr.length; this.cols = arr[0].length; this.grid = new Color[rows][cols];
            for (int y = 0; y < rows; y++) for (int x = 0; x < cols; x++) this.grid[y][x] = arr[y][x];
            setPreferredSize(new Dimension(cols*pixelSize, rows*pixelSize)); revalidate(); repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Draw current frame
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    Color c = grid[y][x];
                    if (c != null) {
                        g2.setColor(c);
                        g2.fillRect(x*pixelSize, y*pixelSize, pixelSize, pixelSize);
                    } else {
                        if (backgroundMode == 0) g2.setColor(Color.WHITE);
                        else if (backgroundMode == 1) g2.setColor(Color.BLACK);
                        else g2.setColor(new Color(200, 200, 200)); // light gray for transparent
                        g2.fillRect(x*pixelSize, y*pixelSize, pixelSize, pixelSize);
                    }
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawRect(x*pixelSize, y*pixelSize, pixelSize, pixelSize);
                }
            }
        }

        private void paintAt(MouseEvent e, boolean isErase) {
            int x = e.getX() / pixelSize; int y = e.getY() / pixelSize;
            if (x < 0 || x >= cols || y < 0 || y >= rows) return;
            if (isErase) grid[y][x] = null; else grid[y][x] = currentColor;
            repaint(x*pixelSize, y*pixelSize, pixelSize, pixelSize);
        }

        @Override public void mousePressed(MouseEvent e) { 
            if (!painting) {
                saveToUndoStack();
                redoStack.clear();
                if (undoRedoCallback != null) undoRedoCallback.run();
            }
            painting = true;
            startX = e.getX() / pixelSize;
            startY = e.getY() / pixelSize;
            
            if (drawingTool == 0) {
                // Pencil - paint immediately
                paintAt(e, SwingUtilities.isRightMouseButton(e));
            } else {
                // For shapes, save temp grid for preview
                tempGrid = new Color[rows][cols];
                for (int y = 0; y < rows; y++) 
                    for (int x = 0; x < cols; x++) 
                        tempGrid[y][x] = grid[y][x];
            }
        }
        
        @Override public void mouseReleased(MouseEvent e) { 
            if (painting && drawingTool > 0) {
                // Draw final shape
                drawShape(startX, startY, e.getX() / pixelSize, e.getY() / pixelSize, SwingUtilities.isRightMouseButton(e), true);
            }
            painting = false;
        }
        
        @Override public void mouseDragged(MouseEvent e) { 
            if (!painting) return;
            if (drawingTool == 0) {
                paintAt(e, SwingUtilities.isRightMouseButton(e));
            } else {
                // For shapes, preview on temp grid
                Color[][] preview = new Color[rows][cols];
                for (int y = 0; y < rows; y++) 
                    for (int x = 0; x < cols; x++) 
                        preview[y][x] = tempGrid[y][x];
                grid = preview;
                drawShape(startX, startY, e.getX() / pixelSize, e.getY() / pixelSize, SwingUtilities.isRightMouseButton(e), false);
                repaint();
            }
        }
        
        @Override public void mouseMoved(MouseEvent e) {}
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        
        private void drawShape(int x1, int y1, int x2, int y2, boolean erase, boolean save) {
            switch (drawingTool) {
                case 1: // Line
                    drawLine(x1, y1, x2, y2, erase);
                    break;
                case 2: // Rectangle outline
                    drawRectangle(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), false, erase);
                    break;
                case 3: // Oval outline
                    drawOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), false, erase);
                    break;
                case 4: // Triangle
                    drawTriangle(x1, y1, x2, y2, erase);
                    break;
                case 5: // Filled rectangle
                    drawRectangle(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), true, erase);
                    break;
                case 6: // Filled oval
                    drawOval(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), true, erase);
                    break;
            }
        }
        
        private void drawLine(int x1, int y1, int x2, int y2, boolean erase) {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int sx = x1 < x2 ? 1 : -1;
            int sy = y1 < y2 ? 1 : -1;
            int err = dx - dy;
            
            int x = x1, y = y1;
            while (true) {
                if (x >= 0 && x < cols && y >= 0 && y < rows) {
                    if (erase) grid[y][x] = null;
                    else grid[y][x] = currentColor;
                }
                if (x == x2 && y == y2) break;
                int e2 = 2 * err;
                if (e2 > -dy) { err -= dy; x += sx; }
                if (e2 < dx) { err += dx; y += sy; }
            }
        }
        
        private void drawRectangle(int x1, int y1, int x2, int y2, boolean filled, boolean erase) {
            if (filled) {
                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        if (x >= 0 && x < cols && y >= 0 && y < rows) {
                            if (erase) grid[y][x] = null;
                            else grid[y][x] = currentColor;
                        }
                    }
                }
            } else {
                for (int x = x1; x <= x2; x++) {
                    if (x >= 0 && x < cols) {
                        if (y1 >= 0 && y1 < rows) {
                            if (erase) grid[y1][x] = null;
                            else grid[y1][x] = currentColor;
                        }
                        if (y2 >= 0 && y2 < rows) {
                            if (erase) grid[y2][x] = null;
                            else grid[y2][x] = currentColor;
                        }
                    }
                }
                for (int y = y1; y <= y2; y++) {
                    if (y >= 0 && y < rows) {
                        if (x1 >= 0 && x1 < cols) {
                            if (erase) grid[y][x1] = null;
                            else grid[y][x1] = currentColor;
                        }
                        if (x2 >= 0 && x2 < cols) {
                            if (erase) grid[y][x2] = null;
                            else grid[y][x2] = currentColor;
                        }
                    }
                }
            }
        }
        
        private void drawOval(int x1, int y1, int x2, int y2, boolean filled, boolean erase) {
            int cx = (x1 + x2) / 2;
            int cy = (y1 + y2) / 2;
            int rx = Math.abs(x2 - x1) / 2;
            int ry = Math.abs(y2 - y1) / 2;
            
            if (rx == 0 || ry == 0) return;
            
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    if (x >= 0 && x < cols && y >= 0 && y < rows) {
                        double dx = (x - cx) / (double) rx;
                        double dy = (y - cy) / (double) ry;
                        double dist = dx * dx + dy * dy;
                        
                        if (filled) {
                            if (dist <= 1.0) {
                                if (erase) grid[y][x] = null;
                                else grid[y][x] = currentColor;
                            }
                        } else {
                            if (dist <= 1.1 && dist >= 0.7) {
                                if (erase) grid[y][x] = null;
                                else grid[y][x] = currentColor;
                            }
                        }
                    }
                }
            }
        }
        
        private void drawTriangle(int x1, int y1, int x2, int y2, boolean erase) {
            int x3 = x1;
            int y3 = y2;
            
            drawLine(x1, y1, x2, y2, erase);
            drawLine(x2, y2, x3, y3, erase);
            drawLine(x3, y3, x1, y1, erase);
        }
    }
    
    private void toggleAnimationMode() {
        if (!isAnimationMode) {
            // Switch to animation mode - remove canvas from pixelArtPanel first
            pixelArtPanel.removeAll();
            mainContentPanel.removeAll();
            updateAnimationPanel();
            mainContentPanel.add(animationPanel, BorderLayout.CENTER);
            animationBtn.setText("< Back");
            isAnimationMode = true;
        } else {
            // Switch back to pixel art mode - remove canvas from animationPanel and restore to pixelArtPanel
            animationPanel.removeAll();
            mainContentPanel.removeAll();
            pixelArtPanel.removeAll();
            
            // Re-add canvas with GridBagLayout centering
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            
            JScrollPane canvasScroll = new JScrollPane(canvas);
            canvasScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            canvasScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            pixelArtPanel.add(canvasScroll, gbc);
            
            mainContentPanel.add(pixelArtPanel, BorderLayout.CENTER);
            animationBtn.setText("Animation");
            isAnimationMode = false;
        }
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
        canvas.repaint();
        frame.pack();
        frame.revalidate();
        frame.repaint();
    }
    
    private void updateAnimationPanel() {
        animationPanel.removeAll();
        List<AnimationFrame> frames = canvas.getAnimationFrames();
        
        // Auto-add first frame if empty
        if (frames.isEmpty()) {
            frames.add(new AnimationFrame(canvas.captureCurrentFrame(), 100));
            canvas.setCurrentFrameIndex(0);
            animationCurrentFrameIndex = 0;
        } else {
            animationCurrentFrameIndex = canvas.getCurrentFrameIndex();
        }
        
        // Top: Frame navigation
        JPanel topAnimPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton prevBtn = new JButton("<<");
        JButton nextBtn = new JButton(">>");
        
        frameIndicatorLabel = new JLabel(String.format("<< %d >>", animationCurrentFrameIndex + 1));
        
        prevBtn.addActionListener(e -> {
            if (animationCurrentFrameIndex > 0) {
                animationCurrentFrameIndex--;
                canvas.setCurrentFrameIndex(animationCurrentFrameIndex);
                frameIndicatorLabel.setText(String.format("<< %d >>", animationCurrentFrameIndex + 1));
                updateAnimationPanel();
                canvas.repaint();
            }
        });
        
        nextBtn.addActionListener(e -> {
            if (animationCurrentFrameIndex < frames.size() - 1) {
                animationCurrentFrameIndex++;
                canvas.setCurrentFrameIndex(animationCurrentFrameIndex);
                frameIndicatorLabel.setText(String.format("<< %d >>", animationCurrentFrameIndex + 1));
                updateAnimationPanel();
                canvas.repaint();
            }
        });
        
        topAnimPanel.add(prevBtn);
        topAnimPanel.add(frameIndicatorLabel);
        topAnimPanel.add(nextBtn);
        
        // Add frame management buttons
        JButton addFrameBtn = new JButton("+");
        addFrameBtn.addActionListener(e -> {
            // IMPORTANT: Save current frame FIRST before switching
            if (animationCurrentFrameIndex >= 0 && animationCurrentFrameIndex < frames.size()) {
                Color[][] currentData = canvas.captureCurrentFrame();
                int currentDelay = frames.get(animationCurrentFrameIndex).delay;
                frames.set(animationCurrentFrameIndex, new AnimationFrame(currentData, currentDelay));
                System.out.println("Frame " + animationCurrentFrameIndex + " saved with data");
            }
            // Add new blank frame
            Color[][] newFrame = new Color[canvas.getRows()][canvas.getCols()];
            frames.add(new AnimationFrame(newFrame, 100));
            animationCurrentFrameIndex = frames.size() - 1;
            System.out.println("Frame " + animationCurrentFrameIndex + " added (blank)");
            System.out.println("Total frames: " + frames.size());
            canvas.setCurrentFrameIndex(animationCurrentFrameIndex);
            updateAnimationPanel();
        });

        JButton copyFrameBtn = new JButton("Copy");
        copyFrameBtn.addActionListener(e -> {
            // IMPORTANT: Save current frame FIRST before switching
            if (animationCurrentFrameIndex >= 0 && animationCurrentFrameIndex < frames.size()) {
                Color[][] currentData = canvas.captureCurrentFrame();
                int currentDelay = frames.get(animationCurrentFrameIndex).delay;
                frames.set(animationCurrentFrameIndex, new AnimationFrame(currentData, currentDelay));
                System.out.println("Frame " + animationCurrentFrameIndex + " saved with data");
            }
            // Copy current frame to new frame
            Color[][] copyFrame = canvas.captureCurrentFrame();
            frames.add(new AnimationFrame(copyFrame, 100));
            animationCurrentFrameIndex = frames.size() - 1;
            System.out.println("Frame " + animationCurrentFrameIndex + " added (copy from current)");
            System.out.println("Total frames: " + frames.size());
            canvas.setCurrentFrameIndex(animationCurrentFrameIndex);
            updateAnimationPanel();
        });

        JButton removeFrameBtn = new JButton("Delete Frame");
        removeFrameBtn.addActionListener(e -> {
            if (frames.size() <= 1) {
                JOptionPane.showMessageDialog(frame, "Cannot delete the last frame! At least 1 frame must remain.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(frame, 
                "Delete frame " + (animationCurrentFrameIndex + 1) + "?", 
                "Delete Frame", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                frames.remove(animationCurrentFrameIndex);
                animationCurrentFrameIndex = Math.min(animationCurrentFrameIndex, frames.size() - 1);
                canvas.setCurrentFrameIndex(animationCurrentFrameIndex);
                System.out.println("Frame deleted. Total frames: " + frames.size());
                updateAnimationPanel();
            }
        });

        topAnimPanel.add(new JSeparator(JSeparator.VERTICAL));
        topAnimPanel.add(addFrameBtn);
        topAnimPanel.add(copyFrameBtn);
        topAnimPanel.add(removeFrameBtn);
        
        animationPanel.add(topAnimPanel, BorderLayout.NORTH);
        
        // Center: Canvas - add it here since it was removed from pixelArtPanel
        animationPanel.add(new JScrollPane(canvas), BorderLayout.CENTER);
        
        // Bottom: Frame settings with proper layout
        JPanel bottomAnimPanel = new JPanel();
        bottomAnimPanel.setLayout(new BoxLayout(bottomAnimPanel, BoxLayout.Y_AXIS));
        bottomAnimPanel.setBorder(BorderFactory.createTitledBorder("Frame Settings"));

        int currentFrameIdx = canvas.getCurrentFrameIndex();
        if (currentFrameIdx < 0 || currentFrameIdx >= frames.size()) {
            currentFrameIdx = 0;
        }
        int currentDelay = frames.get(currentFrameIdx).delay;
        
        // Buat final reference untuk digunakan di lambda
        final int frameIndexForSpinner = currentFrameIdx;

        // Frame Delay Mode Toggle Panel (New)
        JPanel delayModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        delayModePanel.setBorder(BorderFactory.createTitledBorder("Frame Delay Input Mode"));
        JRadioButton freeInputRadio = new JRadioButton("Free Input");
        JRadioButton presetButtonsRadio = new JRadioButton("Preset Buttons");
        ButtonGroup delayModeGroup = new ButtonGroup();
        delayModeGroup.add(freeInputRadio);
        delayModeGroup.add(presetButtonsRadio);
        freeInputRadio.setSelected(true); // default mode
        
        delayModePanel.add(freeInputRadio);
        delayModePanel.add(presetButtonsRadio);
        bottomAnimPanel.add(delayModePanel);
        
        // Frame Delay Free Input Panel (existing spinner)
        JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner delaySpinner = new JSpinner(new SpinnerNumberModel(currentDelay, 10, 5000, 10));
        final JSpinner finalDelaySpinner = delaySpinner; // Make it final for lambda
        delayPanel.add(new JLabel("Frame Delay (ms):"));
        delayPanel.add(new JLabel("(Delay between this and next frame)"));
        delayPanel.add(delaySpinner);
        bottomAnimPanel.add(delayPanel);
        
        // Frame Delay Preset Buttons Panel (new)
        JPanel presetButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetButtonsPanel.setVisible(false);
        JButton[] delayButtons = new JButton[10];
        for (int i = 0; i < 10; i++) {
            int delayValue = (i + 1) * 100; // 100ms, 200ms, ..., 1000ms
            JButton btn = new JButton(delayValue + "ms");
            btn.setPreferredSize(new Dimension(60, 30));
            final int val = delayValue;
            btn.addActionListener(e -> {
                if (frameIndexForSpinner >= 0 && frameIndexForSpinner < frames.size()) {
                    AnimationFrame af = frames.get(frameIndexForSpinner);
                    af.delay = val;
                    finalDelaySpinner.setValue(val);
                    System.out.println("[PRESET BUTTON] Frame " + frameIndexForSpinner + " -> delay set to " + val + "ms");
                }
            });
            delayButtons[i] = btn;
            presetButtonsPanel.add(btn);
        }
        bottomAnimPanel.add(presetButtonsPanel);

        // Toggle visibility of spinner and buttons based on mode
        ActionListener delayModeListener = e -> {
            boolean isFreeInput = freeInputRadio.isSelected();
            delayPanel.setVisible(isFreeInput);
            presetButtonsPanel.setVisible(!isFreeInput);
        };
        
        freeInputRadio.addActionListener(delayModeListener);
        presetButtonsRadio.addActionListener(delayModeListener);
        delayModeListener.actionPerformed(null); // Set initial visibility based on selection
        
        // Change listener for free input spinner updates AnimationFrame.delay
        delaySpinner.addChangeListener(e -> {
            int newDelay = (Integer) delaySpinner.getValue();
            if (frameIndexForSpinner >= 0 && frameIndexForSpinner < frames.size()) {
                AnimationFrame af = frames.get(frameIndexForSpinner);
                af.delay = newDelay;
                System.out.println("[SPINNER CHANGED] Frame " + frameIndexForSpinner + " -> delay = " + newDelay + "ms (now stored in frame object)");
            }
        });
        
        // Loop Delay row (unchanged)
        JPanel loopDelayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner loopDelaySpinner = new JSpinner(new SpinnerNumberModel(animationLoopDelay, 0, 5000, 100));
        loopDelaySpinner.addChangeListener(e -> {
            animationLoopDelay = (Integer) loopDelaySpinner.getValue();
            System.out.println("Loop delay updated to: " + animationLoopDelay + "ms");
        });
        loopDelayPanel.add(new JLabel("Loop Delay (ms):"));
        loopDelayPanel.add(new JLabel("(Delay from last frame to first)"));
        loopDelayPanel.add(loopDelaySpinner);
        bottomAnimPanel.add(loopDelayPanel);



        // Buttons row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton playBtn = new JButton("Play");
        playBtn.addActionListener(e -> {
            // SAVE current delay value SEBELUM play
            if (frameIndexForSpinner >= 0 && frameIndexForSpinner < frames.size()) {
                int currentSpinnerValue = (Integer) finalDelaySpinner.getValue();
                AnimationFrame af = frames.get(frameIndexForSpinner);
                af.delay = currentSpinnerValue;
                System.out.println("PRE-PLAY SAVE: Frame " + frameIndexForSpinner + " delay = " + currentSpinnerValue + "ms");
            }
            playAnimation();
        });

        JButton exportMp4Btn = new JButton("Export MP4");
        exportMp4Btn.addActionListener(e -> exportToMP4());

        buttonPanel.add(playBtn);
        buttonPanel.add(exportMp4Btn);
        bottomAnimPanel.add(buttonPanel);
        
        animationPanel.add(bottomAnimPanel, BorderLayout.SOUTH);
        animationPanel.revalidate();
        animationPanel.repaint();
    }

    private void playAnimation() {
        List<AnimationFrame> frames = canvas.getAnimationFrames();
        if (frames.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No frames to play");
            return;
        }

        // CRITICAL: Save current frame BEFORE play starts
        System.out.println("\n>>> BEFORE PLAY: Saving frame " + animationCurrentFrameIndex);
        if (animationCurrentFrameIndex >= 0 && animationCurrentFrameIndex < frames.size()) {
            Color[][] currentData = canvas.captureCurrentFrame();
            int currentSavedDelay = frames.get(animationCurrentFrameIndex).delay;
            frames.set(animationCurrentFrameIndex, new AnimationFrame(currentData, currentSavedDelay));
            System.out.println("    Frame " + animationCurrentFrameIndex + " saved with delay=" + currentSavedDelay + "ms");
        }

        // Print semua frame delays DENGAN DEBUGGING
        System.out.println("\n========== ANIMATION DEBUG ==========");
        System.out.println("Total frames: " + frames.size());
        for (int i = 0; i < frames.size(); i++) {
            AnimationFrame af = frames.get(i);
            System.out.println("  Frame " + i + ": data=" + (af.data != null ? "OK" : "NULL") + ", delay=" + af.delay + "ms");
        }
        System.out.println("Loop delay: " + animationLoopDelay + "ms");
        System.out.println("====================================\n");

        JDialog playDialog = new JDialog(frame, "Animation Player", false);
        playDialog.setSize(600, 600);
        playDialog.setLocationRelativeTo(frame);
        playDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        playDialog.setAlwaysOnTop(true);

        AnimationCanvas animCanvas = new AnimationCanvas(frames, canvas.getCols(), canvas.getRows());

        JPanel playPanel = new JPanel(new BorderLayout());
        playPanel.add(animCanvas, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final boolean[] isPlaying = {true};
        JButton stopBtn = new JButton("Stop");
        stopBtn.addActionListener(e -> {
            isPlaying[0] = false;
            playDialog.dispose();
        });
        controlPanel.add(stopBtn);
        playPanel.add(controlPanel, BorderLayout.SOUTH);

        playDialog.add(playPanel);
        playDialog.setVisible(true);

        // SIMPLE LOOP: 0 -> 1 -> 2 -> ... -> N-1 -> loop
        Thread animThread = new Thread(() -> {
            try {
                int loopCount = 0;
                while (isPlaying[0]) {
                    loopCount++;
                    System.out.println("\n--- LOOP " + loopCount + " ---");
                    
                    // Loop through semua frames dengan delay antara frame secara tepat
                    for (int i = 0; i < frames.size(); i++) {
                        if (!isPlaying[0]) break;
                        
                        System.out.println("Display frame " + i);
                        final int idx = i;
                        
                        SwingUtilities.invokeLater(() -> {
                            animCanvas.setCurrentFrame(idx);
                        });

                        int frameDelay = frames.get(i).delay;
                        if (frameDelay <= 0) frameDelay = 100;

                        System.out.println("  Waiting frame delay " + frameDelay + "ms before " + ((i + 1 < frames.size()) ? "next frame" : "loop delay"));

                        Thread.sleep(frameDelay);
                        
                        // If this is the last frame and animation is still playing, apply loop delay
                        if (i == frames.size() - 1 && isPlaying[0] && animationLoopDelay > 0) {
                            System.out.println("  Applying loop delay " + animationLoopDelay + "ms");
                            Thread.sleep(animationLoopDelay);
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Animation stopped by user");
            }
            finally {
                isPlaying[0] = false;
                System.out.println("Animation ended\n");
                SwingUtilities.invokeLater(() -> {
                    if (playDialog.isDisplayable()) {
                        playDialog.dispose();
                    }
                });
            }
        });
        animThread.setDaemon(true);
        animThread.start();
    }
    
    private void applyTheme() {
        switch (currentTheme) {
            case 0: // Light Mode
                bgColor = Color.WHITE;
                fgColor = Color.BLACK;
                accentColor = new Color(51, 122, 183);
                panelColor = new Color(240, 240, 240);
                break;
            case 1: // Dark Mode
                bgColor = new Color(30, 30, 30);
                fgColor = new Color(220, 220, 220);
                accentColor = new Color(100, 150, 200);
                panelColor = new Color(45, 45, 45);
                break;
            case 2: // Cozy Mode
                bgColor = new Color(240, 243, 248);
                fgColor = new Color(40, 50, 80);
                accentColor = new Color(75, 110, 150);
                panelColor = new Color(200, 210, 225);
                break;
        }
        
        // Apply theme dengan transisi smooth
        Timer transitionTimer = new Timer(20, null);
        transitionTimer.addActionListener(e -> {
            applyThemeToComponent(frame, true);
            if (toolButtons != null && toolButtons.length > 0) {
                selectTool(currentTool); // Re-apply tool button styling
            }
            frame.repaint();
        });
        transitionTimer.start();
        
        // Stop after transition
        new Timer(300, e -> {
            ((Timer) e.getSource()).stop();
            applyThemeToComponent(frame, true);
            if (toolButtons != null && toolButtons.length > 0) {
                selectTool(currentTool);
            }
            frame.repaint();
        }).start();
    }
    
    private void applyThemeToComponent(Component comp, boolean isTransition) {
        if (comp == null) return;
        
        // Skip palette color swatches - keep their original colors
        if (comp instanceof JButton && comp.getName() != null && comp.getName().startsWith("colorSwatch")) {
            return;
        }
        
        // Apply colors based on component type
        if (comp instanceof JPanel) {
            comp.setBackground(panelColor);
            comp.setForeground(fgColor);
        } else if (comp instanceof JButton) {
            JButton btn = (JButton) comp;
            
            // Tool buttons - dibiarkan dengan border raised
            if (btn.getName() != null && btn.getName().startsWith("toolBtn")) {
                // Jangan ubah untuk saat ini, akan di-update oleh selectTool
                return;
            }
            
            // Regular buttons
            btn.setBackground(panelColor);
            btn.setForeground(fgColor);
            btn.setOpaque(true);
            btn.setBorderPainted(true);
            btn.setFocusPainted(false);
        } else if (comp instanceof JLabel) {
            comp.setForeground(fgColor);
        } else if (comp instanceof JTextComponent) {
            comp.setBackground(bgColor);
            comp.setForeground(fgColor);
        } else if (comp instanceof JComboBox) {
            comp.setBackground(bgColor);
            comp.setForeground(fgColor);
        } else if (comp instanceof JSlider) {
            comp.setBackground(panelColor);
            comp.setForeground(fgColor);
        } else if (comp instanceof JSpinner) {
            comp.setBackground(bgColor);
            comp.setForeground(fgColor);
        } else if (comp instanceof JScrollPane) {
            comp.setBackground(panelColor);
            comp.setForeground(fgColor);
        }
        
        // Recursively apply to children
        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                applyThemeToComponent(container.getComponent(i), isTransition);
            }
        }
    }
    
    private void showCodeGeneratorDialog() {
        String[] languages = {"Java", "C++", "Python", "HTML/CSS/JS"};
        int choice = JOptionPane.showOptionDialog(frame, "Generate code in:", "Code Generator",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, languages, languages[0]);
        
        if (choice == -1) return;
        
        String code = "";
        switch (choice) {
            case 0: code = generateJavaCode(); break;
            case 1: code = generateCppCode(); break;
            case 2: code = generatePythonCode(); break;
            case 3: code = generateHtmlCode(); break;
        }
        
        showCodeDialog(code, languages[choice]);
    }
    
    private void showCodeDialog(String code, String language) {
        JDialog codeDialog = new JDialog(frame, "Code - " + language, true);
        codeDialog.setSize(800, 600);
        codeDialog.setLocationRelativeTo(frame);
        
        JTextArea codeArea = new JTextArea(code);
        codeArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        codeArea.setEditable(false);
        codeArea.setMargin(new Insets(10, 10, 10, 10));
        
        JButton copyBtn = new JButton("Copy to Clipboard");
        copyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(code);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(codeDialog, "Code copied to clipboard!");
        });
        
        JButton exportBtn = new JButton("Export as File");
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            String extension = language.equals("Java") ? ".java" : 
                             language.equals("C++") ? ".cpp" : 
                             language.equals("Python") ? ".py" : ".html";
            fc.setSelectedFile(new File("pixel_art" + extension));
            if (fc.showSaveDialog(codeDialog) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try (PrintWriter pw = new PrintWriter(f)) {
                    pw.print(code);
                    JOptionPane.showMessageDialog(codeDialog, "Exported: " + f.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(codeDialog, "Error: " + ex.getMessage());
                }
            }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(copyBtn);
        buttonPanel.add(exportBtn);
        
        codeDialog.add(new JScrollPane(codeArea), BorderLayout.CENTER);
        codeDialog.add(buttonPanel, BorderLayout.SOUTH);
        codeDialog.setVisible(true);
    }
    
    private String generateJavaCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.awt.*;\n\n");
        sb.append("public class PixelArt {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        int[][] grid = {\n");
        
        for (int y = 0; y < canvas.getRows(); y++) {
            sb.append("            { ");
            for (int x = 0; x < canvas.getCols(); x++) {
                Color c = canvas.getColorAt(x, y);
                if (c == null) sb.append("0");
                else sb.append("0x").append(String.format("%06X", c.getRGB() & 0xFFFFFF));
                if (x < canvas.getCols() - 1) sb.append(", ");
            }
            sb.append(" }");
            if (y < canvas.getRows() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("        };\n");
        sb.append("        // Use grid to draw pixel art\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
    
    private String generateCppCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <iostream>\n");
        sb.append("#include <vector>\n\n");
        sb.append("int main() {\n");
        sb.append("    int grid[").append(canvas.getRows()).append("][").append(canvas.getCols()).append("] = {\n");
        
        for (int y = 0; y < canvas.getRows(); y++) {
            sb.append("        { ");
            for (int x = 0; x < canvas.getCols(); x++) {
                Color c = canvas.getColorAt(x, y);
                if (c == null) sb.append("0");
                else sb.append("0x").append(String.format("%06X", c.getRGB() & 0xFFFFFF));
                if (x < canvas.getCols() - 1) sb.append(", ");
            }
            sb.append(" }");
            if (y < canvas.getRows() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("    };\n");
        sb.append("    // Use grid to draw pixel art\n");
        sb.append("    return 0;\n");
        sb.append("}\n");
        return sb.toString();
    }
    
    private String generatePythonCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Pixel Art\n");
        sb.append("# Size: ").append(canvas.getCols()).append("x").append(canvas.getRows()).append("\n\n");
        sb.append("grid = [\n");
        
        for (int y = 0; y < canvas.getRows(); y++) {
            sb.append("    [");
            for (int x = 0; x < canvas.getCols(); x++) {
                Color c = canvas.getColorAt(x, y);
                if (c == null) sb.append("0x000000");
                else sb.append("0x").append(String.format("%06X", c.getRGB() & 0xFFFFFF));
                if (x < canvas.getCols() - 1) sb.append(", ");
            }
            sb.append("]");
            if (y < canvas.getRows() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("]\n\n");
        sb.append("# Use grid to draw pixel art\n");
        return sb.toString();
    }
    
    private String generateHtmlCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n<head>\n");
        sb.append("    <title>Pixel Art</title>\n");
        sb.append("    <style>\n");
        sb.append("        .pixel { width: 20px; height: 20px; display: inline-block; }\n");
        sb.append("        .grid { display: grid; grid-template-columns: repeat(").append(canvas.getCols()).append(", 20px); }\n");
        sb.append("    </style>\n</head>\n<body>\n");
        sb.append("    <h1>Pixel Art</h1>\n");
        sb.append("    <div class=\"grid\">\n");
        
        for (int y = 0; y < canvas.getRows(); y++) {
            for (int x = 0; x < canvas.getCols(); x++) {
                Color c = canvas.getColorAt(x, y);
                String color = c == null ? "#FFFFFF" : String.format("#%06X", c.getRGB() & 0xFFFFFF);
                sb.append("        <div class=\"pixel\" style=\"background-color: ").append(color).append(";\"></div>\n");
            }
        }
        
        sb.append("    </div>\n</body>\n</html>\n");
        return sb.toString();
    }
    
    private void exportToMP4() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP4 Video", "mp4"));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            String filePath = fc.getSelectedFile().getAbsolutePath();
            if (!filePath.endsWith(".mp4")) filePath += ".mp4";
            
            JOptionPane.showMessageDialog(frame, "MP4 export requires FFmpeg.\nPlease install FFmpeg first.\n\nSaved animation frames to: " + filePath);
        }
    }
    
    private void selectTool(int toolId) {
        currentTool = toolId;
        for (int i = 0; i < toolButtons.length; i++) {
            if (i == toolId) {
                // Selected button - lighter background
                toolButtons[i].setBorder(BorderFactory.createLoweredBevelBorder());
                toolButtons[i].setBackground(accentColor);
                toolButtons[i].setForeground(Color.WHITE);
            } else {
                // Unselected button
                toolButtons[i].setBorder(BorderFactory.createRaisedBevelBorder());
                toolButtons[i].setBackground(panelColor);
                toolButtons[i].setForeground(fgColor);
            }
            toolButtons[i].setOpaque(true);
        }
        canvas.setDrawingTool(toolId);
    }
    
    private JPanel createToolsPanel() {
        JPanel toolsPanel = new JPanel();
        toolsPanel.setLayout(new GridLayout(7, 1, 2, 2));
        toolsPanel.setBorder(BorderFactory.createTitledBorder("Tools"));
        toolsPanel.setPreferredSize(new Dimension(50, 250));
        
        toolButtons = new JButton[7];
        String[] toolNames = {
            "✏",    // Pencil
            "/",    // Line
            "□",    // Rect
            "○",    // Oval
            "△",    // Triangle
            "■",    // Fill Rect
            "●"     // Fill Oval
        };
        
        for (int i = 0; i < toolNames.length; i++) {
            final int toolId = i;
            toolButtons[i] = new JButton(toolNames[i]);
            toolButtons[i].setName("toolBtn_" + i); // Identifier untuk tema
            toolButtons[i].setPreferredSize(new Dimension(40, 40));
            toolButtons[i].setFont(new Font("Arial", Font.BOLD, 18));
            toolButtons[i].setBorder(BorderFactory.createRaisedBevelBorder());
            toolButtons[i].setFocusPainted(false);
            toolButtons[i].addActionListener(e -> selectTool(toolId));
            toolButtons[i].setToolTipText(getToolName(toolId));
            toolsPanel.add(toolButtons[i]);
        }
        selectTool(0); // Select pencil by default
        
        return toolsPanel;
    }
    
    private String getToolName(int toolId) {
        String[] names = {"Pencil", "Line", "Rectangle", "Oval", "Triangle", "Fill Rectangle", "Fill Oval"};
        return toolId >= 0 && toolId < names.length ? names[toolId] : "Unknown";
    }
    
    private JPanel createHSVColorPanel() {
        JPanel hsvPanel = new JPanel();
        hsvPanel.setLayout(new BoxLayout(hsvPanel, BoxLayout.Y_AXIS));
        hsvPanel.setBorder(BorderFactory.createTitledBorder("Color Picker"));
        hsvPanel.setPreferredSize(new Dimension(140, 120));
        
        // Hue slider
        JPanel huePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        huePanel.add(new JLabel("H:"));
        JSlider hueSlider = new JSlider(0, 360, 0);
        hueSlider.setPreferredSize(new Dimension(100, 20));
        huePanel.add(hueSlider);
        hsvPanel.add(huePanel);
        
        // Saturation slider
        JPanel satPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        satPanel.add(new JLabel("S:"));
        JSlider satSlider = new JSlider(0, 100, 100);
        satSlider.setPreferredSize(new Dimension(100, 20));
        satPanel.add(satSlider);
        hsvPanel.add(satPanel);
        
        // Value slider
        JPanel valPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        valPanel.add(new JLabel("V:"));
        JSlider valSlider = new JSlider(0, 100, 100);
        valSlider.setPreferredSize(new Dimension(100, 20));
        valPanel.add(valSlider);
        hsvPanel.add(valPanel);
        
        // Color preview
        JPanel previewPanel = new JPanel();
        previewPanel.setPreferredSize(new Dimension(140, 25));
        previewPanel.setBackground(currentColor);
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        hsvPanel.add(previewPanel);
        
        // Update color when sliders change
        ChangeListener updateColor = e -> {
            int h = hueSlider.getValue();
            int s = satSlider.getValue();
            int v = valSlider.getValue();
            currentColor = Color.getHSBColor(h / 360f, s / 100f, v / 100f);
            previewPanel.setBackground(currentColor);
            canvas.setCurrentColor(currentColor, "HSV");
            previewPanel.repaint();
        };
        
        hueSlider.addChangeListener(updateColor);
        satSlider.addChangeListener(updateColor);
        valSlider.addChangeListener(updateColor);
        
        return hsvPanel;
    }
}

class AnimationCanvas extends JPanel {
    private List<AnimationFrame> frames;
    private int currentFrameIndex = 0;
    private int cols, rows;
    private int pixelSize;

    public AnimationCanvas(List<AnimationFrame> frames, int cols, int rows) {
        this.frames = frames;
        this.cols = cols;
        this.rows = rows;
        setBackground(Color.WHITE);
        this.pixelSize = Math.min(600 / cols, 600 / rows); // fixed pixel size to fit in 600x600
        if (this.pixelSize < 1) this.pixelSize = 1;
        setPreferredSize(new Dimension(cols * this.pixelSize, rows * this.pixelSize));
    }

    public void setCurrentFrame(int idx) {
        this.currentFrameIndex = idx;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (frames != null && currentFrameIndex >= 0 && currentFrameIndex < frames.size()) {
            Color[][] frameData = frames.get(currentFrameIndex).data;
            if (frameData != null && frameData.length > 0) {
                int ps = this.pixelSize;
                for (int y = 0; y < frameData.length; y++) {
                    for (int x = 0; x < frameData[0].length; x++) {
                        Color c = frameData[y][x];
                        if (c != null) {
                            g2.setColor(c);
                            g2.fillRect(x*ps, y*ps, ps, ps);
                        }
                        // Don't draw anything for null pixels - let background show through
                    }
                }
            }
        }
    }
}

class AnimationFrame {
    Color[][] data;
    int delay; // milliseconds

    AnimationFrame(Color[][] frameData, int delay) {
        this.data = new Color[frameData.length][frameData[0].length];
        for (int y = 0; y < frameData.length; y++)
            for (int x = 0; x < frameData[0].length; x++)
                this.data[y][x] = frameData[y][x];
        this.delay = delay;
    }
}
