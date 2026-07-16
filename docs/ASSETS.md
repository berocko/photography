# Assets

Shared 1.21.1 assets and data live in `common/src/main/resources` under the existing `camerapture` namespace. Loader metadata lives in each loader module. Resource-pack format for 1.21/1.21.1 is 34.

Use lowercase namespaced paths, valid JSON, PNG textures, Vorbis OGG sounds, and matching language keys. Keep hand-authored resources distinct from future DataGen output. Gallery GUI work must include explicit sprite dimensions and states; binary art is not fabricated as a placeholder.

Validate JSON and all model/texture/sound references with the project Skill validator after asset changes. The current upstream pack is audited in Milestone 0 without claiming newly generated art.

