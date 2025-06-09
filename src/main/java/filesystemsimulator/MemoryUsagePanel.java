package filesystemsimulator;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class MemoryUsagePanel extends JPanel {

    private long usedInodes, totalInodes;
    private long usedDataBlocks, totalDataBlocks;
    private long actualFileContentSize, allocatedFileSpaceSize;

    private static final Color INODE_COLOR = new Color(70, 130, 180, 220); // SteelBlue with alpha
    private static final Color DATA_BLOCK_COLOR = new Color(50, 205, 50, 220); // LimeGreen with alpha
    private static final Color FILE_CONTENT_COLOR = new Color(255, 165, 0, 220); // Orange with alpha
    private static final Color BAR_BG_COLOR = new Color(225, 225, 225);
    private static final Color FONT_COLOR = new Color(30, 30, 30);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font PERCENT_FONT = new Font("SansSerif", Font.BOLD, 12);


    public MemoryUsagePanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    public void updateStats(long usedInodes, long totalInodes, long usedDataBlocks, long totalDataBlocks, long actualFileContentSize, long allocatedFileSpaceSize) {
        this.usedInodes = usedInodes;
        this.totalInodes = totalInodes;
        this.usedDataBlocks = usedDataBlocks;
        this.totalDataBlocks = totalDataBlocks;
        this.actualFileContentSize = actualFileContentSize;
        this.allocatedFileSpaceSize = allocatedFileSpaceSize;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int y = 20;
        int barHeight = 30;
        int spacing = 60; // Increased spacing for labels

        y = drawUsageBar(g2d, y, "📦 Inode Usage", INODE_COLOR, usedInodes, totalInodes, " inodes") + spacing;
        y = drawUsageBar(g2d, y, "💿 Data Block Usage", DATA_BLOCK_COLOR, usedDataBlocks, totalDataBlocks, " blocks") + spacing;
        drawUsageBar(g2d, y, "🗄️ File Data Usage (Logical vs Allocated)", FILE_CONTENT_COLOR, actualFileContentSize, allocatedFileSpaceSize, "");
    }

    private int drawUsageBar(Graphics2D g, int y, String title, Color color, long used, long total, String unit) {
        int x_margin = 20;
        int barWidth = getWidth() - (2 * x_margin);
        int barY = y + 25;
        int barHeight = 30;

        // Draw title
        g.setFont(TITLE_FONT);
        g.setColor(FONT_COLOR);
        g.drawString(title, x_margin, y + 15);

        // Draw bar background
        g.setColor(BAR_BG_COLOR);
        g.fillRoundRect(x_margin, barY, barWidth, barHeight, 10, 10);

        // Draw used portion of the bar
        int usedWidth = 0;
        if (total > 0) {
            usedWidth = (int) (((double) used / total) * barWidth);
        }
        g.setColor(color);
        g.fillRoundRect(x_margin, barY, usedWidth, barHeight, 10, 10);

        // Draw bar outline
        g.setColor(color.darker());
        g.drawRoundRect(x_margin, barY, barWidth, barHeight, 10, 10);

        // Draw percentage text inside the bar
        double percentage = (total == 0) ? 0 : ((double) used / total) * 100;
        DecimalFormat df = new DecimalFormat("0.##");
        String percentText = df.format(percentage) + "%";

        g.setFont(PERCENT_FONT);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int percentWidth = fm.stringWidth(percentText);
        // Draw with a shadow for readability
        g.setColor(Color.BLACK.darker());
        g.drawString(percentText, x_margin + 11, barY + fm.getAscent() + 6);
        g.setColor(Color.WHITE);
        g.drawString(percentText, x_margin + 10, barY + fm.getAscent() + 5);


        // Draw stats text below the bar
        String usedStr = unit.isEmpty() ? formatFileSize(used) : String.valueOf(used);
        String totalStr = unit.isEmpty() ? formatFileSize(total) : String.valueOf(total);
        String statsText = usedStr + unit + "  /  " + totalStr + unit;

        g.setFont(LABEL_FONT);
        g.setColor(FONT_COLOR);
        FontMetrics fmLabel = g.getFontMetrics();
        int textWidth = fmLabel.stringWidth(statsText);
        g.drawString(statsText, getWidth() - x_margin - textWidth, y + 15);


        return barY + barHeight;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
