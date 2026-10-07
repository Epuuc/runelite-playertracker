package com.PlayerTracker;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PlayerTrackerComponents {
    private final JPanel headerPanel;
    private final JScrollPane scrollPane;
    private final JPanel footerPanel;

    public PlayerTrackerComponents(JPanel headerPanel, JScrollPane scrollPane, JPanel footerPanel) {
        this.headerPanel = headerPanel;
        this.scrollPane = scrollPane;
        this.footerPanel = footerPanel;
    }

    public JPanel getHeaderPanel() {
        return headerPanel;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JPanel getFooterPanel() {
        return footerPanel;
    }
}
