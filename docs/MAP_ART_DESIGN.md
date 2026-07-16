# Server-Authoritative Map Art Design

Status: design only; Milestone 7 is not implemented.

## Goals and invariants

Map art converts an owned Gallery photograph into either a 3×3 (384×384 pixels) or 5×5 (640×640 pixels) set of vanilla 128×128 filled maps. The server owns validation, source bytes, palette conversion result, cost, map IDs, inventory delivery, persistence, and recovery. Client pixels, client-declared ownership, prices, palette bytes, and map IDs are never authoritative.

Tiles are indexed row-major: `index = row * columns + column`, from the top-left. A completed order contains exactly 9 or 25 unique maps, each with exactly 16,384 palette bytes.

## Request and validation flow

1. The client sends only the Gallery photo ID, requested layout (`THREE_BY_THREE` or `FIVE_BY_FIVE`), and a one-use UI request nonce.
2. On the main server thread, reject malformed/replayed nonces, unsupported layouts, rate-limit excess, unavailable Gallery records, non-owners, forbidden/team-inaccessible photos, missing source images, and orders over configured concurrent or daily limits.
3. Resolve the price from validated server configuration/data, then reserve funds through the M5 transaction service. Do not accept a price from the packet.
4. Snapshot immutable order inputs: order ID, player/team owner, photo ID, source image digest, layout, target size, algorithm version, palette version, reserved amount, and request time. Persist state `RESERVED` before asynchronous work.
5. Read/decode the server-owned photograph under byte, dimension, decompression, and timeout limits. Normalize orientation and alpha against a configured background.
6. Resize once to 384×384 or 640×640 with the documented filter before slicing. Never resize each tile independently; that creates seams.
7. Submit palette conversion to a bounded worker pool. Workers operate only on immutable pixel arrays and return palette byte arrays plus diagnostics. Workers never touch a world, player, inventory, `MapState`, saved data, or currency provider.
8. Return to the main server thread. Revalidate session/owner/order/source digest and reservation. If invalid, cancel and compensate without allocating maps.
9. Slice row-major into 128×128 tiles, allocate vanilla map IDs/states, set every tile's colors, and create filled-map stacks using the 1.21.1 map ID data component. Record all allocated IDs in the order before delivery.
10. Deliver all stacks atomically to a capacity-checked destination, or use an explicit overflow container/mailbox defined by M4/M5. Never silently drop valuable results into an unloaded world.
11. Commit the currency reservation only after map states and delivery are durably recorded. Persist `COMPLETED` with output IDs and audit context.
12. Return a small completion packet containing the order ID and authoritative result summary; normal vanilla inventory/map synchronization carries the items/data.

## Image and palette pipeline

- Decode only server-owned encoded bytes and enforce existing maximum byte/resolution controls plus a decompressed-pixel ceiling.
- Convert color in a deterministic color space and quantize against the exact Minecraft 1.21.1 map palette. The algorithm/version is part of persisted metadata.
- Optional dithering is a server configuration choice and must be deterministic for a given source digest and algorithm version.
- Resize to the full grid first. Pixel `(x, y)` goes to tile `(x / 128, y / 128)` and local pixel `(x % 128, y % 128)`.
- A palette conversion cache may key on source digest, target size, palette version, algorithm version, background, and dither settings. Cache content is derived/non-authoritative and bounded by bytes and age.

## Minecraft state and threading

- The main server thread allocates map IDs via the server world's vanilla map-state mechanism and writes `MapState`/persistent state. Filled-map stacks reference those IDs through the Minecraft 1.21.1 map ID data component.
- The chosen dimension for stored map state is a fixed documented implementation choice; image maps do not track terrain or players and must not be updated by terrain exploration.
- Worker threads may resize/quantize immutable arrays only. All registry access, saved-data access, packet handling decisions, currency calls, player/inventory mutations, map allocation, and persistence transitions execute on the main server thread.
- Work queues have configured concurrency, queue length, per-job deadline, and cancellation. Server shutdown stops admission, cancels uncommitted jobs, and persists enough state for recovery.

## Transaction, compensation, and recovery

Persist an order state machine: `RESERVED -> CONVERTING -> ALLOCATING -> DELIVERING -> COMPLETED`, with terminal `CANCELLED` and `FAILED_COMPENSATED`. Each transition is idempotent and includes an audit timestamp/reason.

- Before allocation failure: release/refund the reservation and mark compensated.
- During allocation failure: record every allocated map ID, avoid delivery, refund, and mark those IDs orphaned for bounded cleanup. Vanilla numeric IDs may be impossible to reuse safely; rollback means no player-visible output and compensated funds, not pretending the allocation never happened.
- Delivery failure: keep the complete set in a durable server-owned pending-delivery record, then either retry delivery or refund and invalidate the order according to policy. Never deliver a partial grid.
- Commit failure after delivery is treated as an invariant breach: lock the order, retain audit data, and retry/repair through the transaction service rather than charging twice or duplicating maps.
- Restart recovery scans nonterminal orders. It verifies source digest, reservation state, allocated IDs, and delivery record before resuming or compensating. Replaying the same order/request nonce returns the existing result and never allocates a second set.

## Security and limits

Validate packet length/enums, ownership, layout allow-list, source digest, image bounds, reservation, inventory destination, and request nonce. Apply per-player/team rate limits, global conversion concurrency, bounded queues/caches, maximum pending deliveries, and structured audit logs. Do not log raw image bytes or secrets.

## Future test matrix

At minimum M7 must add these automated tests before completion:

1. 3×3 preprocessing produces 384×384 pixels and exactly nine tiles.
2. 5×5 preprocessing produces 640×640 pixels and exactly twenty-five tiles.
3. Tile slicing is row-major and maps boundary pixels to the correct tile/local coordinate.
4. Resizing the full image before slicing produces no independently-resized seam.
5. Every output tile contains exactly 16,384 palette bytes.
6. Palette conversion is deterministic for identical source/config/version inputs.
7. Transparent pixels use the configured authoritative background.
8. A non-owner Gallery request is rejected before reservation/conversion.
9. A missing or digest-mismatched source is rejected and a reservation is compensated.
10. A client-declared price, palette, map ID, or owner field is ignored/rejected.
11. Insufficient currency creates no order, maps, or inventory mutation.
12. Conversion failure refunds/releases the reservation and allocates no maps.
13. Failure after partial map allocation delivers nothing, compensates funds, and records orphan IDs.
14. Insufficient inventory capacity yields durable all-or-nothing pending delivery, never a partial grid.
15. Successful delivery commits exactly once and records all unique map IDs.
16. Duplicate request nonce/order replay returns the original state without a second charge or allocation.
17. Player disconnect during conversion causes safe revalidation and retry/compensation.
18. Server restart recovers each nonterminal state idempotently.
19. Queue/rate/concurrency limits reject excess work without leaking reservations.
20. Worker code cannot access/mutate world, inventory, saved data, or currency boundaries.
21. Dedicated-server integration creates maps whose data components resolve to the persisted states after restart.
22. 3×3 and 5×5 cost resolution uses server configuration and checked arithmetic without overflow.
