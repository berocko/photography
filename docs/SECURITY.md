# Security

Threat boundaries include forged target/value packets, submission replay and concurrency, UUID theft, oversized or inconsistent image chunks, decompression bombs, path traversal, unbounded Gallery/cache/statistics growth, optional-mod linkage, dedicated-server client linkage, and long map-art work on the main thread.

Required controls are server scene snapshots, owner checks, idempotency keys, per-photo locks or serialized server transactions, checked `long` arithmetic, bounded queues and caches, pre-allocation byte limits, decoded image limits, canonical UUID paths, rate/time/day quotas, and cleanup on disconnect/timeout.

Asynchronous workers may decode/scale immutable byte arrays, but Minecraft world state, SavedData, player inventory, balance providers, item drops, and map data are mutated only on the server thread. A transaction never grants goods after failed withdrawal or marks a submission after failed deposit.

