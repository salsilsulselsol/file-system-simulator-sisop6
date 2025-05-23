package filesystemsimulator; // Paket sejajar dengan Main.java

import filesystemsimulator.exceptions.FileSystemException; // Impor diperlukan
import filesystemsimulator.filesystem.FileSystem; // Impor diperlukan
import filesystemsimulator.filestructures.data.DirectoryTree; // Impor diperlukan
import filesystemsimulator.filestructures.data.FileType; // Impor diperlukan


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
        JPanel topPanel = new JPanel(new BorderLayout());
        currentPathLabel = new JLabel();
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.add(new JLabel("Current Path: "));
        pathPanel.add(currentPathLabel);
        topPanel.add(pathPanel, BorderLayout.CENTER);

        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(e -> showHelpDialog());
        JPanel helpButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        helpButtonPanel.add(helpButton);
        topPanel.add(helpButtonPanel, BorderLayout.EAST);

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

        JButton cdButton = new JButton("cd");
        setupButton(cdButton, buttonSize, panel, rigidArea);
        cdButton.addActionListener(e -> handleCd());

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

        JButton importButton = new JButton("import");
        setupButton(importButton, buttonSize, panel, rigidArea);
        importButton.addActionListener(e -> handleImport());

        JButton exportButton = new JButton("export");
        setupButton(exportButton, buttonSize, panel, rigidArea);
        exportButton.addActionListener(e -> handleExport());

        return panel;
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
            currentPathLabel.setText(fileSystem.getSystemPath()); //
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
        // Mengambil usages dari Command.COMMAND_USAGES
        // Command.printCommandUsages() mencetak ke System.out
        // Jadi kita bisa menangkapnya atau membuat string secara manual.
        // Untuk kesederhanaan, kita buat string.
        StringBuilder helpText = new StringBuilder("Commands and their usages:\n\n");
        // Anda bisa mengakses Command.COMMAND_USAGES jika public static final String[]
        // atau mereplikasi string di sini.
        // Asumsi Command.COMMAND_USAGES tidak bisa diakses langsung, jadi kita hardcode.
        // Lebih baik jika Command.java menyediakan metode getUsageStrings()
        String[] usages = { // Diambil dari Command.java
            "mkdir <dir_name>",
            "rmdir (removes current directory if empty)",
            "ls (lists content of current directory in log)",
            "cd <name> or cd <name1/name2/...> or cd .. or cd /",
            "cp <source_name> <dest_name>",
            "rm <file_name>",
            "cat <file_name> (shows content in the view area)",
            "write <file_name> \"<content>\" or write +append <file_name> \"<content>\"",
            "import <ext_path> <file_name> or import +append <ext_path> <file_name> \"<content>\"",
            "export <file_name> <ext_path>"
        };
        for (String usage : usages) {
            helpText.append(usage).append("\n");
        }

        JTextArea helpTextArea = new JTextArea(helpText.toString());
        helpTextArea.setEditable(false);
        helpTextArea.setWrapStyleWord(true);
        helpTextArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(helpTextArea);
        scrollPane.setPreferredSize(new Dimension(450, 300));
        JOptionPane.showMessageDialog(this, scrollPane, "Help - Command Usages", JOptionPane.INFORMATION_MESSAGE);
    }


    // --- HANDLER TOMBOL ---
    // (Implementasi handler dari respons sebelumnya, pastikan memanggil
    //  logMessage(...) dan updateAllUIElements() atau updateDirectoryTreeDisplay()
    //  di tempat yang sesuai)

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
        logMessage("ls output for " + fileSystem.getSystemPath() + ":\n" + baos.toString().trim());
        updateDirectoryTreeDisplay(); // Mungkin tidak perlu jika ls tidak mengubah state tree
    }

    private void handleCd() {
        String path = JOptionPane.showInputDialog(this, "Enter path to change directory to (e.g., dirname, ../, /):", "cd", JOptionPane.PLAIN_MESSAGE);
        if (path != null && !path.trim().isEmpty()) {
            try {
                fileSystem.changeDir(path); //
                logMessage("Changed directory to: " + fileSystem.getSystemPath());
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
            "Are you sure you want to remove the CURRENT directory (" + fileSystem.getSystemPath() + ")?\nThis will only succeed if the directory is empty.",
            "Confirm rmdir",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                String oldPath = fileSystem.getSystemPath();
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
