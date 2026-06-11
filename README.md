# Sentinel-TextDisplays — Developer API

Per-player Text Display system for Spigot / Paper 1.19.4 – 1.21.x. **This page documents the
developer API.** For installation, commands, configuration, the file format and the in-game editor,
see the **[Wiki](../../wiki)**.

Displays are genuine `text_display` entities (1.19.4+) rendered **per player via packets** — every
viewer gets their own resolved placeholders, their own page, their own copy. No entity ever exists
server-side.

## Add Sentinel-TextDisplays as a dependency

The plugin jar doubles as the API jar — the `api` package and the model types (`Display`, `Page`,
`ClickType`, …) are kept stable so you can compile against them. Add it as a **compile-only**
dependency and declare the plugin as a `depend` (or `softdepend`) so it loads first.

**plugin.yml**
```yaml
depend: [ Sentinel-TextDisplays ]   # or softdepend: if your plugin only optionally uses it
```

**Gradle**
```groovy
dependencies {
    compileOnly files("libs/Sentinel-TextDisplays.jar")
}
```

**Maven**
```xml
<dependency>
    <groupId>github.dimazbtw</groupId>
    <artifactId>sentinel-textdisplays</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/Sentinel-TextDisplays.jar</systemPath>
</dependency>
```

## Access the API

The API is a **static facade** — no instance to grab:

```java
import github.dimazbtw.displays.api.TextDisplaysAPI;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.ClickType;
```

All calls must run on the **main server thread**, after the plugin has enabled.

## Create displays

```java
// Persistent — saved to plugins/Sentinel-TextDisplays/displays/<name>.yml
Display d = TextDisplaysAPI.createDisplay("shop", location,
        "&b&lShop", "&7Hello, &f%player_name%");

// Temporary — memory-only, never written to disk.
// Survives /td reload; disappears on restart. Ideal for displays your plugin manages itself.
Display tmp = TextDisplaysAPI.createTemporaryDisplay("quest-marker", location, "&eClick me!");
```

Lines accept legacy `&` codes, `&#RRGGBB` hex, `<gradient:#a:#b>…</gradient>` gradients,
PlaceholderAPI placeholders (resolved **per viewer**) and ItemsAdder / Nexo glyphs.

## Edit displays

Simple edits have one-call helpers; for anything else, mutate the `Display` directly and push a
single `update`:

```java
TextDisplaysAPI.addLine(d, "&7New line");
TextDisplaysAPI.setLine(d, 0, "&b&lMega Shop");      // zero-based
TextDisplaysAPI.addClickAction(d, 0, ClickType.RIGHT, "MESSAGE:&aThanks!");
TextDisplaysAPI.moveDisplay(d, newLocation);
TextDisplaysAPI.setEnabled(d, false);

// Advanced — batch edits, then update once:
d.setScale(2.0f, 2.0f, 2.0f);                        // true text scaling, per axis
d.setBillboard(BillboardMode.CENTER);
d.setBackgroundSpec("#59000000");                    // ARGB background
d.setTextOpacity(255);
d.setViewPermission("vip.displays");
d.page(0).getLines().add("&7Another line");
TextDisplaysAPI.update(d);                           // save (if persistent) + re-render for viewers
```

## Manage displays

```java
Display d = TextDisplaysAPI.getDisplay("shop");      // by name (null if none), case-insensitive
Collection<Display> all = TextDisplaysAPI.getDisplays();
List<String> names = TextDisplaysAPI.getDisplayNames();
boolean exists = TextDisplaysAPI.exists("shop");

TextDisplaysAPI.deleteDisplay("shop");               // removes the display (and its file, if persistent)
```

## Click actions

Each page holds action chains per click type (`LEFT`, `RIGHT`, `SHIFT_LEFT`, `SHIFT_RIGHT`),
in the `TYPE:data` format:

```java
TextDisplaysAPI.addClickAction(d, 0, ClickType.RIGHT, "SOUND:UI_BUTTON_CLICK");
TextDisplaysAPI.addClickAction(d, 0, ClickType.RIGHT, "NEXT_PAGE");
TextDisplaysAPI.addClickAction(d, 0, ClickType.SHIFT_RIGHT, "PERMISSION:vip.club");
TextDisplaysAPI.addClickAction(d, 0, ClickType.SHIFT_RIGHT, "CONNECT:lobby");
```

Action types: `MESSAGE`, `COMMAND`, `CONSOLE`, `SOUND`, `TELEPORT`, `CONNECT`, `NEXT_PAGE`,
`PREV_PAGE`, `PAGE`, plus the gate type `PERMISSION` (stops the rest of the chain — see the wiki's
**[Pages and Actions](../../wiki/Pages-and-Actions)** page). Adding the first action automatically
gives the display a click hitbox.

## Per-player pages

Every player browses a display's pages independently:

```java
int page = TextDisplaysAPI.getPage(player, d);       // zero-based
TextDisplaysAPI.setPage(player, d, 1);
TextDisplaysAPI.nextPage(player, d);
TextDisplaysAPI.previousPage(player, d);
```

## Events

All under `github.dimazbtw.displays.api.events` — standard Bukkit events.

| Event | Cancellable | Fired when |
|---|---|---|
| `DisplayClickEvent` | ✅ skips the configured actions | a player clicks a display (main thread, before its `TYPE:data` actions run) |
| `DisplayPageChangeEvent` | ✅ keeps the old page | a player's page of a display is about to change |
| `DisplayCreateEvent` | — | a display is created (command, GUI or API) |
| `DisplayDeleteEvent` | — | a display is deleted |

```java
@EventHandler
public void onClick(DisplayClickEvent e) {
    if (e.getDisplay().getId().equals("shop")) {
        e.setCancelled(true);                        // skip the configured TYPE:data actions
        myShop.open(e.getPlayer());                  // handle the click fully in code
    }
}

@EventHandler
public void onPageChange(DisplayPageChangeEvent e) {
    // e.getOldPage(), e.getNewPage() — zero-based
}
```

`DisplayClickEvent` fires for **any** display with a click hitbox (`/td clickbox` or
`Display#setClickBox`) — configured actions are optional, so your plugin can drive clicks entirely
in code.

---

*Requires the Sentinel-TextDisplays plugin + [PacketEvents](https://modrinth.com/plugin/packetevents)
on the server (Minecraft 1.19.4+, Java 17–21). Build against the `api` package and the model types
shown above.*
