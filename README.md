# XP Farm Chest Scan

Client-only Fabric mod (Minecraft **26.2**, Fabric Loader 0.19.3+) for Dali's Mega Mob XP Farm building workflow.

Press **V** (rebindable) while standing near your chests: the mod silently "opens" every allowlisted chest within reach on a vanilla server, reads its contents, and writes a JSON dump to the game directory (`xpfarm-chests.json`). Combine with the off-line Checklist app to know exactly what is stored and what is still missing.

- **100% client-side** (`environment: client`) — runs on vanilla servers, no server-side install, no packets the server notices beyond a normal chest open/close.
- **Ownership filter:** only positions YOU add in the config screen are scanned — other players' chests are never touched.
- Vanilla server logic still applies: the server validates reach, so you must stand within ~4.5 blocks of each chest (raise the radius in config at your own risk — further chests simply skip).
- The scan never renders the chest UI (a mixin swallows the screen open, then the container is closed instantly).
- Safety: if both hands hold placeable items (blocks/buckets), the scan refuses to start — nothing can be accidentally placed.

## Install
Drop `xpfarm-chestscan-1.0.0.jar` into `.minecraft/mods`. Requires Fabric Loader ≥ 0.19.3, Fabric API, Minecraft 26.2, Java 25. Mod Menu is optional (recommended for the config screen).

## Usage
1. Open the config: **Mod Menu → XP Farm Chest Scan** (or `V` scan, if no chests are found it tells you).
2. Add your chests: aim at a chest and press **Add targeted chest**, or stand on a chest and press **Add position under me**. Only these positions are ever scanned.
3. Stand among your chests, press **V**. When done a summary screen shows totals per item and the saved file path (`Copy file path` puts it on your clipboard).

## Output format (`xpfarm-chests.json`)
```json
{
  "schema": 1,
  "generatedAt": "2026-08-12T00:00:00",
  "player": "DALI951",
  "radius": 4.5,
  "chests": [
    { "pos": [x, y, z], "items": [ { "id": "minecraft:stone_bricks", "count": 3456 } ] }
  ],
  "totals": { "minecraft:stone_bricks": 3456 },
  "skipped": []
}
```
`totals` is the aggregate map used by the Checklist HTML import.

## Config file
`config/xpfarm-chestscan.json` (also editable in game):
```json
{
  "radius": 4.5,
  "outputFileName": "xpfarm-chests.json",
  "positions": [[x, y, z]]
}
```

## Building from source
JDK 25 + Gradle 9.5.1:
```
./gradlew build
```
Output: `build/libs/xpfarm-chestscan-1.0.0.jar`.

## License
MIT — see LICENSE. Built with Fabric Loom.