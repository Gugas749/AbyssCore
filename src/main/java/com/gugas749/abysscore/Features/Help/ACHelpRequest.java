package com.gugas749.abysscore.Features.Help;

import java.util.UUID;

/**
 * Represents an active /abysshelp request from a player.
 */
public class ACHelpRequest {

    public final UUID playerUUID;
    public final String playerName;
    public final String reason;
    public final long createdAt;
    public boolean accepted = false;

    public ACHelpRequest(UUID playerUUID, String playerName, String reason) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.reason     = reason;
        this.createdAt  = System.currentTimeMillis();
    }
}
