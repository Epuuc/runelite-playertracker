package com.PlayerTracker;

import java.awt.image.BufferedImage;

import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PluginDescriptor(
	name = "Player Tracker"
)
public class PlayerTrackerPlugin extends Plugin {
	// Variables to track players

	// UI Elements
	private PlayerTrackerPanel panel;
	private NavigationButton navButton;

	// Injected dependencies
	@Inject
	private PlayerTrackerConfig config;

	@Inject 
	private ConfigManager cfgManager;

	@Inject 
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private PlayerTrackerOverlay overlay;

	@Provides
	PlayerTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(PlayerTrackerConfig.class);
	}

	@Override
	protected void startUp() throws Exception {

		panel = new PlayerTrackerPanel(client, cfgManager, overlay);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/PlayerTracker/player_tracker_icon_64x64.png");

		navButton = NavigationButton.builder()
			.tooltip("Player Tracker")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.debug("[PlayerTracker] Plugin started. Navigation button added.");
		panel.updateCustomFilterList(config.customNamesFilter());
		panel.updateAutoCache(config.autoCache());
		panel.updateShowBothTrackers(config.showBothTrackers());

	}

	@Override 
	public void shutDown() throws Exception {
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		// Listen for live toggle changes in the settings panel
		if (event.getGroup().equals("PlayerTracker")) {
			if (event.getKey().equals("customNamesFilter")) {
				panel.updateCustomFilterList(config.customNamesFilter());
			}
			if (event.getKey().equals("autoCache")) {
				panel.updateAutoCache(config.autoCache());
			}
			if (event.getKey().equals("showBothTrackers")) {
				panel.updateShowBothTrackers(config.showBothTrackers());
			}
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event) {
		Player player = event.getPlayer();

		if (player == null) { return; }
		
		panel.addPlayer(new CachedPlayer(player));
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event) {
		Player player = event.getPlayer();

		if (player == null) { return; }

		panel.removePlayer(player.getId());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGING_IN || event.getGameState() == GameState.HOPPING) {
			panel.clearLivePlayerList();
			if (config.wipePlayerCacheOnLogin()) panel.clearCachedPlayerList();
			log.debug("[PlayerTracker] Game state changed to " + event.getGameState() + ". Cleared player lists.");
		}
	}
}