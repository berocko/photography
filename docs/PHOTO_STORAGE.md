# Photo Storage

## Baseline

Original WebP bytes are stored at `<world>/camerapture/<uuid>.webp`; picture items carry UUID, creator name, and capture timestamp. A 250-entry in-memory LRU caches loaded bytes. Albums currently duplicate picture item references through a container component.

## Target

Binary original and thumbnail files remain world-owned, while versioned metadata stores owner UUID, scene snapshot, score/submission state, display name, favourite state, and storage checksums. Virtual Gallery pages transfer metadata and thumbnails first; originals are requested individually. Physical albums will retain ordered photo UUID references only.

All paths are derived from canonical UUIDs, never client filenames. Writes use bounded decoded dimensions and compressed bytes, temporary files plus atomic move where supported, checksum/format validation, and compensating film rollback on failure. Schema changes require migrations that are restart-safe and idempotent.

