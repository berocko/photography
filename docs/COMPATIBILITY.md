# Compatibility

NeoForge 1.21.1 is primary; Fabric 1.21.1 must compile and ultimately match gameplay. Shared contracts are implemented behind loader adapters. Optional FTB, Magic Coins, teams, NPC, or other currency integrations are discovered by mod ID and reflection/isolated compatibility entrypoint only after presence checks; absent mods must not be referenced during class loading.

The existing Mod ID, resource namespace, package, data components, world folder, and photo UUIDs are compatibility-sensitive. Renaming from Camerapture requires an explicit migration and is not part of the current baseline.

