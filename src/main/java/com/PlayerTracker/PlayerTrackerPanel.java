package com.PlayerTracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.NameableContainer;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.config.ConfigPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlayerTrackerPanel extends PluginPanel {
    
    // Header stuff
    private final JButton switchTrackerButton = new JButton("Cached");

    // Live Player List Variables
    private final Map<ButtonType, ButtonFilter> liveButtonTypeMap = new EnumMap<>(ButtonType.class);
    private Set<CachedPlayer> currentLivePlayerList = new HashSet<>();
    private Set<CachedPlayer> currentLiveFilteredPlayerList = new HashSet<>();
    private final JPanel livePlayerTracker;
	private final JPanel livePlayerListContainer = new JPanel();
    private final JLabel livePlayerListTitleLabel = new JLabel("Live Player Tracker (0)");
    private final JButton cacheButton = new JButton("Cache");

    // Cached Player List Variables
    private final Map<ButtonType, ButtonFilter> cachedButtonTypeMap = new EnumMap<>(ButtonType.class);
    private Set<CachedPlayer> currentCachedPlayerList = new HashSet<>();
    private Set<CachedPlayer> currentCachedFilteredPlayerList = new HashSet<>();
    private final JPanel cachedPlayerTracker;
    private final JPanel cachedPlayerListContainer = new JPanel();
    private final JLabel cachedPlayerListTitleLabel = new JLabel("Cached Player Tracker (0)");

    // Configuration Values
    private Set<String> customNamesFilter = new HashSet<>();
    private boolean autoCache = true;

    private static final int MINIMUM_HEIGHT_FOR_BOTH_TRACKERS = 400;

    private final Client client;
    private final ConfigManager config;

    public enum ButtonFilter {
        UNSET, WHITELIST, BLACKLIST
    }

    public enum ButtonType {
        CLAN, FRIENDS_CHAT, FRIENDS, CUSTOM
    }

    public enum ExportType {
        COMMA_SEPARATED, NEW_LINE_SEPARATED, JSON, CSV
    }

    public enum PlayerListType {
        LIVE, CACHED
    }

	public PlayerTrackerPanel(Client client, ConfigManager config, PlayerTrackerOverlay overlay) {
		super();
        this.client = client;
        this.config = config;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

        // set defaults
        for (ButtonType type : ButtonType.values()) {
            liveButtonTypeMap.put(type, ButtonFilter.UNSET);
            cachedButtonTypeMap.put(type, ButtonFilter.UNSET);
        }

        JPanel topLevelFrame = new JPanel();
        topLevelFrame.setLayout(new BoxLayout(topLevelFrame, BoxLayout.Y_AXIS));

        // HEADER TITLE/CONFIG BUTTON
        JPanel headerFrame = new JPanel(new BorderLayout());
        JPanel headerLeftInnerFrame = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JPanel headerRightInnerFrame = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        JLabel titleLabel = new JLabel("Player Tracker");

        Image playerIcon = ImageUtil.loadImageResource(getClass(), "/com/PlayerTracker/player_tracker_icon_64x64.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        JLabel playerIconLabel = new JLabel(new ImageIcon(playerIcon));

        switchTrackerButton.setToolTipText("Open the cached player tracker.");
        switchTrackerButton.setVisible(getHeight() <= MINIMUM_HEIGHT_FOR_BOTH_TRACKERS);
        Image configIcon = ImageUtil.loadImageResource(ConfigPlugin.class, "config_edit_icon.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        JButton configButton = new JButton(new ImageIcon(configIcon));
        configButton.setOpaque(false);
        configButton.addActionListener(e -> overlay.OpenPluginConfiguration());

        headerLeftInnerFrame.add(playerIconLabel);
        headerLeftInnerFrame.add(titleLabel);
        headerRightInnerFrame.add(switchTrackerButton);
        headerRightInnerFrame.add(configButton);
        headerFrame.add(headerLeftInnerFrame, BorderLayout.WEST);
        headerFrame.add(headerRightInnerFrame, BorderLayout.EAST);

        livePlayerTracker = CreateLivePlayerTracker();
        cachedPlayerTracker = CreateCachedPlayerTracker();
        cachedPlayerTracker.setVisible(getHeight() > MINIMUM_HEIGHT_FOR_BOTH_TRACKERS);

        topLevelFrame.add(headerFrame);
        topLevelFrame.add(Box.createRigidArea(new Dimension(0, 10)));
        topLevelFrame.add(livePlayerTracker);
        //topLevelFrame.add(Box.createRigidArea(new Dimension(0, 10)));
        topLevelFrame.add(cachedPlayerTracker);

		add(topLevelFrame, BorderLayout.NORTH);

        switchTrackerButton.addActionListener(e -> {
            if (livePlayerTracker.isVisible()) {
                livePlayerTracker.setVisible(false);
                cachedPlayerTracker.setVisible(true);
                switchTrackerButton.setText("Live");
                switchTrackerButton.setToolTipText("Open the live player tracker.");
            } else {
                livePlayerTracker.setVisible(true);
                cachedPlayerTracker.setVisible(false);
                switchTrackerButton.setText("Cached");
                switchTrackerButton.setToolTipText("Open the cached player tracker.");
            }
        });

        //detect when the screen size changes (doesn't work)
        // addComponentListener(new ComponentAdapter() {
        //     @Override 
        //     public void componentResized(ComponentEvent e) {
        //         log.debug("[PlayerTracker] Screen Height Changed to: "+getHeight());
        //         cachedPlayerTracker.setVisible(getHeight() > MINIMUM_HEIGHT_FOR_BOTH_TRACKERS);
                   //ik this will override which one is visible if the active tracker is switched to cached beforehand, but the event isn't working as I intend
        //         switchTrackerButton.setVisible(getHeight() <= MINIMUM_HEIGHT_FOR_BOTH_TRACKERS);
        //     }
        // });
		
        currentLivePlayerList.clear();
        currentCachedPlayerList.clear();
		updateLivePlayerList();
        updateCachedPlayerList();
	}

    public void clearLivePlayerList() {
        currentLivePlayerList.clear();
    }

    public void clearCachedPlayerList() {
        currentCachedPlayerList.clear();
    }

    public void addPlayer(CachedPlayer player) {
        log.debug("[PlayerTracker] added Player ID "+ player.getId() + " ("+player.getName()+")");

        currentLivePlayerList.removeIf(p -> p.getName().equalsIgnoreCase(player.getName()));
        
        currentLivePlayerList.add(player);
        updateLivePlayerList();
        if (autoCache) {
            currentCachedPlayerList.removeIf(p -> p.getName().equalsIgnoreCase(player.getName()));
            currentCachedPlayerList.add(player);
            updateCachedPlayerList();
        }
    }

    public void removePlayer(int playerID) {
        log.debug("[PlayerTracker] removed Player ID "+playerID);
        CachedPlayer player = currentLivePlayerList.stream()
            .filter(p -> p.getId() == playerID)
            .findFirst()
            .orElse(null);
        if (player == null) return;

        currentLivePlayerList.remove(player);
        player.nullifyOriginalPlayer();

        updateLivePlayerList();
    }

    public void updateCustomFilterList(String customNames) {
        Set<String> customNamesList = new HashSet<>();
		for (String name : customNames.split(",")) {
			StringBuilder regex = new StringBuilder();
			for (char c : name.trim().toCharArray()) {
				if (c == '*') {
					regex.append(".*");
				} else if (c == '?') {
					regex.append(".");
				} else {
					regex.append(Pattern.quote(String.valueOf(c)));
				}
			}
			customNamesList.add(regex.toString());
		}
        customNamesFilter.clear();
        customNamesFilter.addAll(customNamesList);
    }

    public void updateAutoCache(boolean newValue) {
        this.autoCache = newValue;
        cacheButton.setVisible(!autoCache);


        if (newValue) {
            manuallyCacheLivePlayers();
        }
    }

    public void updateShowBothTrackers(boolean newValue) {
        livePlayerTracker.setVisible(true);
        cachedPlayerTracker.setVisible(newValue);
        switchTrackerButton.setVisible(!newValue);
        switchTrackerButton.setText("Cached");
        switchTrackerButton.setToolTipText("Open the cached player tracker.");
    }

    private void updateLivePlayerList() {
        updatePlayerListContainer(currentLivePlayerList, livePlayerListContainer, currentLiveFilteredPlayerList, liveButtonTypeMap, livePlayerListTitleLabel, "Live Player Tracker");
    }

    private void updateCachedPlayerList() {
        updatePlayerListContainer(currentCachedPlayerList, cachedPlayerListContainer, currentCachedFilteredPlayerList, cachedButtonTypeMap, cachedPlayerListTitleLabel, "Cached Player Tracker");
    }

    private void updatePlayerListContainer(Set<CachedPlayer> players, JPanel container, Set<CachedPlayer> filteredPlayers, Map<ButtonType, ButtonFilter> buttonMap, JLabel titleLabel, String title) {
        SwingUtilities.invokeLater(() -> {
			container.removeAll();
            
            filteredPlayers.clear();
            filteredPlayers.addAll(players);
            Set<String> fullWhitelist = new HashSet<>();
            Set<String> fullBlacklist = new HashSet<>();

            if (buttonMap.get(ButtonType.CLAN) != ButtonFilter.UNSET) {
                Set<String> clanMemberNames = getClanMemberNames();
                if (buttonMap.get(ButtonType.CLAN) == ButtonFilter.WHITELIST) {
                    fullWhitelist.addAll(clanMemberNames);
                } else if (buttonMap.get(ButtonType.CLAN) == ButtonFilter.BLACKLIST) {
                    fullBlacklist.addAll(clanMemberNames);
                }
            }
            if (buttonMap.get(ButtonType.FRIENDS_CHAT) != ButtonFilter.UNSET) {
                Set<String> friendsChatMemberNames = getFriendsChatMemberNames();
                if (buttonMap.get(ButtonType.FRIENDS_CHAT) == ButtonFilter.WHITELIST) {
                    fullWhitelist.addAll(friendsChatMemberNames);
                } else if (buttonMap.get(ButtonType.FRIENDS_CHAT) == ButtonFilter.BLACKLIST) {
                    fullBlacklist.addAll(friendsChatMemberNames);
                }
            }
            if (buttonMap.get(ButtonType.FRIENDS) != ButtonFilter.UNSET) {
                Set<String> friendNames = getFriendNames();
                if (buttonMap.get(ButtonType.FRIENDS) == ButtonFilter.WHITELIST) {
                    fullWhitelist.addAll(friendNames);
                } else if (buttonMap.get(ButtonType.FRIENDS) == ButtonFilter.BLACKLIST) {
                    fullBlacklist.addAll(friendNames);
                }
            }
            if (buttonMap.get(ButtonType.CUSTOM) != ButtonFilter.UNSET) {
                if (buttonMap.get(ButtonType.CUSTOM) == ButtonFilter.WHITELIST) {
                    fullWhitelist.addAll(customNamesFilter);
                } else if (buttonMap.get(ButtonType.CUSTOM) == ButtonFilter.BLACKLIST) {
                    fullBlacklist.addAll(customNamesFilter);
                }
            }
            if (!fullWhitelist.isEmpty()) {
                filteredPlayers.removeIf(player -> !listContains(fullWhitelist, player.getName()));
            }
            if (!fullBlacklist.isEmpty()) {
                filteredPlayers.removeIf(player -> listContains(fullBlacklist, player.getName()));
            }

            if (players == null || players.isEmpty()) {
                titleLabel.setText(title+" (0)");
			} else {
                titleLabel.setText(title+" ("+filteredPlayers.size()+")");
                List<CachedPlayer> sortedList = new ArrayList<>(filteredPlayers);
                sortedList.sort(Comparator.comparing(CachedPlayer::getName, String.CASE_INSENSITIVE_ORDER));
				for (CachedPlayer player : sortedList) {
					container.add(createPlayerCard(player));
				}
			}

			container.revalidate();
			container.repaint();
        });
    }

    private PlayerTrackerComponents CreateGenericTracker(Map<ButtonType, ButtonFilter> buttonTypeMap, JPanel playersContainer, Set<CachedPlayer> filteredPlayerList, JLabel headerTitleLabel, PlayerListType trackerType) {
        // HEADER PANEL
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        headerTitleLabel.setFont(FontManager.getRunescapeBoldFont());
        headerTitleLabel.setForeground(Color.WHITE);
        headerTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        headerPanel.add(headerTitleLabel, BorderLayout.WEST);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // FILTER BUTTONS PANEL
        JPanel buttonGroup = new JPanel(new GridBagLayout());
        buttonGroup.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonGroup.setAlignmentX(LEFT_ALIGNMENT);
        buttonGroup.setMaximumSize(new Dimension(PANEL_WIDTH, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 2, 0, 2);

        JLabel filterLabel = new JLabel("Filter: ");
        filterLabel.setFont(FontManager.getRunescapeSmallFont());
        filterLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        buttonGroup.add(filterLabel, gbc);
        JButton clanButton = new JButton("Clan");
        JButton friendsChatButton = new JButton("FC");
        JButton friendsButton = new JButton("Friends");
        JButton customButton = new JButton("Custom");
        clanButton.setMargin(new Insets(2, 0, 2, 0));
        friendsChatButton.setMargin(new Insets(2, 0, 2, 0));
        friendsButton.setMargin(new Insets(2, 0, 2, 0));
        customButton.setMargin(new Insets(2, 0, 2, 0));
        gbc.weightx = 1.0;
        gbc.gridx = 1;
        buttonGroup.add(clanButton, gbc);
        gbc.gridx = 2;
        buttonGroup.add(friendsChatButton, gbc);
        gbc.gridx = 3;
        buttonGroup.add(friendsButton, gbc);
        gbc.gridx = 4;
        buttonGroup.add(customButton, gbc);
        headerPanel.add(buttonGroup);
        clanButton.addActionListener(e -> onFilterButtonClicked(ButtonType.CLAN, clanButton, buttonTypeMap, trackerType));
        friendsChatButton.addActionListener(e -> onFilterButtonClicked(ButtonType.FRIENDS_CHAT, friendsChatButton, buttonTypeMap, trackerType));
        friendsButton.addActionListener(e -> onFilterButtonClicked(ButtonType.FRIENDS, friendsButton, buttonTypeMap, trackerType));
        customButton.addActionListener(e -> onFilterButtonClicked(ButtonType.CUSTOM, customButton, buttonTypeMap, trackerType));
        log.debug("[PlayerTracker] Built buttons for "+trackerType);

        // SCROLLABLE CONTAINER WITH ALL LIVE PLAYERS
		playersContainer.setLayout(new BoxLayout(playersContainer, BoxLayout.Y_AXIS));
		playersContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        playersContainer.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR));

		JScrollPane scrollPane = new JScrollPane(playersContainer);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        scrollPane.setPreferredSize(new Dimension(PANEL_WIDTH - 10, 220));

        // EXPORT BUTTON
        JButton exportButton = new JButton("Export");
        exportButton.setMargin(new Insets(2, 0, 2, 0));
        JPopupMenu exportOptionsMenu = new JPopupMenu();
        JMenuItem exportCommaSeparated = new JMenuItem("Export (comma separated)");
        JMenuItem exportNewLineSeparated = new JMenuItem("Export (line separated)");
        //JMenuItem exportAsJSON = new JMenuItem("Export (JSON)");
        //JMenuItem exportAsCSV = new JMenuItem("Export (CSV)");
        exportOptionsMenu.add(exportCommaSeparated);
        exportOptionsMenu.add(exportNewLineSeparated);
        //exportOptionsMenu.add(exportAsJSON);
        //exportOptionsMenu.add(exportAsCSV);
        exportButton.addActionListener(e -> {
            exportOptionsMenu.show(exportButton, 0, exportButton.getHeight());
        });
        exportCommaSeparated.addActionListener(e -> HandleExport(ExportType.COMMA_SEPARATED, filteredPlayerList));
        exportNewLineSeparated.addActionListener(e -> HandleExport(ExportType.NEW_LINE_SEPARATED, filteredPlayerList));
        //exportAsJSON.addActionListener(e -> HandleExport(ExportType.JSON, filteredPlayerList));
        //exportAsCSV.addActionListener(e -> HandleExport(ExportType.CSV, filteredPlayerList));

        // CUSTOM FILTER BUTTON
        JButton customFilterButton = new JButton("Custom Filter");
        customFilterButton.setMargin(new Insets(2, 0, 2, 0));
        customFilterButton.setToolTipText("Will set the Custom Filter contents to this list of players");
        customFilterButton.addActionListener(e -> {
            String filteredListAsString = "";
            for (CachedPlayer plr : filteredPlayerList) {
                filteredListAsString += plr.getName() + ", ";
            }
            config.setConfiguration("PlayerTracker", "customNamesFilter", filteredListAsString);
        });

        JPanel footerFrame = new JPanel();
        footerFrame.add(exportButton);
        footerFrame.add(customFilterButton);

        return new PlayerTrackerComponents(headerPanel, scrollPane, footerFrame);
    }

    private JPanel CreateLivePlayerTracker() {

        PlayerTrackerComponents components = CreateGenericTracker(liveButtonTypeMap, livePlayerListContainer, currentLiveFilteredPlayerList, livePlayerListTitleLabel, PlayerListType.LIVE);

        cacheButton.setMargin(new Insets(2, 0, 2, 0));
        cacheButton.setVisible(!autoCache);
        cacheButton.setToolTipText("Manually add the current live player list to the cached player list.");
        cacheButton.addActionListener(e -> manuallyCacheLivePlayers());
        components.getFooterPanel().add(cacheButton);

        // WRAPPER PANEL TO HOLD EVERYTHING TOGETHER
        JPanel frameWrapper = new JPanel(new BorderLayout());
        frameWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        frameWrapper.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR, 1));
        frameWrapper.add(components.getHeaderPanel(), BorderLayout.NORTH);
        frameWrapper.add(components.getScrollPane(), BorderLayout.CENTER);
        frameWrapper.add(components.getFooterPanel(), BorderLayout.SOUTH);



        return frameWrapper;
    }

    private JPanel CreateCachedPlayerTracker() {
        PlayerTrackerComponents components = CreateGenericTracker(cachedButtonTypeMap, cachedPlayerListContainer, currentCachedFilteredPlayerList, cachedPlayerListTitleLabel, PlayerListType.CACHED);

        JButton clearButton = new JButton("Clear");
        clearButton.setMargin(new Insets(2, 0, 2, 0));
        clearButton.addActionListener(e -> {
            currentCachedPlayerList.clear();
            updateCachedPlayerList();
        });
        components.getFooterPanel().add(clearButton);

        // WRAPPER PANEL TO HOLD EVERYTHING TOGETHER
        JPanel frameWrapper = new JPanel(new BorderLayout());
        frameWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        frameWrapper.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR, 1));
        frameWrapper.add(components.getHeaderPanel(), BorderLayout.NORTH);
        frameWrapper.add(components.getScrollPane(), BorderLayout.CENTER);
        frameWrapper.add(components.getFooterPanel(), BorderLayout.SOUTH);

        return frameWrapper;
    }

	private JPanel createPlayerCard(CachedPlayer player) {
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        Dimension cardSize = new Dimension(PANEL_WIDTH - 22, 28);
        card.setMaximumSize(cardSize);
        card.setPreferredSize(cardSize);

		JLabel nameLabel = new JLabel(player.getName());
		nameLabel.setFont(FontManager.getRunescapeFont());
		nameLabel.setForeground(Color.WHITE);

		JLabel combatLabel = new JLabel("Lvl: " + player.getCombatLevel());
		combatLabel.setFont(FontManager.getRunescapeSmallFont());
		combatLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		card.add(nameLabel, BorderLayout.WEST);
		card.add(combatLabel, BorderLayout.EAST);

		return card;
	}

    private void onFilterButtonClicked(ButtonType buttonType, JButton button, Map<ButtonType, ButtonFilter> buttonMap, PlayerListType trackerType) {


        ButtonFilter newStatus = ButtonFilter.UNSET;

        if (buttonMap.get(buttonType) == ButtonFilter.UNSET) {
            newStatus = ButtonFilter.WHITELIST;
            button.setBackground(Color.GREEN);
            button.setToolTipText(newStatus.toString());
        } else if (buttonMap.get(buttonType) == ButtonFilter.WHITELIST) {
            newStatus = ButtonFilter.BLACKLIST;
            button.setBackground(Color.RED);
            button.setToolTipText(newStatus.toString());
        } else if (buttonMap.get(buttonType) == ButtonFilter.BLACKLIST) {
            newStatus = ButtonFilter.UNSET;
            button.setToolTipText(null);
            button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        }


        log.debug("[PlayerTracker] updating `"+buttonType+"` from `"+buttonMap.get(buttonType)+"` to `"+newStatus+"` for `"+trackerType+"`.");

        buttonMap.put(buttonType, newStatus);

        if (trackerType == PlayerListType.LIVE) { updateLivePlayerList(); }
        if (trackerType == PlayerListType.CACHED) { updateCachedPlayerList(); }

    }

    private Set<String> getClanMemberNames() {
        Set<String> memberNames = new HashSet<>();
        if (client == null) { return memberNames; }

        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null) { return memberNames; }
        for (ClanChannelMember member : clanChannel.getMembers()) {
            memberNames.add(member.getName());
        }
        return memberNames;
    }

    private Set<String> getFriendsChatMemberNames() {
        Set<String> memberNames = new HashSet<>();
        if (client == null) { return memberNames; }

        FriendsChatManager friendsChat = client.getFriendsChatManager();
        if (friendsChat == null) { return memberNames; }
        for (FriendsChatMember member : friendsChat.getMembers()) {
            memberNames.add(member.getName());
        }
        
        
        return memberNames;
    }

    private Set<String> getFriendNames() {
        Set<String> friendNames = new HashSet<>();
        if (client == null) { return friendNames; }

        NameableContainer<Friend> friendContainer = client.getFriendContainer();
        for (net.runelite.api.Friend friend : friendContainer.getMembers()) {
            friendNames.add(friend.getName());
        }
        return friendNames;
    }

    private void HandleExport(ExportType exportType, Set<CachedPlayer> list) {
        StringBuilder exportBuilder = new StringBuilder();
        List<CachedPlayer> sortedList = new ArrayList<>(list);
        sortedList.sort(Comparator.comparing(CachedPlayer::getName, String.CASE_INSENSITIVE_ORDER));
        for (CachedPlayer player : sortedList) {
            if (exportType == ExportType.COMMA_SEPARATED) {
                exportBuilder.append(player.getName()).append(", ");
            } else if (exportType == ExportType.NEW_LINE_SEPARATED) {
                exportBuilder.append(player.getName()).append("\n");
            } else if (exportType == ExportType.JSON) {
                // to do later
            } else if (exportType == ExportType.CSV) {
                // to do later
            }
        }
        StringSelection exportContent = new StringSelection(exportBuilder.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(exportContent, null);
    }

    private static boolean listContains(Set<String> list, String toMatch) {
        if (list == null || list.isEmpty() || toMatch == null) { return false; }
        toMatch = toMatch.replace("\u00A0", " ").trim();
        if (toMatch.isEmpty()) { return false; }
        for (String listItem : list) {
            if (listItem == null || listItem.trim().isEmpty()) { continue; }

            if (toMatch.matches("(?i)"+ listItem)) { return true; }
        }
        return false;
    }

    private void manuallyCacheLivePlayers() {
        for (CachedPlayer player : currentLivePlayerList) {
            currentCachedPlayerList.removeIf(p -> p.getName().equalsIgnoreCase(player.getName()));
            currentCachedPlayerList.add(player);
        }
        updateCachedPlayerList();
    }
}