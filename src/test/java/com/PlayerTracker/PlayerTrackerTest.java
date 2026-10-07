package com.PlayerTracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PlayerTrackerTest
{
	public static void main(String[] args) throws Exception
	{
		// Load your custom plugin class into RuneLite's plugin manager
		ExternalPluginManager.loadBuiltin(PlayerTrackerPlugin.class);
		
		// Boot the RuneLite client
		RuneLite.main(args);
	}
}