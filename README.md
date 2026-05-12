# Fabric Utility

Fabric Utility is a Minecraft 1.20.1 Fabric server utility mod.

## Features

- `/tagchunk` stores persistent tags on the current chunk.
- `/tagchunk subchunk` stores tags on the current 16-block-tall vertical subchunk.
- Chunk tags can include optional values, using comma-separated values for lists.
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
- Items with the NBT tag `BanHammer` ban punched players for seven days.

## Commands

- `/tagchunk add <tag> [value]`
- `/tagchunk remove <tag>`
- `/tagchunk get`
- `/tagchunk check`
- `/tagchunk subchunk add <tag> [value]`
- `/tagchunk subchunk remove <tag>`
- `/tagchunk subchunk get`
- `/tagchunk subchunk check`
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
