# BetterMapX 0.3.1-alpha validation

## Static fixes

- Pokemon full-map labels are removed from the normal render path. Name, level, rarity, shiny, source, distance and Y remain in the hover tooltip only.
- Always-on Pokemon altitude arrows were removed.
- Player position marker is now a cyan high-contrast diamond with a dark outline and directional needle.
- Glow footprint is fixed-size. No animation changes the aura radius/scale; animation is internal shimmer/lobes/sparks.
- Glow maximum footprint is 24 px (Rare), 27 px (Ultra Rare), 30 px (Legendary/Mythical), and up to 32 px for Shiny overlay.
- Right-click fresh coordinates can use Teleportar. Teleport command is no longer blocked by `existing == null` or by a client-side permission-level guess. Server command permissions remain authoritative.

## Build status

`./gradlew clean compileJava --offline` could not run because the Gradle wrapper distribution is not present in this environment and network access to `services.gradle.org` is blocked (`UnknownHostException`).

Therefore:

- Source changes: STATICALLY CONFIRMED
- Gradle build: NÃO CONFIRMADO
- Runtime: NÃO CONFIRMADO
