# Map Art Audit

## Result

Map art is not implemented in this repository. Milestone 7 remains **未开始**. No UI control, packet, conversion service, map allocation, cost transaction, persistence record, or rollback path currently creates map art from a photograph.

## Current Album and picture behavior

- `AlbumItem` stores up to 36 physical Picture item stacks and exposes them as three pages of twelve slots.
- `AlbumScreen` and its screen handler support physical album inventory editing and page navigation. They are not the planned persistent Virtual Gallery.
- Right-clicking a non-empty Album opens `PictureScreen` over the Album's current Picture stacks.
- `PictureScreen` is a client viewer for existing photographs and can save the displayed image as a local PNG. Local saving is not a server gameplay transaction.
- Pictures are transferred and stored through Camerapture's existing picture ID/image store flow. The current flow does not persist a map-art order or map allocation alongside a photograph.

## Search evidence

The production source contains no map-art request packet, `MapState` or filled-map allocation path, `MapColor` palette converter, 128×128 tile slicer, 3×3/5×5 order, map-frame grid placement, map-art currency charge, or compensating rollback. References to map art exist only in product/roadmap/planning documentation.

## Dependencies and boundary

Map art is planned for M7 and must not be built as a client-only image export. It depends on:

- M4 Virtual Gallery for authoritative photo ownership and selection;
- M5 currency and shared transaction services for atomic/compensating charges;
- server-persisted photo metadata and image availability;
- bounded server conversion and allocation policies.

No placeholder button or fake result preview is added by the camera UX work. The future implementation contract is defined in `docs/MAP_ART_DESIGN.md`.
