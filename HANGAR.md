# LastWords

**Custom death messages with colours, gradients and per-cause message pools. Paper 1.21+ and 26.x.**

---

## What it does

Replaces Minecraft's flat death messages with your own. Every death cause gets its own pool of messages and
one is picked at random, so the same fall does not read the same way twice.

```
☠ Steve forgot about fall damage
☠ Steve tested gravity. Gravity won.
☠ Alex was slain by Steve using Netherite Sword
```

## Features

- **Per-cause message pools**: PLAYER, FALL, LAVA, FIRE, DROWNING, VOID, EXPLOSION, PROJECTILE, STARVATION,
  FREEZE, MAGIC, WITHER, POISON, SUFFOCATION, CONTACT, DEFAULT
- **Random pick** from each pool — write as many variants as you like
- **MiniMessage colours and gradients**, legacy `&` codes also accepted
- **Placeholders**: `{player}` `{killer}` `{weapon}` `{x}` `{y}` `{z}` `{world}`
- **Private death-coordinates message** to the player who died, so they can find their stuff
- **Drop-in**: edit one YAML file, `/lastwords` to reload
- No dependencies, one jar

## Commands

| Command | What it does |
|---|---|
| `/lastwords` | Reload the config (permission `lastwords.reload`, default op) |

## Compatibility

Built for the Paper API 1.21 and up. Every release is started on a **live Paper 1.21.11 server and a live
Paper 26.2 server** and the actual behaviour is checked — not just "the plugin loads".

## Source & licence

MIT licensed, source on [GitHub](https://github.com/arvidhqx-commits/lastwords).

## Development note

This project is **AI-assisted**: the code is written with Claude under the direction, testing and release
approval of the maintainer. Every release is run against a live Paper server before it ships.
