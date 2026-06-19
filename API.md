# Fabric Utility Public API

The API is available from the `net.supersnetwork.fabric_utility.api` package.

## Nicknames

Use `NicknameApi` instead of reading Fabric Utility's saved data directly.

```java
Text displayName = NicknameApi.getDisplayName(player);
String plainName = NicknameApi.getEffectiveName(player);

NicknameApi.setNickname(player, "The Puppy");
NicknameApi.clearNickname(player);
```
*
Listen for nickname changes:

```java
NicknameApi.registerChangeListener((player, previous, current) -> {
    // Refresh your mod's cache or UI.
});
```

Nickname writes made through the API immediately refresh the player's
nameplate and tab-list display. Commands and API writes preserve MiniMessage
source. History autocomplete presents plain text and resolves it back to the
formatted saved value. `getPlainNickname` returns visible text and
`getDisplayName` returns native Minecraft text.

## Chunk and subchunk tags

Use `ChunkTagApi` to query or mutate persistent tags:

```java
boolean isSpecial = ChunkTagApi.hasTag(world, blockPos, "example:special");

ChunkTagApi.add(
        world,
        blockPos.getX() >> 4,
        blockPos.getZ() >> 4,
        Optional.empty(),
        "example:special",
        List.of("value")
);
```

Register behavior for a custom tag:

```java
ChunkTagApi.register("example:special", new ChunkTagHandler() {
    @Override
    public void onAdded(ChunkTagContext context, List<String> values) {
        ServerWorld world = context.world();
        // React to the tag being added.
    }

    @Override
    public void onUpdated(
            ChunkTagContext context,
            List<String> previousValues,
            List<String> newValues
    ) {
        // React to changed values.
    }

    @Override
    public void onRemoved(ChunkTagContext context, List<String> previousValues) {
        // Clean up behavior associated with the tag.
    }
});
```

Tag callbacks run on the server thread when the tag is changed.
