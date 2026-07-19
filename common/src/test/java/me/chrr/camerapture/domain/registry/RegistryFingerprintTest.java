package me.chrr.camerapture.domain.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RegistryFingerprintTest {
    @Test
    void inputOrderDoesNotChangeFingerprint() {
        RegistryFingerprint first = fingerprint(List.of("b@2", "a@1"), "rules", List.of("minecraft:zombie", "minecraft:pig"));
        RegistryFingerprint second = fingerprint(List.of("a@1", "b@2"), "rules", List.of("minecraft:pig", "minecraft:zombie"));
        assertEquals(first, second);
    }

    @Test
    void modVersionChangesFingerprint() {
        assertNotEquals(
                fingerprint(List.of("example@1"), "rules", List.of("minecraft:pig")),
                fingerprint(List.of("example@2"), "rules", List.of("minecraft:pig"))
        );
    }

    @Test
    void ruleDigestChangesFingerprint() {
        assertNotEquals(
                fingerprint(List.of("example@1"), "rules-a", List.of("minecraft:pig")),
                fingerprint(List.of("example@1"), "rules-b", List.of("minecraft:pig"))
        );
    }

    private static RegistryFingerprint fingerprint(List<String> mods, String rules, List<String> entities) {
        return RegistryFingerprint.compute(new RegistryFingerprint.Input(
                "1.21.1", 1, mods, List.of("vanilla"), "config", rules, entities, List.of("minecraft:plains")
        ));
    }
}
