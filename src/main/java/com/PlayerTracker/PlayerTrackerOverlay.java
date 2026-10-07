package com.PlayerTracker;

import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.inject.Inject;

import net.runelite.api.MenuAction;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PlayerTrackerOverlay extends OverlayPanel {
    
    private EventBus eventBus;

    @Inject
    public PlayerTrackerOverlay(PlayerTrackerPlugin plugin, EventBus eventBus) {
        super(plugin);
        this.eventBus = eventBus;

        setPosition(OverlayPosition.TOP_LEFT);

        // Register the right-click menu entry on the overlay
        getMenuEntries().add(new OverlayMenuEntry(
            MenuAction.RUNELITE_OVERLAY_CONFIG,
            "Configure",
            "Player Tracker"
        ));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        return super.render(graphics);
    }

    public void OpenPluginConfiguration() {
        eventBus.post(new OverlayMenuClicked(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "Player Tracker"), this));
    }
}
