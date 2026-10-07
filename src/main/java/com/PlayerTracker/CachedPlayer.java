package com.PlayerTracker;

import java.util.Objects;

import net.runelite.api.Player;

public class CachedPlayer {
    
    private Player originalPlayer;

    private final int id;
    private final String name;
    private int combatLevel;
    private boolean isClanMember;
    private boolean isFriend;
    private boolean isFriendsChatMember;

    public CachedPlayer(Player player) {
        this.originalPlayer = player;

        this.id = player.getId();
        this.name = player.getName();
        this.combatLevel = player.getCombatLevel();
        this.isClanMember = player.isClanMember();
        this.isFriend = player.isFriend();
        this.isFriendsChatMember = player.isFriendsChatMember();
    }

    private boolean isOriginalValid() {
        return originalPlayer != null;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getCombatLevel() {
        if (isOriginalValid()) { return originalPlayer.getCombatLevel(); }
        return this.combatLevel;
    }

    public boolean isClanMember() {
        if (isOriginalValid()) { return originalPlayer.isClanMember(); }
        return this.isClanMember;
    }

    public boolean isFriend() {
        if (isOriginalValid()) { return originalPlayer.isFriend(); }
        return this.isFriend;
    }

    public boolean isFriendsChatMember() {
        if (isOriginalValid()) { return originalPlayer.isFriendsChatMember(); }
        return this.isFriendsChatMember;
    }

    public void nullifyOriginalPlayer() {
        if (this.originalPlayer == null) return;

        this.combatLevel = originalPlayer.getCombatLevel();
        this.isClanMember = originalPlayer.isClanMember();
        this.isFriend = originalPlayer.isFriend();
        this.isFriendsChatMember = originalPlayer.isFriendsChatMember();
        this.originalPlayer = null;
    }

    @Override 
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CachedPlayer that = (CachedPlayer) o;

        return this.name == that.name;
    }

    @Override 
    public int hashCode() {
        return Objects.hash(name);
    }

}
