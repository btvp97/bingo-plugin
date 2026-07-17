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
}
