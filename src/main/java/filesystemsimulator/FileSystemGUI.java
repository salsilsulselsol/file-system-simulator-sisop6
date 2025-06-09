package filesystemsimulator;

import filesystemsimulator.exceptions.FileSystemException;
import filesystemsimulator.filesystem.FileSystem;
import filesystemsimulator.filestructures.data.DirectoryTree;
import filesystemsimulator.filestructures.data.FileType;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

public class FileSystemGUI extends JFrame {

    private FileSystem fileSystem;
    private JTextArea outputArea;
    private JLabel currentPathLabel;
    private JTree directoryTreeComponent;
    private FileSystemTreeModel fileSystemTreeModel;
    private JTextArea fileContentArea;

    public FileSystemGUI() {
        setTitle("File System Simulator GUI");
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        if (!initializeFileSystemViaDialog()) {
            JOptionPane.showMessageDialog(this, "File system initialization failed or was cancelled. Application will exit.", "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return; // Pastikan keluar jika inisialisasi gagal
        }

        setupMainUI();
        setVisible(true);
        updateAllUIElements();
    }

    private boolean initializeFileSystemViaDialog() {
        JTextField pathField = new JTextField(System.getProperty("user.home") + File.separator + "filesystem.dat", 30);
        JTextField sizeField = new JTextField("262144", 10); // Default 256KB

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        panel.add(new JLabel("Enter the path to the container file:"), gbc);
        panel.add(pathField, gbc);
        panel.add(new JLabel("Enter the maximum file system size in bytes:"), gbc);
        panel.add(sizeField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Initialize File System", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String path = pathField.getText();
            String sizeStr = sizeField.getText();
            if (path.isEmpty() || sizeStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Path and size cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return initializeFileSystemViaDialog();
            }
            try {
                long size = Long.parseLong(sizeStr);
                if (size <= 0) {
                    JOptionPane.showMessageDialog(this, "Size must be a positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return initializeFileSystemViaDialog();
                }
                fileSystem = new FileSystem(path, size); //
                return true;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid size format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return initializeFileSystemViaDialog();
            } catch (FileSystemException e) { //
                JOptionPane.showMessageDialog(this, "Error initializing file system: " + e.getMessage(), "File System Error", JOptionPane.ERROR_MESSAGE);
                return initializeFileSystemViaDialog();
            }
        }
        return false;
    }

    private void setupMainUI() {
        setLayout(new BorderLayout(5, 5)); // Tambahkan sedikit jarak antar komponen BorderLayout

        // Top Panel: Current Path and Help Button
        // JPanel topPanel = new JPanel(new BorderLayout());
        // currentPathLabel = new JLabel();
        // JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // pathPanel.add(new JLabel("Current Path: "));
        // pathPanel.add(currentPathLabel);
        // topPanel.add(pathPanel, BorderLayout.CENTER);

        // JButton helpButton = new JButton("Help");
        // helpButton.addActionListener(e -> showHelpDialog());
        // JPanel helpButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // helpButtonPanel.add(helpButton);
        // topPanel.add(helpButtonPanel, BorderLayout.EAST);

        JPanel topPanel = createEnhancedTopPanel();
        add(topPanel, BorderLayout.NORTH);


        // Center Panel: Tree View dan File Content
        directoryTreeComponent = new JTree();
        directoryTreeComponent.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        directoryTreeComponent.setCellRenderer(new FileSystemCellRenderer());
        // Tambahkan listener untuk menampilkan konten file saat node file dipilih
        directoryTreeComponent.addTreeSelectionListener(e -> {
            TreePath selectedPath = e.getPath();
            if (selectedPath != null) {
                Object lastComponent = selectedPath.getLastPathComponent();
                if (lastComponent instanceof DirectoryTree.Node selectedNode) {
                    if (selectedNode.type == FileType.FILE) { //
                        displayFileContent(selectedNode.name);
                    } else {
                        fileContentArea.setText("Selected item is a directory.");
                    }
                }
            }
        });


        DirectoryTree.Node actualRootNode = getActualFileSystemRootNode();
        if (actualRootNode != null) {
            fileSystemTreeModel = new FileSystemTreeModel(actualRootNode);
            directoryTreeComponent.setModel(fileSystemTreeModel);
        } else {
            directoryTreeComponent.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("File System Not Ready")));
            logMessage("Error: Could not get root node for JTree initialization.");
        }

        JScrollPane treeScrollPane = new JScrollPane(directoryTreeComponent);
        treeScrollPane.setMinimumSize(new Dimension(250, 100));

        fileContentArea = new JTextArea("Select a file to view its content.");
        fileContentArea.setEditable(false);
        fileContentArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        fileContentArea.setLineWrap(true);
        fileContentArea.setWrapStyleWord(true);
        JScrollPane fileContentScrollPane = new JScrollPane(fileContentArea);
        fileContentScrollPane.setMinimumSize(new Dimension(300, 100));

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, fileContentScrollPane);
        centerSplitPane.setResizeWeight(0.45);

