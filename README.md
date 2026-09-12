# Byte Client

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Minecraft versions

Byte Client currently ships a verified Fabric build for Minecraft 1.21.11.
The Gradle configuration selects version-specific source sets so older 1.21.x
profiles can be added without unsafe runtime checks. Minecraft 1.21.8-1.21.10
are not currently claimed: they changed client input, rendering, and
identifier APIs and require additional dedicated adapters. Do not reuse a JAR
built for a different Minecraft version.

The rendering modules use vanilla lightmap and post-effect APIs. Sodium and
VulkanMod can coexist when their builds target the same Minecraft version, but
their renderer internals are not treated as interchangeable dependencies.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
