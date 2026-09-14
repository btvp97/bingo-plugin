package com.misclickers.bingo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bingo")
public interface BingoConfig extends Config {
    @ConfigItem(
            keyName = "baseUrl",
            name = "Backend URL",
            description = "Base URL of the bingo backend",
            position = 0
    )
    default String baseUrl() {
        return "https://bingo-backend-production-d903.up.railway.app";
    }

    @ConfigItem(
            keyName = "joinCode",
            name = "Team join code",
            description = "The code your bingo organizer gave your team",
            position = 1
    )
    default String joinCode() {
        return "";
    }

    @ConfigItem(
            keyName = "showProgressOverlay",
            name = "Show progress overlay",
            description = "Show the on-screen overlay listing tiles your team has partial progress on",
            position = 2
    )
    default boolean showProgressOverlay() {
        return true;
    }
}
