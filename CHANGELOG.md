# 0.3.1-alpha

- Replaced expanding/contracting circular Pokemon glows with fixed-size animated nebula/shimmer glows.
- Greatly reduced glow footprint while keeping Rare/Ultra Rare/Legendary/Mythical/Shiny conspicuous.
- Pokemon name, level, rarity, distance and Y are now shown only on mouse hover in the full map.
- Removed always-on altitude arrows from Pokemon markers.
- Replaced the easy-to-lose white player marker with a high-contrast cyan directional marker with dark outline.
- Fixed right-click -> Teleportar for fresh map positions (`existing == null`) and removed the unreliable client-side permission gate; server permissions remain authoritative.

# Changelog

## 0.3.0-alpha

- Added native Spawn Areas overlay with Pokémon search and single-selection workflow.
- Added static spawn geography evaluation for biome, dimension and Y conditions.
- Upgraded atlas tile format to persist biome IDs (backward-compatible v1 reader).
- Rebuilt Pokémon glow as animated radial bloom textures.
- Added distinct Rare, Ultra Rare, Legendary, Mythical and independent Shiny visual identities.
- Added stronger multi-pass bloom for the rarest tiers.
- Added UUID phase offsets, marker spring entry/exit and position smoothing.
- Improved map pan/zoom and UI hover motion.
- Retained the full-map vanilla blur suppression from 0.2.6-alpha.