        // Bottom Panel: Output Log dan Button Panel
        outputArea = new JTextArea(12, 50);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);

        JPanel commandButtonPanel = createCommandButtonPanel();
        JScrollPane buttonScrollPane = new JScrollPane(commandButtonPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        buttonScrollPane.setMinimumSize(new Dimension(100, 120)); // Beri tinggi minimal agar tombol terlihat
        buttonScrollPane.setPreferredSize(new Dimension(100, 130));


        JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputScrollPane, buttonScrollPane);
        bottomSplitPane.setResizeWeight(0.7);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplitPane, bottomSplitPane);
        mainSplitPane.setResizeWeight(0.75);

        add(mainSplitPane, BorderLayout.CENTER);
        logMessage("File system initialized. GUI is ready.");
    }

        private JPanel createEnhancedTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Left side: Path info
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.add(new JLabel("📁 Current Path: "));
        currentPathLabel = new JLabel();
        currentPathLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        currentPathLabel.setForeground(new Color(0, 100, 200));
        pathPanel.add(currentPathLabel);
        
        // Center: Quick stats (will be updated dynamically)
        JLabel statsLabel = new JLabel("💾 Ready");
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Right side: Help and quick action buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton quickMemoryButton = new JButton("💾");
        quickMemoryButton.setToolTipText("Quick Memory View");
        quickMemoryButton.setPreferredSize(new Dimension(35, 25));
        quickMemoryButton.addActionListener(e -> handleMemoryVisualization());
        
        JButton quickTreeButton = new JButton("🌳");
        quickTreeButton.setToolTipText("Quick Tree View");
        quickTreeButton.setPreferredSize(new Dimension(35, 25));
        quickTreeButton.addActionListener(e -> handleTreeView());
        
        JButton helpButton = new JButton("❓ Help");
        helpButton.addActionListener(e -> showHelpDialog());
        
        rightPanel.add(quickMemoryButton);
        rightPanel.add(quickTreeButton);
        rightPanel.add(helpButton);
        
        topPanel.add(pathPanel, BorderLayout.WEST);
        topPanel.add(statsLabel, BorderLayout.CENTER);
        topPanel.add(rightPanel, BorderLayout.EAST);
        
        return topPanel;
    }


    private DirectoryTree.Node getActualFileSystemRootNode() {
        if (fileSystem != null && fileSystem.tree != null) {
            return fileSystem.tree.root; // Asumsi 'root' adalah public atau ada getter
        }
        return null;
    }

    private JPanel createCommandButtonPanel() {
        JPanel panel = new JPanel();
        // Menggunakan BoxLayout untuk menyusun tombol secara vertikal dan memastikan mereka mengisi lebar
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));


        Dimension buttonSize = new Dimension(120, 28); // Ukuran tombol yang konsisten
        Component rigidArea = Box.createRigidArea(new Dimension(0, 5)); // Spasi antar tombol

        JButton mkdirButton = new JButton("mkdir");
        setupButton(mkdirButton, buttonSize, panel, rigidArea);
        mkdirButton.addActionListener(e -> handleMkdir());

        JButton lsButton = new JButton("ls (to log)");
        setupButton(lsButton, buttonSize, panel, rigidArea);
        lsButton.addActionListener(e -> handleLs());

        // NEW: Enhanced listing button
        JButton detailedListButton = new JButton("📋 Detailed List");
        detailedListButton.setBackground(new Color(230, 240, 255));
        setupButton(detailedListButton, buttonSize, panel, rigidArea);
        detailedListButton.addActionListener(e -> handleDetailedList());

        // NEW: Directory info button
        JButton dirInfoButton = new JButton("📊 Dir Info");
        dirInfoButton.setBackground(new Color(255, 240, 230));
        setupButton(dirInfoButton, buttonSize, panel, rigidArea);
        dirInfoButton.addActionListener(e -> handleDirectoryInfo());

        JButton cdButton = new JButton("cd");
        setupButton(cdButton, buttonSize, panel, rigidArea);
        cdButton.addActionListener(e -> handleCd());

        JButton moveButton = new JButton("mv"); // Atau "Move" jika Anda lebih suka
        setupButton(moveButton, buttonSize, panel, rigidArea);
        moveButton.addActionListener(e -> handleMove());

        JButton catButton = new JButton("cat (to view)");
        setupButton(catButton, buttonSize, panel, rigidArea);
        catButton.addActionListener(e -> handleCat());

        JButton writeButton = new JButton("write");
        setupButton(writeButton, buttonSize, panel, rigidArea);
        writeButton.addActionListener(e -> handleWrite());

        JButton rmButton = new JButton("rm");
        setupButton(rmButton, buttonSize, panel, rigidArea);
        rmButton.addActionListener(e -> handleRm());

        JButton rmdirButton = new JButton("rmdir");
        setupButton(rmdirButton, buttonSize, panel, rigidArea);
        rmdirButton.addActionListener(e -> handleRmdir());

        JButton cpButton = new JButton("cp");
        setupButton(cpButton, buttonSize, panel, rigidArea);
        cpButton.addActionListener(e -> handleCp());

        // NEW: Memory visualization button
        JButton memoryButton = new JButton("💾 Memory");
        memoryButton.setBackground(new Color(240, 255, 240));
        setupButton(memoryButton, buttonSize, panel, rigidArea);
        memoryButton.addActionListener(e -> handleMemoryVisualization());

        // NEW: Tree view button
        JButton treeViewButton = new JButton("🌳 Tree View");
        treeViewButton.setBackground(new Color(255, 250, 240));
        setupButton(treeViewButton, buttonSize, panel, rigidArea);
        treeViewButton.addActionListener(e -> handleTreeView());

        JButton importButton = new JButton("import");
        setupButton(importButton, buttonSize, panel, rigidArea);
        importButton.addActionListener(e -> handleImport());

        JButton exportButton = new JButton("export");
        setupButton(exportButton, buttonSize, panel, rigidArea);
        exportButton.addActionListener(e -> handleExport());

        return panel;
    }

    // NEW: Handler for detailed directory listing
    private void handleDetailedList() {
        try {
            outputArea.append("=== 📋 Detailed Directory Listing ===\n");
            
            // Redirect System.out to capture the output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);
            
            fileSystem.listCurrentDirDetailed();
            
            System.out.flush();
            System.setOut(oldOut);
            
            outputArea.append(baos.toString());
            outputArea.append("\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            
        } catch (Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        }
    }

    // NEW: Handler for directory information
    private void handleDirectoryInfo() {
        try {
            outputArea.append("=== 📊 Directory Information ===\n");
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);
            
            fileSystem.showDirectoryInfo();
            
            System.out.flush();
            System.setOut(oldOut);
            
            outputArea.append(baos.toString());
            outputArea.append("\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            
        } catch (Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        }
    }

    // NEW: Handler for memory visualization
    private void handleMemoryVisualization() {
        try {
            // Show in a separate dialog window for better visualization
            JDialog memoryDialog = new JDialog(this, "💾 Memory Visualization", true);
            memoryDialog.setSize(600, 500);
            memoryDialog.setLocationRelativeTo(this);
            
            JTextArea memoryArea = new JTextArea();
            memoryArea.setEditable(false);
            memoryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            memoryArea.setBackground(new Color(248, 248, 248));
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);
            
            fileSystem.showMemoryVisualization();
            
            System.out.flush();
            System.setOut(oldOut);
            
            memoryArea.setText(baos.toString());
            
            JScrollPane scrollPane = new JScrollPane(memoryArea);
            memoryDialog.add(scrollPane, BorderLayout.CENTER);
            
            // Add refresh button
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton refreshButton = new JButton("🔄 Refresh");
            refreshButton.addActionListener(e -> {
                try {
                    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                    PrintStream ps2 = new PrintStream(baos2);
                    PrintStream oldOut2 = System.out;
                    System.setOut(ps2);
                    
                    fileSystem.showMemoryVisualization();
                    
                    System.out.flush();
                    System.setOut(oldOut2);
                    
                    memoryArea.setText(baos2.toString());
                } catch (Exception ex) {
                    memoryArea.setText("Error refreshing: " + ex.getMessage());
                }
            });
            buttonPanel.add(refreshButton);
            memoryDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            memoryDialog.setVisible(true);
            
            // Also log to main output
            outputArea.append("=== 💾 Memory Visualization (also shown in dialog) ===\n");
            outputArea.append(baos.toString());
            outputArea.append("\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            
        } catch (Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        }
    }

    // NEW: Handler for tree view
    private void handleTreeView() {
        try {
            // Show tree view in a separate dialog
            JDialog treeDialog = new JDialog(this, "🌳 File System Tree View", true);
            treeDialog.setSize(500, 600);
            treeDialog.setLocationRelativeTo(this);
            
            JTextArea treeArea = new JTextArea();
            treeArea.setEditable(false);
            treeArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            treeArea.setBackground(new Color(250, 255, 250));
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);
            
            fileSystem.showFileSystemTree();
            
            System.out.flush();
            System.setOut(oldOut);
            
            treeArea.setText(baos.toString());
            
            JScrollPane scrollPane = new JScrollPane(treeArea);
            treeDialog.add(scrollPane, BorderLayout.CENTER);
            
            // Add expand/collapse controls
            JPanel controlPanel = new JPanel(new FlowLayout());
            JButton refreshTreeButton = new JButton("🔄 Refresh Tree");
            refreshTreeButton.addActionListener(e -> {
                try {
                    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                    PrintStream ps2 = new PrintStream(baos2);
                    PrintStream oldOut2 = System.out;
                    System.setOut(ps2);
                    
                    fileSystem.showFileSystemTree();
                    
                    System.out.flush();
                    System.setOut(oldOut2);
                    
                    treeArea.setText(baos2.toString());
                    updateDirectoryTreeDisplay(); // Also refresh main tree
                } catch (Exception ex) {
                    treeArea.setText("Error refreshing tree: " + ex.getMessage());
                }
            });
            controlPanel.add(refreshTreeButton);
            treeDialog.add(controlPanel, BorderLayout.SOUTH);
            
            treeDialog.setVisible(true);
            
            // Also log to main output
            outputArea.append("=== 🌳 File System Tree (also shown in dialog) ===\n");
            outputArea.append(baos.toString());
            outputArea.append("\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            
        } catch (Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        }
    }

    private static class EnhancedFileSystemCellRenderer extends DefaultTreeCellRenderer {
        private final Icon defaultClosedIcon;
        private final Icon defaultOpenIcon;
        private final Icon defaultLeafIcon;

        public EnhancedFileSystemCellRenderer() {
            defaultClosedIcon = UIManager.getIcon("Tree.closedIcon");
            defaultOpenIcon = UIManager.getIcon("Tree.openIcon");
            defaultLeafIcon = UIManager.getIcon("Tree.leafIcon");
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            if (value instanceof DirectoryTree.Node node) {
                setText(getEnhancedNodeText(node));
                
                if (node.type == FileType.DIRECTORY) {
                    setIcon(expanded ? defaultOpenIcon : defaultClosedIcon);
                    if (!sel) {
                        setForeground(new Color(0, 100, 200)); // Blue for directories
                    }
                } else {
                    setIcon(defaultLeafIcon);
                    if (!sel) {
                        setForeground(getFileColor(node.name)); // Different colors for different file types
                    }
                }
            } else {
                setIcon(defaultLeafIcon);
            }
            return this;
        }
        
        private String getEnhancedNodeText(DirectoryTree.Node node) {
            String icon = node.type == FileType.DIRECTORY ? "📁" : "📄";
            return icon + " " + node.name;
        }

        private Color getFileColor(String fileName) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
                return new Color(100, 100, 100); // Gray for text files
            } else if (lowerName.endsWith(".java") || lowerName.endsWith(".py") || lowerName.endsWith(".js")) {
                return new Color(0, 150, 0); // Green for code files
            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".gif")) {
                return new Color(150, 0, 150); // Purple for images
            } else if (lowerName.endsWith(".exe") || lowerName.endsWith(".bin")) {
                return new Color(200, 0, 0); // Red for executables
            } else {
                return new Color(150, 100, 0); // Brown for other files
            }
        }
    }

    private void setupButton(JButton button, Dimension size, Container container, Component spacer) {
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Short.MAX_VALUE, size.height)); // Lebar maksimum, tinggi tetap
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // Pusatkan tombol jika BoxLayout
        container.add(button);
        container.add(spacer);
    }


    private void logMessage(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            outputArea.append(message + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                outputArea.append(message + "\n");
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            });
        }
    }

    private void updateAllUIElements() {
        updateCurrentPathLabel();
        updateDirectoryTreeDisplay();
        fileContentArea.setText("Select a file in the tree or use 'cat' button.");
    }

    private void updateCurrentPathLabel() {
        if (fileSystem != null) {
            currentPathLabel.setText(fileSystem.getCurrentPath()); //
        } else {
            currentPathLabel.setText("Not Initialized");
        }
    }

    private void updateDirectoryTreeDisplay() {
        if (fileSystem != null && fileSystem.tree != null && fileSystemTreeModel != null) {
            DirectoryTree.Node currentTreeRoot = getActualFileSystemRootNode();
            fileSystemTreeModel.updateTreeStructure(currentTreeRoot);

            // Secara otomatis memperluas root node jika tree memiliki baris
            if (directoryTreeComponent.getRowCount() > 0) {
                directoryTreeComponent.expandRow(0);
                // Pilih root node setelah update
                assert currentTreeRoot != null;
                directoryTreeComponent.setSelectionPath(new TreePath(currentTreeRoot));
            }
        } else {
            logMessage("Cannot update tree: FileSystem or TreeModel not ready.");
        }
    }

    private void displayFileContent(String fileName) {
        // Ini adalah fungsi helper untuk menampilkan konten file ke fileContentArea
        // Mirip dengan handleCat tapi tanpa dialog input
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);
        try {
            fileSystem.printFile(fileName); //
            System.out.flush();
            fileContentArea.setText(baos.toString());
        } catch (FileSystemException e) { //
            logMessage("Error displaying file " + fileName + ": " + e.getMessage());
            fileContentArea.setText("Error displaying file: " + e.getMessage());
        } finally {
            System.setOut(oldOut);
        }
    }
    private void showHelpDialog() {

        String helpText = "<html><body style='width: 450px;'>" + "<h2>Daftar Perintah dan Penggunaannya:</h2>" +
            "<h3>Operasi File dan Direktori:</h3>" + // Judul bagian

            // Perintah mkdir
            "<p><b>mkdir &lt;nama_direktori&gt;</b><br>" +
            "Membuat direktori baru di lokasi (path) saat ini.<br>" +
            "<i>Contoh:</i> Jika path saat ini adalah <code>/</code>, masukkan <code>direktoriKu</code> pada dialog untuk membuat direktori <code>/direktoriKu</code>.</p>" +

            // Perintah rmdir
            "<p><b>rmdir</b><br>" +
            "Menghapus direktori <u>saat ini</u>. Operasi ini hanya berhasil jika direktori tersebut kosong. Anda akan diminta konfirmasi sebelum penghapusan.<br>" +
            "<i>Contoh:</i> Jika path saat ini adalah <code>/direktoriKu</code> (dan direktori tersebut kosong), perintah <code>rmdir</code> akan menghapus <code>direktoriKu</code> dan path saat ini akan kembali ke direktori induknya (<code>/</code>).</p>" +

            // Perintah ls
            "<p><b>ls (ke log)</b><br>" +
            "Menampilkan daftar isi (file dan subdirektori) dari direktori saat ini. Output akan ditampilkan di area log di bagian bawah antarmuka.<br>" +
            "<i>Contoh:</i> Jika direktori <code>/direktoriKu</code> berisi file <code>catatan.txt</code> dan subdirektori <code>gambar/</code>, output di log akan menampilkan kedua item tersebut.</p>" +

            // Perintah cd
            "<p><b>cd &lt;path_tujuan&gt;</b><br>" +
            "Mengubah direktori aktif saat ini ke <u>path_tujuan</u>. Path tujuan dapat berupa:<br>" +
            "  - Nama subdirektori yang ada di direktori saat ini (misalnya, <code>subDir</code>).<br>" +
            "  - Path relatif dari direktori saat ini (misalnya, <code>folder/subfolder</code>).<br>" +
            "  - Path absolut dari root direktori (misalnya, <code>/dokumen/penting</code>).<br>" +
            "  - Simbol <code>..</code> untuk pindah ke direktori induk satu tingkat di atas.<br>" +
            "  - Simbol <code>/</code> untuk pindah langsung ke direktori root.<br>" +
            "<i>Contoh:</i> Jika path saat ini <code>/</code>, masukkan <code>dokumen</code> untuk mengubah path ke <code>/dokumen</code>.<br>" +
            "<i>Contoh:</i> Jika path saat ini <code>/dokumen/penting</code>, masukkan <code>..</code> untuk mengubah path ke <code>/dokumen</code>.</p>" +

            // Perintah cp
            "<p><b>cp &lt;nama_sumber&gt; &lt;nama_tujuan&gt;</b><br>" +
            "Menyalin file dari <u>nama_sumber</u> menjadi <u>nama_tujuan</u>. Kedua file (sumber dan tujuan) berada di dalam <u>direktori saat ini</u>.<br>" +
            "<i>Contoh:</i> Jika path saat ini adalah <code>/data</code> dan terdapat file <code>laporan.txt</code>, masukkan <code>laporan.txt</code> sebagai sumber dan <code>laporan_salinan.txt</code> sebagai tujuan. Ini akan membuat file baru <code>/data/laporan_salinan.txt</code> yang isinya sama dengan <code>laporan.txt</code>.</p>" +

            // Perintah mv
            "<p><b>mv &lt;path_sumber&gt; &lt;path_tujuan&gt;</b><br>" +
            "Memindahkan atau mengganti nama (rename) sebuah file atau direktori.<br>" +
            "  - Jika <u>path_tujuan</u> adalah sebuah direktori yang sudah ada, maka item dari <u>path_sumber</u> akan dipindahkan ke dalam direktori tujuan tersebut dengan nama aslinya.<br>" +
            "  - Jika <u>path_tujuan</u> menyertakan nama baru (bukan direktori yang ada), maka item dari <u>path_sumber</u> akan dipindahkan ke lokasi parent dari path_tujuan dan diganti namanya sesuai nama terakhir di path_tujuan.<br>" +
            "<i>Contoh (memindahkan file ke direktori lain):</i> Sumber: <code>/fileA.txt</code>, Tujuan: <code>/dir1</code> (atau bisa juga <code>/dir1/</code>). Hasilnya: <code>fileA.txt</code> akan berada di dalam <code>/dir1/</code> menjadi <code>/dir1/fileA.txt</code>.<br>" +
            "<i>Contoh (mengganti nama file):</i> Sumber: <code>data_lama.txt</code>, Tujuan: <code>data_baru.txt</code> (keduanya di direktori saat ini). Hasilnya: file <code>data_lama.txt</code> akan berganti nama menjadi <code>data_baru.txt</code>.<br>" +
            "<i>Contoh (memindahkan dan mengganti nama file):</i> Sumber: <code>/dir_awal/fileX.doc</code>, Tujuan: <code>/dir_akhir/fileY.doc</code>. Hasilnya: file <code>fileX.doc</code> akan pindah ke <code>/dir_akhir/</code> dan berganti nama menjadi <code>fileY.doc</code>.<br>" +
            "<i>Contoh (memindahkan direktori):</i> Sumber: <code>/folder_proyek</code>, Tujuan: <code>/arsip/</code>. Hasilnya: direktori <code>folder_proyek</code> (beserta isinya) akan pindah menjadi <code>/arsip/folder_proyek</code>.<br>" +
            "<i>Contoh (mengganti nama direktori):</i> Sumber: <code>folder_sementara</code>, Tujuan: <code>folder_final</code> (keduanya di direktori saat ini). Hasilnya: direktori <code>folder_sementara</code> akan berganti nama menjadi <code>folder_final</code>.</p>" +

            // Perintah rm
            "<p><b>rm &lt;nama_file&gt;</b><br>" +
            "Menghapus (delete) sebuah file bernama <u>nama_file</u> yang berada di <u>direktori saat ini</u>. Anda akan diminta konfirmasi sebelum file dihapus secara permanen.<br>" +
            "<i>Contoh:</i> Jika path saat ini <code>/sementara</code>, masukkan <code>buang.tmp</code> pada dialog untuk menghapus file <code>/sementara/buang.tmp</code>.</p>" +
            "<h3>Operasi Isi File:</h3>" +

            // Perintah cat
            "<p><b>cat &lt;nama_file&gt; (lihat)</b><br>" +
            "Menampilkan isi dari file bernama <u>nama_file</u> (yang berada di direktori saat ini) ke area tampilan konten file yang tersedia di antarmuka. Anda juga bisa menampilkan isi file dengan mengklik nama file tersebut pada tampilan struktur direktori (tree view).<br>" +
            "<i>Contoh:</i> Masukkan <code>info.txt</code> pada dialog untuk melihat konten dari file <code>info.txt</code>.</p>" +

            // Perintah write
            "<p><b>write &lt;nama_file&gt; \"&lt;konten&gt;\"</b><br>" +
            "Menulis <u>konten</u> yang diberikan ke dalam file bernama <u>nama_file</u> yang berada di direktori saat ini.<br>" +
            "Jika <u>nama_file</u> sudah ada, isinya akan ditimpa dengan konten baru. Jika belum ada, file baru akan dibuat.<br>" +
            "Gunakan opsi/centang <code>+append</code> pada dialog input untuk menambahkan <u>konten</u> ke bagian akhir dari file yang sudah ada, tanpa menimpa isi sebelumnya.<br>" +
            "<i>Contoh (timpa/buat baru):</i> Nama file: <code>surat.txt</code>, Konten: <code>Ini isi surat saya.</code><br>" +
            "<i>Contoh (tambahkan ke akhir):</i> Centang <code>+append</code>, Nama file: <code>log_harian.txt</code>, Konten: <code>Aktivitas baru telah ditambahkan.</code></p>" +
            "<h3>Interaksi dengan Sistem File Host (Komputer Anda):</h3>" +

            // Perintah import
            "<p><b>import &lt;path_eksternal&gt; &lt;nama_file_di_simulator&gt;</b><br>" +
            "Mengimpor sebuah file dari sistem file komputer Anda (ditentukan oleh <u>path_eksternal</u>) ke dalam <u>direktori saat ini</u> di simulator, dan disimpan dengan nama <u>nama_file_di_simulator</u>.<br>" +
            "Gunakan tombol 'Browse...' pada dialog untuk mempermudah pemilihan file dari komputer Anda.<br>" +
            "<i>Contoh:</i> Path eksternal: <code>C:\\Users\\Andi\\Dokumen\\penting.docx</code>, Nama file di simulator: <code>dokumen_penting.docx</code>. Ini akan mengimpor file tersebut ke direktori aktif di simulator dengan nama <code>dokumen_penting.docx</code>.</p>" +
            // Anda bisa menambahkan penjelasan untuk +append jika sudah ada di dialog import
            // helpText.append("Opsi <code>+append</code> juga tersedia jika Anda ingin menambahkan konten ke file yang diimpor.<br>");

            // Perintah export
            "<p><b>export &lt;nama_file_di_simulator&gt; &lt;path_eksternal&gt;</b><br>" +
            "Mengekspor file bernama <u>nama_file_di_simulator</u> (dari direktori saat ini di simulator) ke sistem file komputer Anda di lokasi yang ditentukan oleh <u>path_eksternal</u>.<br>" +
            "Gunakan tombol 'Browse/Set Lokasi Simpan...' pada dialog untuk memilih direktori dan nama file tujuan di komputer Anda.<br>" +
            "<i>Contoh:</i> Nama file di simulator: <code>hasil_simulasi.csv</code>, Path eksternal: <code>D:\\Kerja\\Proyek\\data_output.csv</code>. Ini akan mengekspor file <code>hasil_simulasi.csv</code> dari simulator dan menyimpannya sebagai <code>data_output.csv</code> di komputer Anda.</p>" +
            "</body></html>";

        // Menggunakan JEditorPane untuk menampilkan teks HTML
        JEditorPane editorPane = new JEditorPane("text/html", helpText);
        editorPane.setEditable(false); // Agar teks tidak bisa diubah oleh pengguna
        // Menambahkan sedikit jarak (padding) di sekitar teks
        editorPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));


        JScrollPane scrollPane = new JScrollPane(editorPane);
        // Mengatur ukuran preferensi dialog agar nyaman dibaca
        scrollPane.setPreferredSize(new Dimension(650, 500));

        JOptionPane.showMessageDialog(this, scrollPane, "Bantuan - Penggunaan Perintah", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- HANDLER TOMBOL ---
    private void handleMkdir() {
        String dirName = JOptionPane.showInputDialog(this, "Enter directory name:", "mkdir", JOptionPane.PLAIN_MESSAGE);
        if (dirName != null && !dirName.trim().isEmpty()) {
            try {
                fileSystem.makeFile(dirName, FileType.DIRECTORY); //
                logMessage("Directory '" + dirName + "' created.");
                updateDirectoryTreeDisplay();
            } catch (FileSystemException e) { //
                logMessage("Error mkdir: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error creating directory: " + e.getMessage(), "Mkdir Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleLs() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);
        fileSystem.listCurrentDir(); //
        System.out.flush();
        System.setOut(oldOut);
        logMessage("ls output for " + fileSystem.getCurrentPath() + ":\n" + baos.toString().trim());
        updateDirectoryTreeDisplay(); // Mungkin tidak perlu jika ls tidak mengubah state tree
    }

    private void handleCd() {
        String path = JOptionPane.showInputDialog(this, "Enter path to change directory to (e.g., dirname, ../, /):", "cd", JOptionPane.PLAIN_MESSAGE);
        if (path != null && !path.trim().isEmpty()) {
            try {
                fileSystem.changeDir(path); //
                logMessage("Changed directory to: " + fileSystem.getCurrentPath());
                updateAllUIElements();
            } catch (FileSystemException e) { //
                logMessage("Error cd: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error changing directory: " + e.getMessage(), "Cd Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCat() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name to display content:", "cat", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            displayFileContent(fileName); // Menggunakan helper method
        }
    }

    private void handleWrite() {
        JTextField fileNameField = new JTextField(20);
        JTextArea contentTextArea = new JTextArea(5, 30);
        JCheckBox appendCheckBox = new JCheckBox("+append");

        JPanel panel = new JPanel(new BorderLayout(5,5));
        JPanel inputFieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,2,2,2);

        inputFieldsPanel.add(new JLabel("File name:"), gbc);
        inputFieldsPanel.add(fileNameField, gbc);
        gbc.weighty = 1.0; // Agar JTextArea bisa membesar
        gbc.fill = GridBagConstraints.BOTH;
        inputFieldsPanel.add(new JLabel("Content:"), gbc);

        panel.add(inputFieldsPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(contentTextArea), BorderLayout.CENTER);
        panel.add(appendCheckBox, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel, "Write to File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String fileName = fileNameField.getText();
            String content = contentTextArea.getText();
            boolean append = appendCheckBox.isSelected();

            if (fileName != null && !fileName.trim().isEmpty()) {
                try {
                    if (append) {
                        fileSystem.appendToFile(fileName, content.getBytes()); //
                        logMessage("Appended to '" + fileName + "'.");
                    } else {
                        fileSystem.writeToFile(fileName, content.getBytes()); //
                        logMessage("Written to '" + fileName + "'.");
                    }
                    updateDirectoryTreeDisplay();
                } catch (FileSystemException e) { //
                    logMessage("Error write: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error writing to file: " + e.getMessage(), "Write Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logMessage("Write operation cancelled or file name empty.");
            }
        }
    }

    private void handleRm() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name to remove:", "rm", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove file '" + fileName + "'?", "Confirm Remove File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmation == JOptionPane.YES_OPTION) {
                try {
                    fileSystem.deleteFile(fileName); //
                    logMessage("File '" + fileName + "' removed.");
                    updateDirectoryTreeDisplay();
                } catch (FileSystemException e) { //
                    logMessage("Error rm: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error removing file: " + e.getMessage(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void handleRmdir() {
        int confirmation = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to remove the CURRENT directory (" + fileSystem.getCurrentPath() + ")?\nThis will only succeed if the directory is empty.",
            "Confirm rmdir",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                String oldPath = fileSystem.getCurrentPath();
                fileSystem.removeDir(); //
                logMessage("Directory '" + oldPath + "' removed.");
                updateAllUIElements();
            } catch (FileSystemException e) { //
                logMessage("Error rmdir: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error removing directory: " + e.getMessage(), "Rmdir Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCp() {
        JTextField sourceField = new JTextField(20);
        JTextField destField = new JTextField(20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,2,2,2);

        panel.add(new JLabel("Source file name:"), gbc);
        panel.add(sourceField, gbc);
        panel.add(new JLabel("Destination file name:"), gbc);
        panel.add(destField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Copy File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String sourceName = sourceField.getText();
            String destName = destField.getText();
            if (sourceName != null && !sourceName.trim().isEmpty() && destName != null && !destName.trim().isEmpty()) {
                try {
                    fileSystem.copyFile(sourceName, destName); //
                    logMessage("File '" + sourceName + "' copied to '" + destName + "'.");
                    updateDirectoryTreeDisplay();
                } catch (FileSystemException e) { //
                    logMessage("Error cp: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error copying file: " + e.getMessage(), "Copy Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logMessage("Copy operation cancelled or names empty.");
            }
        }
    }

    private void handleImport() {
        JTextField extPathField = new JTextField(30);
        JTextField destFileNameField = new JTextField(20);
        // Opsi +append akan membuat dialog lebih kompleks, jadi kita sederhanakan dulu.

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,2,2,2);

        gbc.gridwidth = 2; // Label dan field
        panel.add(new JLabel("External file path (on your computer):"), gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 1.0; // Agar field bisa memanjang
        panel.add(extPathField, gbc);
        gbc.weightx = 0.0; // Reset
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Tombol di akhir baris
        JButton browseExtButton = new JButton("Browse...");
        browseExtButton.addActionListener(evt -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                extPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(browseExtButton, gbc);

        gbc.gridwidth = 2;
        panel.add(new JLabel("Destination file name (in simulator):"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        panel.add(destFileNameField, gbc);


        int result = JOptionPane.showConfirmDialog(this, panel, "Import File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String extPath = extPathField.getText();
            String destFileName = destFileNameField.getText();

            if (extPath != null && !extPath.trim().isEmpty() && destFileName != null && !destFileName.trim().isEmpty()) {
                try {
                    fileSystem.importFile(extPath, destFileName); //
                    logMessage("File '" + extPath + "' imported as '" + destFileName + "'.");
                    updateDirectoryTreeDisplay();
                } catch (FileSystemException e) { //
                    logMessage("Error import: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error importing file: " + e.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logMessage("Import operation cancelled or paths empty.");
            }
        }
    }

    private void handleExport() {
        JTextField fileNameField = new JTextField(20);
        JTextField extPathField = new JTextField(30);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,2,2,2);

        gbc.gridwidth = 2;
        panel.add(new JLabel("File name to export (from simulator):"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        panel.add(fileNameField, gbc);
        gbc.weightx = 0.0;

        gbc.gridwidth = 1;
        panel.add(new JLabel("External destination path (on your computer):"), gbc);
        gbc.weightx = 1.0;
        panel.add(extPathField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JButton browseSaveButton = new JButton("Browse/Set Save Location...");
        browseSaveButton.addActionListener(evt -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to save");
            int returnValue = fileChooser.showSaveDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                extPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(browseSaveButton, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Export File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String fileName = fileNameField.getText();
            String extPath = extPathField.getText();
            if (fileName != null && !fileName.trim().isEmpty() && extPath != null && !extPath.trim().isEmpty()) {
                try {
                    fileSystem.exportFile(fileName, extPath); //
                    logMessage("File '" + fileName + "' exported to '" + extPath + "'.");
                } catch (FileSystemException e) { //
                    logMessage("Error export: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error exporting file: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logMessage("Export operation cancelled or paths empty.");
            }
        }
    }

    private void handleMove() {
        JTextField sourcePathField = new JTextField(30);
        JTextField destPathField = new JTextField(30);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);

        panel.add(new JLabel("Source path (file or directory):"), gbc);
        panel.add(sourcePathField, gbc);
        panel.add(new JLabel("Destination path (directory or new path/name):"), gbc);
        panel.add(destPathField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Move Item (mv)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String sourcePath = sourcePathField.getText();
            String destPath = destPathField.getText();

            if (sourcePath != null && !sourcePath.trim().isEmpty() && destPath != null && !destPath.trim().isEmpty()) {
                try {
                    fileSystem.moveItem(sourcePath, destPath);
                    logMessage("Item '" + sourcePath + "' moved to '" + destPath + "'.");
                    updateAllUIElements(); // Update tree dan path saat ini
                } catch (FileSystemException e) { // Tangkap FileSystemException saja
                    logMessage("Error move: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error moving item: " + e.getMessage(), "Move Error", JOptionPane.ERROR_MESSAGE);
                }
                // IOException seharusnya sudah ditangani dan dibungkus sebagai FileSystemException oleh FileSystem.java
                // Jika Anda ingin menangkap IOException secara terpisah di sini, Anda bisa,
                // tetapi lebih baik jika FileSystem.java yang konsisten melempar FileSystemException.
            } else {
                logMessage("Move operation cancelled or paths empty.");
            }
        }
    }

    // Kustom Cell Renderer untuk ikon folder/file
    private static class FileSystemCellRenderer extends DefaultTreeCellRenderer {
        private Icon folderIcon;
        private Icon fileIcon;
        private final Icon defaultClosedIcon;
        private final Icon defaultOpenIcon;
        private final Icon defaultLeafIcon;


        public FileSystemCellRenderer() {
            defaultClosedIcon = UIManager.getIcon("Tree.closedIcon");
            defaultOpenIcon = UIManager.getIcon("Tree.openIcon");
            defaultLeafIcon = UIManager.getIcon("Tree.leafIcon");
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DirectoryTree.Node node) {
                setText(node.name);
                if (node.type == FileType.DIRECTORY) { //
                    setIcon(expanded ? defaultOpenIcon : defaultClosedIcon);
                } else {
                    setIcon(defaultLeafIcon);
                }
            } else {
                // Fallback untuk node yang bukan DirectoryTree.Node (misalnya, jika tree belum dimuat)
                setIcon(defaultLeafIcon);
            }
            return this;
        }
    }
}
