# Network Protocol

The baseline registers seven typed payloads: new picture, upload request, upload chunks, download request, download chunks, picture error, and synced config. NeoForge currently declares protocol string `1`; Fabric relies on registered payload codecs.

New protocol families will carry an explicit protocol/schema version and request ID. Server handlers validate logical side, player ownership, state transition, UUID, chunk index/order, total declared bytes, actual bytes, dimensions, rate limits, timeout, and replay status before mutation.

Upload sessions are player-bound and bounded globally and per player. Disconnect, timeout, malformed chunks, or persistence failure releases buffers and reservation state. Gallery traffic is paged. No packet may directly set balances, scores, submission state, or discovery counters.

