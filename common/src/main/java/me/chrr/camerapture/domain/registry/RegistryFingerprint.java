package me.chrr.camerapture.domain.registry;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Deterministic, order-independent cache fingerprint. */
public record RegistryFingerprint(String sha256) {
    public RegistryFingerprint {
        Objects.requireNonNull(sha256, "sha256");
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("fingerprint must be lowercase SHA-256");
        }
    }

    public static RegistryFingerprint compute(Input input) {
        Objects.requireNonNull(input, "input");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                write(out, "minecraft", input.minecraftVersion());
                write(out, "algorithm", Integer.toString(input.algorithmVersion()));
                writeSorted(out, "mods", input.mods());
                writeSorted(out, "packs", input.enabledDataPacks());
                write(out, "config", input.serverConfigDigest());
                write(out, "rules", input.ruleDigest());
                writeSorted(out, "entities", input.entityIds());
                writeSorted(out, "biomes", input.biomeIds());
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new RegistryFingerprint(HexFormat.of().formatHex(digest.digest(bytes.toByteArray())));
        } catch (IOException impossible) {
            throw new IllegalStateException("in-memory fingerprint serialization failed", impossible);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String digestStrings(Collection<String> values) {
        return compute(new Input("digest", 0, List.of(), List.of(), "", "", values, List.of())).sha256();
    }

    private static void writeSorted(DataOutputStream out, String label, Collection<String> values) throws IOException {
        List<String> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        write(out, label + ".size", Integer.toString(sorted.size()));
        for (String value : sorted) {
            write(out, label, value);
        }
    }

    private static void write(DataOutputStream out, String label, String value) throws IOException {
        byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = Objects.requireNonNull(value, label).getBytes(StandardCharsets.UTF_8);
        out.writeInt(labelBytes.length);
        out.write(labelBytes);
        out.writeInt(valueBytes.length);
        out.write(valueBytes);
    }

    public record Input(
            String minecraftVersion,
            int algorithmVersion,
            Collection<String> mods,
            Collection<String> enabledDataPacks,
            String serverConfigDigest,
            String ruleDigest,
            Collection<String> entityIds,
            Collection<String> biomeIds
    ) {
        public Input {
            Objects.requireNonNull(minecraftVersion, "minecraftVersion");
            mods = List.copyOf(mods);
            enabledDataPacks = List.copyOf(enabledDataPacks);
            Objects.requireNonNull(serverConfigDigest, "serverConfigDigest");
            Objects.requireNonNull(ruleDigest, "ruleDigest");
            entityIds = List.copyOf(entityIds);
            biomeIds = List.copyOf(biomeIds);
        }
    }
}
