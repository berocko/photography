# Testing

Pure domain algorithms use JUnit 5 in `common/src/test/java`. World, inventory, persistence, packet, and loader behavior use focused GameTests or integration smoke tests. Both loader artifacts build after shared changes.

Milestone 1 has 19 passing JUnit tests covering automatic valuation/log compression/clamps, exact/tag/namespace precedence, runtime/automatic/global fallback, saturated override arithmetic, entity UUID cap, missing UUID handling, entity and biome type caps, inverse decay, reward overflow clamps, checked currency arithmetic, gameplay config schema/defaults and malformed-value isolation, valuation-rule parsing/errors, and photo/scene/score Codec serialization.

Validated commands use JDK 21:

```text
./gradlew tasks --all
./gradlew clean buildAll :common:test -PdevMods.enabled=false
./gradlew :neoforge:runServer
./gradlew :neoforge:runClient
```

The server smoke reached `Done`; the client completed mod/resource loading and atlas creation. Both long-running processes were then intentionally interrupted. The testing-skill layout validator passes non-strict with one expected warning because GameTest fixtures have not yet been introduced; strict mode reports that warning as failure.

Later integration coverage follows the relevant feature: film consume/refund, duplicate submission, eleventh type reward, biome chunk dedupe, atomic shop purchase, Gallery capacity, map-art output/overflow, and restart persistence.

Camera UX adds pure curve tests for exact no-zoom/full-zoom endpoints, monotonicity, minimum/exponent correction, and middle-zoom behavior. Client smoke tests confirm development mods load without asserting mouse feel or GUI interaction; scroll capture with open screens, equal X/Y scaling, tab order, JEI, and Mouse Tweaks remain explicit manual checks unless a person performs them.

Map-art implementation is absent. Its required future server/transaction/restart test matrix is listed in `docs/MAP_ART_DESIGN.md` and must not be reported as passing before M7 exists.
