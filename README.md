# Fastplace

Fastplace by **CrazyLadAlex** places a line of blocks in the direction of the block you place. Choose a distance with `/fp` or `/fastplace`, then place a block normally. The extra blocks are placed and taken from your inventory in one batch.

Supports Paper and its forks, including Leaf, on Minecraft **1.21 through 26.3**. The plugin uses Java 21 bytecode. Run your server with the Java version that its Minecraft version requires. Folia and vanilla servers are not supported.

## Build from source

Install a JDK 21 or newer and Maven, then run:

```shell
git clone https://github.com/CrazyLadAlexx/FastPlace.git
cd FastPlace
mvn clean package
```

The compiled plugin is written to:

```text
target/FastPlace-1.0.jar
```

To rebuild after changing the source, run `mvn clean package` again. The server API is provided by Paper and is not bundled into the JAR.

## Install on a server

1. Stop your Paper or Leaf server.
2. Build the plugin using the commands above.
3. Copy `target/FastPlace-1.0.jar` into the server's `plugins` folder. Replace the existing Fastplace JAR when updating.
4. Start the server. Fastplace creates its configuration and message files automatically.
5. Grant players a permission tier, then use `/fp <amount>` in game.

The server files will look like this:

```text
server/
├── server.jar
└── plugins/
    ├── FastPlace-1.0.jar
    └── Fastplace/
        ├── config.yml
        └── messages.yml
```

Restart the server after editing either configuration file.

## Commands

| Command | Action |
| --- | --- |
| `/fp <amount>` | Enable fastplace with the chosen distance. |
| `/fastplace <amount>` | The full command, with the same behaviour. |
| `/fp` or `/fastplace` | Disable fastplace. |

The amount is the number of **additional** placements after the original block. `/fp 9` with 10 matching seeds plants 10 crops in total. `/fp 20` with 17 dirt places the original block and up to 16 extra blocks, then sends the out-of-blocks message once.

## Permissions

Assign these nodes through your server's permission plugin:

| Permission | Available amounts |
| --- | --- |
| `fastplace.16` | 1-16 |
| `fastplace.32` | 1-32 |
| `fastplace.64` | 1-64 |
| `fastplace.128` | 1-128 |

Higher tiers include the lower tiers. Operators receive the permissions by default. Permission denials are respected. Players without a tier receive the unlock message when they use the command.

## How placement works

Ordinary blocks extend along the face used for the original placement. Plants extend horizontally in the direction you are looking. If the first space in that direction cannot support the plant, Fastplace checks another horizontal direction. Seeds need farmland, and saplings need suitable ground.

A line stops at an occupied block, invalid support, a non-player object, a protected placement, an unloaded chunk, a world border, or the build-height limit. Players standing in the path do not stop it.

Matching items are consumed from other inventory slots and the other hand before the placement hand. Item metadata must match, so a named or modified item is not spent in place of an ordinary block. Armour slots are excluded. Creative mode does not consume items.

Each line runs as one batch after the original placement has finished, normally on the next server tick. If several players are placing lines at once, the queue can defer later lines to another tick. It does not spread individual lines over multiple ticks.

Stored container contents and locks, head textures, and both sign sides are preserved. Sign text, colour, glow, and wax state are copied with the block state. Extra placements fire the usual Bukkit placement events so protection plugins can cancel them. Cancelled or unpaid placements are restored without spending items. Powered TNT is primed after placement approval and inventory deduction.

Disabling fastplace, changing the held slot or swapping hands, leaving the world, dying, or disconnecting cancels pending placement. Placing a new original block replaces that player's pending line.

## Configuration

Edit `plugins/Fastplace/config.yml`:

```yaml
max-distance: 128
tick-budget-millis: 4.0
max-batches-per-tick: 8
```

| Setting | Purpose |
| --- | --- |
| `max-distance` | Global distance limit. Cannot exceed 128. |
| `tick-budget-millis` | Time available before the queue defers another complete line to the next tick. |
| `max-batches-per-tick` | Maximum number of queued lines processed in one tick. |

