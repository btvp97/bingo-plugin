package com.misclickers.bingo;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Not a JUnit test — this is RuneLite's documented pattern for running a
 * plugin standalone during development. Run this class's main() from
 * IntelliJ (right-click -> Run) and it launches the real RuneLite client
 * with this plugin sideloaded, so you can log in and click around it live.
 */
public class BingoPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(BingoPlugin.class);
        RuneLite.main(args);
    }
}
