package com.PlayerTracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("PlayerTracker")
public interface PlayerTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "displayTotal",
		name = "Display Total Players",
		description = "Display the total number of players in the area on your screen"
	)
	default boolean displayTotal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "customNamesFilter",
		name = "Custom Names Filter",
		description = "Comma-separated list of player names to filter for."
	)
	default String customNamesFilter()
	{
		return "";
	}

	@ConfigItem(
		keyName = "wipePlayerCacheOnLogin",
		name = "Wipe Player Cache on Login",
		description = "Wipe the list of cached players when you sign back in or hop worlds."
	)
	default boolean wipePlayerCacheOnLogin() {
		return false;
	}

	@ConfigItem (
		keyName = "autoCache",
		name = "Auto Cache",
		description = "Automatically cache any nearby players. (When disabled, creates a button to cache instead)"
	)
	default boolean autoCache() {
		return true;
	}

	@ConfigItem (
		keyName = "showBothTrackers",
		name = "Show Both Trackers",
		description = "Whether or not to show both trackers, or just 1 with a button to toggle between them."
	)
	default boolean showBothTrackers() {
		return true;
	}
}
