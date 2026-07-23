# Clan Bingo

A RuneLite plugin for running clan bingo events. Join a team with a code your
clan's board organizer gives you, and the plugin automatically detects
qualifying in-game events (boss/monster kills, item drops, clue completions,
and a handful of other tracked activities) and marks the matching tile
complete — no manual reporting needed. Progress syncs across your whole team
automatically.

## Features

- **Automatic detection** for kill counts (any killable NPC, via the same
  signal that powers RuneLite's own Loot Tracker), item drops from monsters,
  and a specific set of chat-message-driven activities (clue caskets, pet
  drops, Hunter rumours, raid completions, bone burying, and more — see
  `ChatPatterns.java` for the exact list).
- **Automatic team sync** — a teammate completing a tile updates your board
  too, within `POLL_INTERVAL_SECONDS` (currently 20s) of them reporting it,
  not just the player who triggered it. This runs as a background poll over
  plain HTTP rather than a persistent push connection — no extra third-party
  dependency, no client-side socket to keep alive.
- **On-screen feedback** — a pop-up banner when a tile completes, plus a
  persistent overlay showing live progress on any tile the team has started.
- **Sidebar board view** — the full board, current score, team name, and a
  per-tile progress meter (e.g. "6/10").
- **Item-icon tiles** — a tile can optionally show a real in-game item sprite
  (rendered live via RuneLite's `ItemManager`, not a bundled image) instead
  of its text title. Set `iconItemId` on the tile when authoring the board;
  any OSRS item ID works, independent of what the tile actually detects.

## Setup (for players)

1. Install the plugin from the Plugin Hub and enable it.
2. Open its settings and paste in the join code your clan gave you.
3. Log in (or click Refresh in the plugin's side panel) — you'll join
   automatically and see the board.

## How it works / what data this sends

This plugin talks to a backend server operated by the clan running the event
(not a service run by the plugin's author for arbitrary users). Here's
exactly what crosses the network, since that's worth being explicit about:

- **What's sent:** only events the plugin detects about *your own* character
  — e.g. "you killed a Goblin," "you obtained a Bones," "you completed a
  medium clue" — each tagged with your RSN and the team you joined with a
  code. This is the same category of information already visible in your own
  chat box / loot tracker; nothing about other players is read, collected, or
  transmitted.
- **Why:** the backend is what actually tracks board state per team (which
  tiles are complete, current score) and pushes updates to teammates. The
  plugin itself holds no state beyond the current session.
- **Where:** a backend the board organizer deploys and controls (this
  project's reference implementation runs on Railway). The URL is a plugin
  setting, not hardcoded to a single operator's service.
- **Opt-in:** nothing is sent until a join code is entered — the plugin is
  inert without one.

## Development

- `BingoConfig.java` — settings: backend URL and team join code.
- `BingoPanel.java` — the sidebar: board, score, team name, per-tile
  progress, Refresh button.
- `ChatPatterns.java` / `ChatEventDetector.java` — chat-message-based
  detection (pure function, no RuneLite dependencies, easy to unit test).
- `BingoPlugin.java` — wires everything together: join/refresh lifecycle,
  event subscribers (`NpcLootReceived`, `ServerNpcLoot`,
  `ItemContainerChanged`, `ChatMessage`), the background sync poll, overlay/
  sound triggers.
- `TileCompletionOverlay.java` / `BingoProgressOverlay.java` — on-screen UI.
- `BingoPluginTest.java` — run this (under `src/test/java`) to launch a real
  RuneLite client with the plugin loaded, for local development.

### Running it locally

1. Open this folder in IntelliJ and let Gradle sync (first sync downloads
   the RuneLite client jar).
2. Right-click `BingoPluginTest.java` → **Run 'BingoPluginTest.main()'**.
3. Log in, open the plugin's settings, and enter a join code created via the
   backend's `/admin/boards/:id/teams` endpoint.
