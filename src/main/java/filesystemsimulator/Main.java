package filesystemsimulator;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Buat dan tampilkan GUI
        SwingUtilities.invokeLater(FileSystemGUI::new);
    }
}
