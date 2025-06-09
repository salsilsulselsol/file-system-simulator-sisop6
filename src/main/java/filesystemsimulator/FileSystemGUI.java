package filesystemsimulator;

import filesystemsimulator.exceptions.FileSystemException;
import filesystemsimulator.filesystem.FileSystem;
import filesystemsimulator.filestructures.data.DirectoryTree;
import filesystemsimulator.filestructures.data.FileType;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionListener;
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
    private JLabel statsLabel;

    public FileSystemGUI() {
        setTitle("File System Simulator GUI");
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        if (!initializeFileSystemViaDialog()) {
            JOptionPane.showMessageDialog(this, "File system initialization failed or was cancelled. Application will exit.", "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        setupMainUI();
        setVisible(true);
        updateAllUIElements();
    }

    private boolean initializeFileSystemViaDialog() {
        JTextField pathField = new JTextField(System.getProperty("user.home") + File.separator + "filesystem.dat", 30);
        JTextField sizeField = new JTextField("262144", 10);

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
                fileSystem = new FileSystem(path, size);
                return true;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid size format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return initializeFileSystemViaDialog();
            } catch (FileSystemException e) {
                JOptionPane.showMessageDialog(this, "Error initializing file system: " + e.getMessage(), "File System Error", JOptionPane.ERROR_MESSAGE);
                return initializeFileSystemViaDialog();
            }
        }
        return false;
    }

    private void setupMainUI() {
        setLayout(new BorderLayout(5, 5));

        JPanel topPanel = createEnhancedTopPanel();
        add(topPanel, BorderLayout.NORTH);


        directoryTreeComponent = new JTree();
        directoryTreeComponent.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // PERBAIKAN: Menggunakan satu-satunya Cell Renderer yang benar
        directoryTreeComponent.setCellRenderer(new FileSystemCellRenderer());
        directoryTreeComponent.addTreeSelectionListener(e -> {
            TreePath selectedPath = e.getPath();
            if (selectedPath != null) {
                Object lastComponent = selectedPath.getLastPathComponent();
                if (lastComponent instanceof DirectoryTree.Node selectedNode) {
                    if (selectedNode.type == FileType.FILE) {
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
        buttonScrollPane.setMinimumSize(new Dimension(150, 120));
        buttonScrollPane.setPreferredSize(new Dimension(160, 130));

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

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.add(new JLabel("📁 Current Path: "));
        currentPathLabel = new JLabel();
        currentPathLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        currentPathLabel.setForeground(new Color(0, 100, 200));
        pathPanel.add(currentPathLabel);

        statsLabel = new JLabel("💾 Loading stats...");
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // PERBAIKAN UTAMA: Mengganti tombol agar lebih deskriptif
        JButton quickMemoryButton = new JButton("📊 Memory");
        quickMemoryButton.setToolTipText("Show Memory and Storage Visualization");
        quickMemoryButton.setPreferredSize(new Dimension(110, 30));
        quickMemoryButton.addActionListener(e -> handleMemoryVisualization());

        JButton helpButton = new JButton("❓ Help");
        helpButton.setPreferredSize(new Dimension(80, 30));
        helpButton.addActionListener(e -> showHelpDialog());

        rightPanel.add(quickMemoryButton);
        rightPanel.add(helpButton);

        topPanel.add(pathPanel, BorderLayout.WEST);
        topPanel.add(statsLabel, BorderLayout.CENTER);
        topPanel.add(rightPanel, BorderLayout.EAST);

        return topPanel;
    }

    private void handleMemoryVisualization() {
        try {
            JDialog memoryDialog = new JDialog(this, "📊 Memory Visualization", true);
            memoryDialog.setSize(600, 420);
            memoryDialog.setLocationRelativeTo(this);

            MemoryUsagePanel memoryPanel = new MemoryUsagePanel();

            updateMemoryPanel(memoryPanel);

            memoryDialog.add(new JScrollPane(memoryPanel), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton refreshButton = new JButton("🔄 Refresh");
            refreshButton.addActionListener(e -> {
                try {
                    updateMemoryPanel(memoryPanel);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memoryDialog, "Error refreshing data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            buttonPanel.add(refreshButton);
            memoryDialog.add(buttonPanel, BorderLayout.SOUTH);

            memoryDialog.setVisible(true);

        } catch (Exception ex) {
            logMessage("Error showing memory visualization: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Could not display memory visualization: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMemoryPanel(MemoryUsagePanel panel) throws FileSystemException {
        long usedInodes = fileSystem.getUsedInodeCount();
        long totalInodes = fileSystem.getTotalInodeCount();
        long usedDataBlocks = fileSystem.getUsedDataBlockCount();
        long totalDataBlocks = fileSystem.getTotalDataBlockCount();
        long actualFileContentSize = fileSystem.getActualFileContentSize();
        long allocatedFileSpaceSize = fileSystem.getAllocatedFileSpaceSize();
        panel.updateStats(usedInodes, totalInodes, usedDataBlocks, totalDataBlocks, actualFileContentSize, allocatedFileSpaceSize);
    }

    private DirectoryTree.Node getActualFileSystemRootNode() {
        if (fileSystem != null && fileSystem.tree != null) {
            return fileSystem.tree.root;
        }
        return null;
    }

    private JPanel createCommandButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));


        Dimension buttonSize = new Dimension(140, 28);
        Component rigidArea = Box.createRigidArea(new Dimension(0, 5));

        setupButton(createActionButton("mkdir", e -> handleMkdir()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("ls (to log)", e -> handleLs()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("📋 Detailed List", e -> handleDetailedList()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("📊 Dir Info", e -> handleDirectoryInfo()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("cd", e -> handleCd()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("mv", e -> handleMove()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("cat (to view)", e -> handleCat()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("write", e -> handleWrite()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("rm", e -> handleRm()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("rmdir", e -> handleRmdir()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("cp", e -> handleCp()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("import", e -> handleImport()), buttonSize, panel, rigidArea);
        setupButton(createActionButton("export", e -> handleExport()), buttonSize, panel, rigidArea);

        return panel;
    }

    private JButton createActionButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        if (text.contains("Detailed List")) button.setBackground(new Color(230, 240, 255));
        if (text.contains("Dir Info")) button.setBackground(new Color(255, 240, 230));
        return button;
    }


    private void handleDetailedList() {
        try {
            outputArea.append("=== 📋 Detailed Directory Listing ===\n");
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

    // PERBAIKAN: Hanya ada SATU definisi kelas Cell Renderer.
    // Nama kelas diubah menjadi FileSystemCellRenderer dan yang duplikat dihapus.
    private static class FileSystemCellRenderer extends DefaultTreeCellRenderer {
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
                setText(getEnhancedNodeText(node));
                if (node.type == FileType.DIRECTORY) {
                    setIcon(expanded ? defaultOpenIcon : defaultClosedIcon);
                    if (!sel) {
                        setForeground(new Color(0, 100, 200));
                    }
                } else {
                    setIcon(defaultLeafIcon);
                    if (!sel) {
                        setForeground(getFileColor(node.name));
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
                return new Color(100, 100, 100);
            } else if (lowerName.endsWith(".java") || lowerName.endsWith(".py") || lowerName.endsWith(".js")) {
                return new Color(0, 150, 0);
            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".gif")) {
                return new Color(150, 0, 150);
            } else if (lowerName.endsWith(".exe") || lowerName.endsWith(".bin")) {
                return new Color(200, 0, 0);
            } else {
                return new Color(150, 100, 0);
            }
        }
    }

    private void setupButton(JButton button, Dimension size, Container container, Component spacer) {
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Short.MAX_VALUE, size.height));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
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
        updateStatsLabel();
        fileContentArea.setText("Select a file in the tree or use 'cat' button.");
    }

    private void updateStatsLabel() {
        if (fileSystem == null) {
            statsLabel.setText("Not Initialized");
            return;
        }
        try {
            long usedSpace = fileSystem.getUsedDataSpace();
            long totalSpace = fileSystem.getTotalDataSpace();
            String usedFormatted = fileSystem.formatFileSize(usedSpace);
            String totalFormatted = fileSystem.formatFileSize(totalSpace);
            double percentage = (totalSpace == 0) ? 0 : ((double) usedSpace / totalSpace) * 100;
            statsLabel.setText(String.format("💾 Usage: %s / %s (%.1f%%)", usedFormatted, totalFormatted, percentage));
        } catch (FileSystemException e) {
            statsLabel.setText("💾 Error getting stats");
            logMessage("Could not update stats label: " + e.getMessage());
        }
    }

    private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }

        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    private void updateCurrentPathLabel() {
        if (fileSystem != null) {
            currentPathLabel.setText(fileSystem.getCurrentPath());
        } else {
            currentPathLabel.setText("Not Initialized");
        }
    }

    private void updateDirectoryTreeDisplay() {
        if (fileSystem != null && fileSystem.tree != null && fileSystemTreeModel != null) {
            DirectoryTree.Node currentTreeRoot = getActualFileSystemRootNode();
            fileSystemTreeModel.updateTreeStructure(currentTreeRoot);

            if (directoryTreeComponent.getRowCount() > 0) {
                directoryTreeComponent.expandRow(0);
                assert currentTreeRoot != null;
                directoryTreeComponent.setSelectionPath(new TreePath(currentTreeRoot));
            }
        } else {
            logMessage("Cannot update tree: FileSystem or TreeModel not ready.");
        }
    }

    private void displayFileContent(String fileName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);
        try {
            fileSystem.printFile(fileName);
            System.out.flush();
            fileContentArea.setText(baos.toString());
        } catch (FileSystemException e) {
            logMessage("Error displaying file " + fileName + ": " + e.getMessage());
            fileContentArea.setText("Error displaying file: " + e.getMessage());
        } finally {
            System.setOut(oldOut);
        }
    }

    private void showHelpDialog() {
        String helpText = "<html><body style='width: 450px;'>" + "<h2>Daftar Perintah dan Penggunaannya:</h2>" +
            "<h3>Operasi File dan Direktori:</h3>" +
            "<p><b>mkdir &lt;nama_direktori&gt;</b><br>" +
            "Membuat direktori baru di lokasi (path) saat ini.</p>" +
            "<p><b>rmdir</b><br>" +
            "Menghapus direktori <u>saat ini</u>. Operasi ini hanya berhasil jika direktori tersebut kosong.</p>" +
            "<p><b>ls (ke log)</b><br>" +
            "Menampilkan daftar isi dari direktori saat ini ke area log.</p>" +
            "<p><b>cd &lt;path_tujuan&gt;</b><br>" +
            "Mengubah direktori aktif ke <u>path_tujuan</u>. (Gunakan '..' untuk induk, '/' untuk root).</p>" +
            "<p><b>cp &lt;nama_sumber&gt; &lt;nama_tujuan&gt;</b><br>" +
            "Menyalin file dari <u>sumber</u> menjadi <u>tujuan</u> di direktori saat ini.</p>" +
            "<p><b>mv &lt;path_sumber&gt; &lt;path_tujuan&gt;</b><br>" +
            "Memindahkan atau mengganti nama file/direktori.</p>" +
            "<p><b>rm &lt;nama_file&gt;</b><br>" +
            "Menghapus sebuah file bernama <u>nama_file</u> di direktori saat ini.</p>" +
            "<h3>Operasi Isi File:</h3>" +
            "<p><b>cat &lt;nama_file&gt; (lihat)</b><br>" +
            "Menampilkan isi dari file ke area tampilan konten.</p>" +
            "<p><b>write &lt;nama_file&gt; \"&lt;konten&gt;\"</b><br>" +
            "Menulis <u>konten</u> ke dalam file. Timpa atau gunakan <code>+append</code> untuk menambahkan.</p>" +
            "<h3>Interaksi dengan Sistem File Host:</h3>" +
            "<p><b>import &lt;path_eksternal&gt; &lt;nama_di_simulator&gt;</b><br>" +
            "Mengimpor file dari komputer Anda ke dalam simulator.</p>" +
            "<p><b>export &lt;nama_di_simulator&gt; &lt;path_eksternal&gt;</b><br>" +
            "Mengekspor file dari simulator ke komputer Anda.</p>" +
            "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", helpText);
        editorPane.setEditable(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));


        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(650, 500));

        JOptionPane.showMessageDialog(this, scrollPane, "Bantuan - Penggunaan Perintah", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleMkdir() {
        String dirName = JOptionPane.showInputDialog(this, "Enter directory name:", "mkdir", JOptionPane.PLAIN_MESSAGE);
        if (dirName != null && !dirName.trim().isEmpty()) {
            try {
                fileSystem.makeFile(dirName, FileType.DIRECTORY);
                logMessage("Directory '" + dirName + "' created.");
                updateAllUIElements();
            } catch (FileSystemException e) {
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
        fileSystem.listCurrentDir();
        System.out.flush();
        System.setOut(oldOut);
        logMessage("ls output for " + fileSystem.getCurrentPath() + ":\n" + baos.toString().trim());
        updateAllUIElements();
    }

    private void handleCd() {
        String path = JOptionPane.showInputDialog(this, "Enter path to change directory to (e.g., dirname, ../, /):", "cd", JOptionPane.PLAIN_MESSAGE);
        if (path != null && !path.trim().isEmpty()) {
            try {
                fileSystem.changeDir(path);
                logMessage("Changed directory to: " + fileSystem.getCurrentPath());
                updateAllUIElements();
            } catch (FileSystemException e) {
                logMessage("Error cd: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error changing directory: " + e.getMessage(), "Cd Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCat() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name to display content:", "cat", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            displayFileContent(fileName);
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
        gbc.weighty = 1.0;
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
                        fileSystem.appendToFile(fileName, content.getBytes());
                        logMessage("Appended to '" + fileName + "'.");
                    } else {
                        fileSystem.writeToFile(fileName, content.getBytes());
                        logMessage("Written to '" + fileName + "'.");
                    }
                    updateAllUIElements();
                } catch (FileSystemException e) {
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
                    fileSystem.deleteFile(fileName);
                    logMessage("File '" + fileName + "' removed.");
                    updateAllUIElements();
                } catch (FileSystemException e) {
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
                fileSystem.removeDir();
                logMessage("Directory '" + oldPath + "' removed.");
                updateAllUIElements();
            } catch (FileSystemException e) {
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
                    fileSystem.copyFile(sourceName, destName);
                    logMessage("File '" + sourceName + "' copied to '" + destName + "'.");
                    updateAllUIElements();
                } catch (FileSystemException e) {
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

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,2,2,2);

        gbc.gridwidth = 2;
        panel.add(new JLabel("External file path (on your computer):"), gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        panel.add(extPathField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
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
                    fileSystem.importFile(extPath, destFileName);
                    logMessage("File '" + extPath + "' imported as '" + destFileName + "'.");
                    updateAllUIElements();
                } catch (FileSystemException e) {
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
                    fileSystem.exportFile(fileName, extPath);
                    logMessage("File '" + fileName + "' exported to '" + extPath + "'.");
                } catch (FileSystemException e) {
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
                    updateAllUIElements();
                } catch (FileSystemException e) {
                    logMessage("Error move: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error moving item: " + e.getMessage(), "Move Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logMessage("Move operation cancelled or paths empty.");
            }
        }
    }
}
