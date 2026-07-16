# Testing

Pure domain algorithms use JUnit 5 in `common/src/test/java`. World, inventory, persistence, packet, and loader behavior use focused GameTests or integration smoke tests. Both loader artifacts build after shared changes.

Milestone 1 has 19 passing JUnit tests covering automatic valuation/log compression/clamps, exact/tag/namespace precedence, runtime/automatic/global fallback, saturated override arithmetic, entity UUID cap, missing UUID handling, entity and biome type caps, inverse decay, reward overflow clamps, checked currency arithmetic, gameplay config schema/defaults and malformed-value isolation, valuation-rule parsing/errors, and photo/scene/score Codec serialization.

Validated commands use JDK 21:

```text
./gradlew tasks --all
./gradlew clean buildAll :common:test
./gradlew :neoforge:runServer
./gradlew :neoforge:runClient
```

The server smoke reached `Done`; the client completed mod/resource loading and atlas creation. Both long-running processes were then intentionally interrupted. The testing-skill layout validator passes non-strict with one expected warning because GameTest fixtures have not yet been introduced; strict mode reports that warning as failure.

Later integration coverage follows the relevant feature: film consume/refund, duplicate submission, eleventh type reward, biome chunk dedupe, atomic shop purchase, Gallery capacity, map-art output/overflow, and restart persistence.
