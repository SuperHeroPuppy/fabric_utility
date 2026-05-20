# Fabric Utility

Fabric Utility is a Minecraft 1.20.1 Fabric server utility mod.

## Features

- `/tagchunk` stores persistent tags on the current chunk.
- `/tagchunk subchunk` stores tags on the current 16-block-tall vertical subchunk.
- Chunk tags can include optional values, using comma-separated values for lists.
- Existing tags can be modified with `/tagchunk set` and tag arguments autocomplete from the current chunk or subchunk.
- `invulnerability` chunk or subchunk tags protect blocks from non-bypassed players, explosions, TNT, and falling blocks.
- `invulnerability` tag values act as username bypasses. Example: `/tagchunk add invulnerability Steve,Alex`.
- Entity command tag `invulnerability` prevents damage, death, fire, fall damage, and knockback.
- Shift-right-click with an empty hand pets living entities, plays a sound, and spawns heart particles.
- Gamerule `fabricUtilityAllowPetting` enables or disables petting.
- Gamerule `fabricUtilityWorldHeightLimit` sets an optional max build height guard. `0` uses vanilla behavior.
- Gamerule `fabricUtilityAdminNicknameChangesAffectHistory` controls whether operators receive nickname change log messages.
- `/nick` lets players save, switch, clear, and list nicknames, with a configurable character limit.
- `/nick admin` lets operators set, clear, discover, inspect history, and list player nicknames.
- Nicknames are applied to chat, proxy chat, common game messages, join/leave messages, tab list names, and nameplates.
- `/proxy` provides proximity chat with local, world, and area-based messaging.
- Proxy chat supports pinned modes, do-not-disturb, and configurable ranges.
- Named chat areas can be created and managed using chunk tagging.
- Config file `config/fabric_utility.properties` can block entity ids from being pettable.
- Mod Menu can open a client-side config editor when Mod Menu is installed.
- `/fabricutility config` can reload, sync, inspect, and edit server config values.
- Petting sounds, command-tag sound overrides, player fallback sound, volume, pitch, particle count, and nickname enablement are configurable.
- Items with the NBT tag `BanHammer` let operators ban punched players for seven days.
- Non-operators carrying a `supplementaries:cage` containing a command block minecart have that contained entity sanitized into a pig.

## Commands

- `/tagchunk add <tag> [value]`
- `/tagchunk remove <tag>`
- `/tagchunk set <tag> [value]`
- `/tagchunk get`
- `/tagchunk check`
- `/tagchunk subchunk add <tag> [value]`
- `/tagchunk subchunk remove <tag>`
- `/tagchunk subchunk set <tag> [value]`
- `/tagchunk subchunk get`
- `/tagchunk subchunk check`
- `/fabricutility config reload`
- `/fabricutility config sync`
- `/fabricutility config list`
- `/fabricutility config get <key>`
- `/fabricutility config set <key> <value>`
- `/nick add <nickname>`
- `/nick remove <nickname>`
- `/nick set <nickname>`
- `/nick list`
- `/nick clear`
- `/nick admin set <player> <nickname>`
- `/nick admin clear <player>`
- `/nick admin discover <player>`
- `/nick admin history <player>`
- `/nick admin list`
- `/m local <message>`
- `/m world <message>`
- `/proxy dnd on|off`
- `/proxy range <chunks>` (admin only)
- `/proxy area create <name> <radius>`
- `/proxy area delete <name>`
- `/proxy area list`
- `/pin none|world|local|area <name>`

## Ban Hammer NBT

Any item with `BanHammer` in its NBT becomes a ban hammer for operators. If the tag is a string, that string is used as the ban reason. Otherwise the default reason is `the ban hammer has spoken`.

Non-operators cannot activate ban hammer items.

## NBT Safety

For non-operators, `supplementaries:cage` items are scanned for embedded `minecraft:command_block_minecart` entity data under their block entity NBT. When found, the embedded entity id is changed to `minecraft:pig` and command-related fields are removed.

This is a compatibility safety patch for Supplementaries cages that have been edited with external NBT tools.

## Config Keys

- `blockedPettableEntities`: comma-separated entity ids that cannot be pet.
- `pettingSoundSuffixes`: comma-separated entity sound suffixes to try, such as `ambient,step,hurt,death`.
- `maxPlayerPetParticles`: heart particle cap when petting players.
- `defaultPlayerPetSound`: fallback sound id for players and entities without a matching pet sound.
- `defaultPlayerPetVolume`: fallback sound volume.
- `defaultPlayerPetPitch`: fallback sound pitch.
- `nicknameSystemEnabled`: enables nickname chat/game-message replacement.
- `nicknameCharacterLimit`: maximum nickname length. Defaults to `35`.
- `proxyChatEnabled`: enables the proxy chat system.
- `proxyChatRangeChunks`: default proximity chat range in chunks.
- `customPetSounds`: semicolon-separated command-tag sound rules, formatted as `tag=namespace:sound:volume:pitch`.

## Proxy Chat

Proxy chat provides advanced messaging options beyond vanilla Minecraft chat:

- **Local Chat**: Messages sent with `/m local <message>` are only visible to players within the configured range (default 3 chunks).
- **World Chat**: Messages sent with `/m world <message>` are visible to all players on the server.
- **Chat Pinning**: Players can pin their chat to specific modes using `/pin none|world|local|area <name>` to always send messages in that mode.
- **Do Not Disturb**: Players can enable DND with `/proxy dnd on` to block incoming private messages and replies.
- **Range Configuration**: Only administrators can set the local chat range with `/proxy range <chunks>` (1-16 chunks)
- **Chat Areas**: Named areas can be created with `/proxy area create <name> <radius>` and used for area-specific chat when pinned.

Chat areas are stored as chunk tags with the prefix `proxy_area:` and can be managed alongside other chunk tags. The system integrates with the nickname system, so nicknames are displayed in proxy chat messages when enabled.

## Height Limit

`fabricUtilityWorldHeightLimit` is a safe max-build-height guard. Values above the vanilla dimension height cannot expand chunk storage at runtime; use dimension type/worldgen data for worlds that need a taller buildable height.

## Nickname Logs

`fabricUtilityAdminNicknameChangesAffectHistory` defaults to `true`. When enabled, operators receive a `[Nick]` message when players add, switch, remove, clear, or are assigned nicknames. Set it to `false` to disable those admin log messages.

## Nickname Limit

`nicknameCharacterLimit` defaults to `35`. `/nick add <nickname>`, `/nick set <nickname>`, and `/nick admin set <player> <nickname>` reject nicknames longer than the configured limit.