The time budget is checked between lines. A single line or a slow placement-event handler can take longer than that budget. Lower `max-distance` if large lines take too much time on your server.

## Messages

Edit `plugins/Fastplace/messages.yml`:

```yaml
prefix: '&6&lFastplace &8» '
enabled: '{prefix}&fEnabled fastplace with &6{amount} &fdistance!'
disabled: '{prefix}&cFastplace Disabled'
locked: '&c&l(!) &cYou have not unlocked fastplace!'
usage: '&c&l(!) &cUsage: /fp 18 - /fastplace 18'
exhausted: '{prefix}&cYou have run out of blocks!'
```

Messages accept legacy `&` colour codes. `{prefix}` inserts the prefix, and `{amount}` inserts the distance in the enabled message. Italics are disabled.

## Project structure

```text
FastPlace/
├── .gitignore
├── pom.xml
├── README.md
└── src/main/
    ├── java/me/alex/fastplace/
    │   ├── Fastplace.java
    │   ├── api/
    │   │   └── FastPlaceAPI.java
    │   ├── command/
    │   │   └── FastplaceCommand.java
    │   ├── inventory/
    │   │   └── InventoryPlan.java
    │   ├── listener/
    │   │   └── PlacementListener.java
    │   ├── message/
    │   │   └── Messages.java
    │   ├── placement/
    │   │   ├── BlockMetadata.java
    │   │   ├── DirectionResolver.java
    │   │   ├── PlacementBounds.java
    │   │   ├── PlacementRequest.java
    │   │   ├── PlacementService.java
    │   │   ├── PlacementTemplate.java
    │   │   └── PlacementTransaction.java
    │   └── player/
    │       └── PlayerSettings.java
    └── resources/
        ├── config.yml
        ├── messages.yml
        └── plugin.yml
```

`Fastplace.java` loads the plugin, registers commands and listeners, and exposes the API. The `placement` package handles directions, block data, bounds, and queued placement. The other packages contain the commands, inventory deductions, event handling, messages, and player settings. Maven writes compiled files to `target/`, which is excluded from Git.

## Use the API in another plugin

The API class is `me.alex.fastplace.api.FastPlaceAPI`.

First install Fastplace into your local Maven repository from this project:

```shell
mvn clean install
```

Add this dependency to the other plugin's `pom.xml`:

```xml
<dependency>
    <groupId>me.alex</groupId>
    <artifactId>fastplace</artifactId>
    <version>1.0</version>
    <scope>provided</scope>
</dependency>
```

Add the dependency declaration to that plugin's `plugin.yml` so Fastplace loads first:

```yaml
depend: [Fastplace]
```

Acquire the API during startup, then use it from your commands or listeners:

```java
package example.plugin;

import me.alex.fastplace.api.FastPlaceAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePlugin extends JavaPlugin {
    private FastPlaceAPI fastplace;

    @Override
    public void onEnable() {
        fastplace = FastPlaceAPI.getInstance();
    }

    public boolean enableFastplace(Player player, int distance) {
        return fastplace.enable(player, distance);
    }

    public void disableFastplace(Player player) {
        fastplace.disable(player);
    }
}
```

| Method | Result |
| --- | --- |
| `enable(player, distance)` | Enables a valid distance. Returns `false` if the amount is invalid or exceeds the player's permissions. |
| `disable(player)` | Disables fastplace and cancels pending placement. |
| `getDistance(player)` | Returns the selected distance, or `0` when disabled. |
| `getMaximumDistance(player)` | Returns the limit allowed by permissions and configuration. |
| `isEnabled(player)` | Returns whether the selected distance is currently permitted. |

Call API methods on the server thread. They do not send command messages. Keep the dependency as `provided`, and place both plugin JARs in the server's `plugins` folder.

For optional integration, use `softdepend: [Fastplace]` and check the registered provider before calling it:

```java
FastPlaceAPI api = getServer().getServicesManager().load(FastPlaceAPI.class);
if (api != null) {
    api.enable(player, 16);
}
```
