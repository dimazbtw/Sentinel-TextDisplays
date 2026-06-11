# Sentinel-TextDisplays

Per-player **Text Display** entities for Minecraft **1.19.4 – 1.21.x** — full PlaceholderAPI,
ItemsAdder & Nexo support, HEX colors, real text scaling, multi-page holograms and click
interactions, using a **DecentHolograms-style** configuration and command set.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-7c3aed.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.19.4–1.21.x-7c3aed)
![Java](https://img.shields.io/badge/Java-17–21-7c3aed)
![Requires](https://img.shields.io/badge/requires-PacketEvents-c026d3)

These are genuine `minecraft:text_display` entities (the ones added in 1.19.4) — **not**
armor-stand holograms — so text size, billboard, background, opacity and alignment behave exactly
like vanilla. They are rendered **per-player via packets**, so every player sees their *own*
placeholders, emojis and page. No entity ever exists server-side.

> 📖 **Full documentation** — setup, every command, the file format, colors and the editor —
> lives on the **[Wiki](https://github.com/dimazbtw/Sentinel-TextDisplays/wiki)**.

---

## Features

- ✅ **Real Text Displays** with configurable **size/scale** (per axis), billboard mode,
  background color, text opacity, line width, shadow, see-through, alignment and brightness.
- ✅ **Per-player rendering** — `%player_name%`, balances, ping, etc. are personalized per viewer.
- ✅ **PlaceholderAPI**, **ItemsAdder** and **Nexo** glyph/emoji support.
- ✅ **HEX colors** (`&#RRGGBB`, `<#RRGGBB>`), **gradients**, and legacy `&` codes.
- ✅ **DecentHolograms-style** hologram files and commands (pages, lines, actions, `display-range`,
  `update-range`, `update-interval`, `down-origin`, `facing`).
- ✅ **Click interactions** with LEFT / RIGHT / SHIFT_LEFT / SHIFT_RIGHT and DH action types.
- ✅ **Multi-page** holograms with `NEXT_PAGE` / `PREV_PAGE` / `PAGE` navigation.
- ✅ **Per-display view permission** and ranges; in-game GUI editor; full tab-completion.
- ✅ **Lightweight** — depends on the free **PacketEvents** plugin (not bundled). Java **17–21**.

## Requirements

- Spigot / Paper (or forks) on **Minecraft 1.19.4 or newer**.
- **Java 17–21**.
- **[PacketEvents](https://modrinth.com/plugin/packetevents)** (free) — required.
- Optional: PlaceholderAPI, ItemsAdder, Nexo.

## Installation

1. Install **PacketEvents** into `plugins/` (hard dependency).
2. Drop `Sentinel-TextDisplays-x.y.z.jar` into `plugins/`.
3. Start the server. A `plugins/Sentinel-TextDisplays/` folder with `config.yml` and a
   `displays/` folder (with a commented `example.yml`) is created.

## Quick start

```text
/td create welcome &bWelcome, &f%player_name%
/td line add welcome &7Enjoy your stay
/td scale welcome 1.5
/td page addaction welcome 1 RIGHT MESSAGE:&aHello %player_name%!
```

---

## Commands

Base command `/td` (aliases `sd`, `displays`, `textdisplay`). Permission: `displays.admin`.

**General:** `help`, `reload`, `version`, `list`, `gui`, `edit <name>`, `nearby <range>`

> **Tip:** `/td gui` opens a full in-game editor — everything below (create, delete,
> enable/disable, lines, pages, click actions, appearance, settings) is also editable there.

**Display:**
`create <name> [content]`, `delete <name>`, `clone <name> <newName>`, `enable/disable <name>`,
`info <name>`, `movehere <name>`, `move <name> <x> <y> <z> [world]`, `teleport <name>`,
`center <name>`, `rename <name> <newName>`, `facing <name> <degrees>`

**Lines:**
`line add <name> <content>`, `line set <name> <line> <content>`, `line remove <name> <line>`,
`line insertbefore <name> <line> <content>`, `line insertafter <name> <line> <content>`

**Pages:**
`page add <name>`, `page insert <name> <page>`, `page remove <name> <page>`,
`page swap <name> <page1> <page2>`,
`page addaction <name> <page> <clickType> <TYPE:data>`,
`page removeaction <name> <page> <clickType> <index>`,
`page clearactions <name> <page> [clickType]`

**Appearance (text-display options):**
`scale <name> <x> [y] [z]`, `billboard <name> <FIXED|VERTICAL|HORIZONTAL|CENTER>`,
`background <name> <default|transparent|#RRGGBB|#AARRGGBB>`, `opacity <name> <0-255>`,
`linewidth <name> <n>`, `shadow <name> <true|false>`, `seethrough <name> <true|false>`,
`alignment <name> <LEFT|CENTER|RIGHT>`, `brightness <name> <block> <sky>`,
`displayrange <name> <blocks>`, `updaterange <name> <blocks>`, `updateinterval <name> <ticks>`,
`downorigin <name> <true|false>`, `viewpermission <name> <node|none>`,
`clickbox <name> <width> <height>`

Page numbers and line numbers are **1-based**, like DecentHolograms.

## Hologram file format (DecentHolograms style)

One file per display in `plugins/Sentinel-TextDisplays/displays/<name>.yml`:

```yaml
location: "world:0.500:101.000:0.500"
facing: 0.0
enabled: true
display-range: 48.0
update-range: 48.0
update-interval: 20
down-origin: false
pages:
- lines:
  - content: '<gradient:#00c6ff:#0072ff>Welcome</gradient>'
  - content: '&7Hello, &f%player_name%'
  actions:
    RIGHT:
    - 'MESSAGE:&aYou clicked the display!'
    - 'SOUND:UI_BUTTON_CLICK'
# --- text-display options (extensions) ---
scale: [1.5, 1.5, 1.5]
billboard: CENTER
background: transparent
text-opacity: 255
line-width: 200
text-shadow: true
see-through: false
text-alignment: CENTER
brightness-block: -1
brightness-sky: -1
view-permission: ''
click-width: 3.0
click-height: 1.5
```

## Colors

- Legacy codes: `&a`, `&l`, `&r`, …
- HEX: `&#ff8800` or `<#ff8800>`
- Gradient: `<gradient:#ff0000:#00ff00>text</gradient>`

A color code does **not** clear formatting — only `&r` resets — so `&l&cText` is bold red.

## Click actions

Click types: `LEFT`, `RIGHT`, `SHIFT_LEFT`, `SHIFT_RIGHT`. Each action is `TYPE:data`:

| Action | Effect |
|---|---|
| `MESSAGE:<text>` | Send a chat message |
| `COMMAND:<cmd>` | Run a command as the player |
| `CONSOLE:<cmd>` | Run a command from console |
| `CONNECT:<server>` | Send to a BungeeCord/Velocity server |
| `TELEPORT:[world:]<x>:<y>:<z>[:<yaw>:<pitch>]` | Teleport the player |
| `SOUND:<sound>[:<vol>:<pitch>]` | Play a sound |
| `PERMISSION:<node>` | Stop the remaining actions if the player lacks the node |
| `NEXT_PAGE[:<display>]` / `PREV_PAGE[:<display>]` | Switch page |
| `PAGE:[<display>:]<page>` | Go to a specific page |

Placeholders work inside action data too.

## Permissions

| Node | Default | Description |
|---|---|---|
| `displays.admin` | op | Create and manage displays |

Per-display visibility can also be gated with `/td viewpermission <name> <node>`.

## Developer API

The plugin doubles as the API jar (the `api` package and model types are kept stable). Add it as a
compile-only dependency and declare `depend: [Sentinel-TextDisplays]` in your `plugin.yml`.

```java
import github.dimazbtw.displays.api.TextDisplaysAPI;
import github.dimazbtw.displays.display.ClickType;
import github.dimazbtw.displays.display.Display;

// Persistent display (saved to displays/<name>.yml)
Display d = TextDisplaysAPI.createDisplay("shop", location, "&b&lShop", "&7Hello, &f%player_name%");

// Temporary display — memory-only, gone on restart
Display tmp = TextDisplaysAPI.createTemporaryDisplay("quest-marker", location, "&eClick me!");

TextDisplaysAPI.addClickAction(d, 0, ClickType.RIGHT, "MESSAGE:&aThanks!");
TextDisplaysAPI.setPage(player, d, 1);   // per-player pages
TextDisplaysAPI.update(d);               // save + re-render for viewers
```

Cancellable Bukkit events: `DisplayClickEvent`, `DisplayPageChangeEvent`, `DisplayCreateEvent`,
`DisplayDeleteEvent` (all under `github.dimazbtw.displays.api.events`).

```java
@EventHandler
void onClick(DisplayClickEvent e) {
    if (e.getDisplay().getId().equals("shop")) {
        e.setCancelled(true);              // skip the configured TYPE:data actions
        myShop.open(e.getPlayer());        // handle the click in code
    }
}
```

## Building from source

Requires JDK 17+ (builds Java 17 bytecode). Uses the bundled Maven Wrapper:

```bash
./mvnw clean package      # Linux/macOS
mvnw.cmd clean package     # Windows
```

The jar is written to `target/Sentinel-TextDisplays-<version>.jar`.

## Contributing

Issues and pull requests are welcome. Please keep changes focused and match the existing code style.

## License

Sentinel-TextDisplays is free and open source under the **[GNU General Public License v3.0](LICENSE)**.
You may use, study, modify and redistribute it — derivative works must stay open source under the
same license.

## Support

Questions or issues? Open an [issue](https://github.com/dimazbtw/Sentinel-TextDisplays/issues), or
reach me on Discord — **@notdimaz**.
