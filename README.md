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
- `/nick` lets players save, switch, clear, and list nicknames.
- `/nick admin` lets operators set, clear, discover, and list player nicknames.
- Nicknames are applied to chat and common game messages.
- Config file `config/fabric_utility.properties` can block entity ids from being pettable.
- Mod Menu can open a client-side config editor when Mod Menu is installed.
- `/fabricutility config` can reload, sync, inspect, and edit server config values.
- Petting sounds, command-tag sound overrides, player fallback sound, volume, pitch, particle count, and nickname enablement are configurable.
- Items with the NBT tag `BanHammer` ban punched players for seven days.

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
- `/nick admin list`

## Ban Hammer NBT

Any item with `BanHammer` in its NBT becomes a ban hammer. If the tag is a string, that string is used as the ban reason. Otherwise the default reason is `the ban hammer has spoken`.

## Config Keys

- `blockedPettableEntities`: comma-separated entity ids that cannot be pet.
- `pettingSoundSuffixes`: comma-separated entity sound suffixes to try, such as `ambient,step,hurt,death`.
- `maxPlayerPetParticles`: heart particle cap when petting players.
- `defaultPlayerPetSound`: fallback sound id for players and entities without a matching pet sound.
- `defaultPlayerPetVolume`: fallback sound volume.
- `defaultPlayerPetPitch`: fallback sound pitch.
- `nicknameSystemEnabled`: enables nickname chat/game-message replacement.
- `customPetSounds`: semicolon-separated command-tag sound rules, formatted as `tag=namespace:sound:volume:pitch`.

## Height Limit

`fabricUtilityWorldHeightLimit` is a safe max-build-height guard. Values above the vanilla dimension height cannot expand chunk storage at runtime; use dimension type/worldgen data for worlds that need a taller buildable height.
