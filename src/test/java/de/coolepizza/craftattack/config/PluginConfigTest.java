package de.coolepizza.craftattack.config;

import de.coolepizza.craftattack.ActivationMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigTest {

    @Test
    @DisplayName("Defaults should initialize with expected standard values")
    void testDefaultConfig() {
        PluginConfig config = PluginConfig.defaults();

        assertEquals(ActivationMode.SWAP, config.getActivationMode());
        assertEquals(50, config.getSpawnRadius());
        assertEquals(5.0, config.getMultiplier(), 0.001);
        assertEquals(5, config.getMultiplyValue());
        assertTrue(config.isBoostEnabled());
        assertEquals("world", config.getWorldName());
        assertNull(config.getLegacyMessage());
    }

    @Test
    @DisplayName("Load modern config with namespaced keys")
    void testLoadModernConfig() {
        String yaml = """
                activation:
                  mode: SNEAK
                elytra:
                  spawn-radius: 75
                  multiplier: 8.5
                  boost-enabled: false
                  world: "custom_world"
                """;

        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(ActivationMode.SNEAK, config.getActivationMode());
        assertEquals(75, config.getSpawnRadius());
        assertEquals(8.5, config.getMultiplier(), 0.001);
        assertEquals(8, config.getMultiplyValue());
        assertFalse(config.isBoostEnabled());
        assertEquals("custom_world", config.getWorldName());
        assertNull(config.getLegacyMessage());
    }

    @Test
    @DisplayName("Load legacy flat keys for backwards compatibility")
    void testLoadLegacyConfig() {
        String yaml = """
                spawnRadius: 35
                multiplyValue: 4
                boostEnabled: false
                world: "legacy_world"
                message: "Drücke %key% zum Boosten"
                activation:
                  mode: SWAP
                """;

        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(ActivationMode.SWAP, config.getActivationMode());
        assertEquals(35, config.getSpawnRadius());
        assertEquals(4.0, config.getMultiplier(), 0.001);
        assertEquals(4, config.getMultiplyValue());
        assertFalse(config.isBoostEnabled());
        assertEquals("legacy_world", config.getWorldName());
        assertEquals("Drücke %key% zum Boosten", config.getLegacyMessage());
    }

    @Test
    @DisplayName("Modern keys should take precedence over legacy keys if both are present")
    void testModernPrecedenceOverLegacy() {
        String yaml = """
                spawnRadius: 20
                multiplyValue: 3
                boostEnabled: false
                world: "old_world"
                elytra:
                  spawn-radius: 120
                  multiplier: 9.0
                  boost-enabled: true
                  world: "new_world"
                """;

        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(120, config.getSpawnRadius());
        assertEquals(9.0, config.getMultiplier(), 0.001);
        assertTrue(config.isBoostEnabled());
        assertEquals("new_world", config.getWorldName());
    }

    @Test
    @DisplayName("Negative spawn-radius and multiplier should trigger warnings and fallback to defaults")
    void testNegativeValuesFallback() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("PluginConfigTestNegative");
        logger.addHandler(logHandler);

        String yaml = """
                elytra:
                  spawn-radius: -15
                  multiplier: -3.5
                """;

        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section, logger);

        assertEquals(PluginConfig.DEFAULT_SPAWN_RADIUS, config.getSpawnRadius());
        assertEquals(PluginConfig.DEFAULT_MULTIPLIER, config.getMultiplier(), 0.001);

        boolean radiusWarned = logHandler.getRecords().stream()
                .anyMatch(r -> r.getMessage().contains("Spawn radius cannot be negative"));
        boolean multiplierWarned = logHandler.getRecords().stream()
                .anyMatch(r -> r.getMessage().contains("Multiplier cannot be negative"));

        assertTrue(radiusWarned, "Expected warning for negative spawn-radius");
        assertTrue(multiplierWarned, "Expected warning for negative multiplier");
    }

    @Test
    @DisplayName("Malformed numeric strings should trigger warnings and fallback gracefully without exception")
    void testMalformedNumericStrings() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("PluginConfigTestMalformed");
        logger.addHandler(logHandler);

        String yaml = """
                activation:
                  mode: SWAP
                elytra:
                  spawn-radius: "not_a_number"
                  multiplier: "invalid_double"
                """;

        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section, logger);

        assertEquals(PluginConfig.DEFAULT_SPAWN_RADIUS, config.getSpawnRadius());
        assertEquals(PluginConfig.DEFAULT_MULTIPLIER, config.getMultiplier(), 0.001);

        boolean radiusWarned = logHandler.getRecords().stream()
                .anyMatch(r -> r.getMessage().contains("Invalid number format for spawn radius"));
        boolean multiplierWarned = logHandler.getRecords().stream()
                .anyMatch(r -> r.getMessage().contains("Invalid number format for multiplier"));

        assertTrue(radiusWarned, "Expected warning for invalid spawn radius");
        assertTrue(multiplierWarned, "Expected warning for invalid multiplier");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Empty or blank world names should fall back to default world with warning")
    void testEmptyWorldName(String blankWorld) {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("PluginConfigTestBlankWorld_" + blankWorld.hashCode());
        logger.addHandler(logHandler);

        String yaml = "elytra:\n  world: \"" + blankWorld + "\"\n";
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section, logger);

        assertEquals(PluginConfig.DEFAULT_WORLD, config.getWorldName());
        boolean worldWarned = logHandler.getRecords().stream()
                .anyMatch(r -> r.getMessage().contains("world name is empty"));
        assertTrue(worldWarned, "Expected warning for empty world name");
    }

    @Test
    @DisplayName("Null ConfigurationSection should safely fall back to defaults without NullPointerException")
    void testNullConfigurationSection() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("PluginConfigTestNull");
        logger.addHandler(logHandler);

        PluginConfig config = PluginConfig.load(null, logger);

        assertEquals(PluginConfig.DEFAULT_ACTIVATION_MODE, config.getActivationMode());
        assertEquals(PluginConfig.DEFAULT_SPAWN_RADIUS, config.getSpawnRadius());
        assertEquals(PluginConfig.DEFAULT_MULTIPLIER, config.getMultiplier(), 0.001);
        assertEquals(PluginConfig.DEFAULT_BOOST_ENABLED, config.isBoostEnabled());
        assertEquals(PluginConfig.DEFAULT_WORLD, config.getWorldName());

        assertFalse(logHandler.getRecords().isEmpty());
    }

    @Test
    @DisplayName("Setters and in-place reload update configuration correctly")
    void testSettersAndReload() {
        PluginConfig config = PluginConfig.defaults();

        config.setActivationMode(ActivationMode.SNEAK);
        assertEquals(ActivationMode.SNEAK, config.getActivationMode());

        config.setSpawnRadius(80);
        assertEquals(80, config.getSpawnRadius());

        config.setMultiplier(7.2);
        assertEquals(7.2, config.getMultiplier(), 0.001);
        assertEquals(7, config.getMultiplyValue());

        config.setBoostEnabled(false);
        assertFalse(config.isBoostEnabled());

        config.setWorldName("nether");
        assertEquals("nether", config.getWorldName());

        config.setLegacyMessage("Custom Msg");
        assertEquals("Custom Msg", config.getLegacyMessage());

        // Now reload with fresh config
        String newYaml = """
                activation:
                  mode: SWAP
                elytra:
                  spawn-radius: 40
                  multiplier: 6.0
                  boost-enabled: true
                  world: "world"
                """;
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(newYaml));

        config.reload(section);
        assertEquals(ActivationMode.SWAP, config.getActivationMode());
        assertEquals(40, config.getSpawnRadius());
        assertEquals(6.0, config.getMultiplier(), 0.001);
        assertTrue(config.isBoostEnabled());
        assertEquals("world", config.getWorldName());
    }

    @Test
    @DisplayName("Missing activation.mode should fall back to DEFAULT_ACTIVATION_MODE (SWAP)")
    void testMissingActivationModeFallsBackToDefault() {
        String yaml = """
                elytra:
                  spawn-radius: 60
                  multiplier: 4.5
                """;
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(ActivationMode.SWAP, config.getActivationMode());
        assertEquals(60, config.getSpawnRadius());
        assertEquals(4.5, config.getMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Invalid activation.mode string should fall back to SWAP and log warning")
    void testInvalidActivationModeFallsBackToDefault() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("PluginConfigTestInvalidMode");
        logger.addHandler(logHandler);

        String yaml = """
                activation:
                  mode: "NOT_A_VALID_MODE"
                """;
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section, logger);

        assertEquals(ActivationMode.SWAP, config.getActivationMode());
        assertTrue(logHandler.getRecords().stream()
                .anyMatch(r -> r.getMessage().contains("Invalid activation mode")));
    }

    @Test
    @DisplayName("Mixed modern and legacy configuration keys are resolved correctly")
    void testMixedModernAndLegacyConfiguration() {
        String yaml = """
                activation:
                  mode: SNEAK
                spawnRadius: 42
                elytra:
                  multiplier: 7.7
                  boost-enabled: false
                world: "legacy_mix_world"
                message: "Mixed config message"
                """;
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(ActivationMode.SNEAK, config.getActivationMode());
        assertEquals(42, config.getSpawnRadius()); // from legacy spawnRadius
        assertEquals(7.7, config.getMultiplier(), 0.001); // from modern elytra.multiplier
        assertFalse(config.isBoostEnabled()); // from modern elytra.boost-enabled
        assertEquals("legacy_mix_world", config.getWorldName()); // from legacy world
        assertEquals("Mixed config message", config.getLegacyMessage());
    }

    @Test
    @DisplayName("Boolean string representations in modern and legacy keys parse correctly")
    void testBooleanStringParsing() {
        String yamlModernString = """
                elytra:
                  boost-enabled: "false"
                """;
        YamlConfiguration s1 = new YamlConfiguration();
        assertDoesNotThrow(() -> s1.loadFromString(yamlModernString));
        PluginConfig c1 = PluginConfig.load(s1);
        assertFalse(c1.isBoostEnabled());

        String yamlLegacyString = """
                boostEnabled: "false"
                """;
        YamlConfiguration s2 = new YamlConfiguration();
        assertDoesNotThrow(() -> s2.loadFromString(yamlLegacyString));
        PluginConfig c2 = PluginConfig.load(s2);
        assertFalse(c2.isBoostEnabled());

        String yamlLegacyTrueString = """
                boostEnabled: "true"
                """;
        YamlConfiguration s3 = new YamlConfiguration();
        assertDoesNotThrow(() -> s3.loadFromString(yamlLegacyTrueString));
        PluginConfig c3 = PluginConfig.load(s3);
        assertTrue(c3.isBoostEnabled());
    }

    @Test
    @DisplayName("Boundary values (0 spawn radius, 0.0 multiplier) are valid and preserved")
    void testBoundaryValues() {
        String yaml = """
                elytra:
                  spawn-radius: 0
                  multiplier: 0.0
                """;
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(yaml));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(0, config.getSpawnRadius());
        assertEquals(0.0, config.getMultiplier(), 0.001);
        assertEquals(0, config.getMultiplyValue());
    }

    @Test
    @DisplayName("Completely empty YAML configuration uses all default values")
    void testCompletelyEmptyYaml() {
        YamlConfiguration section = new YamlConfiguration();
        assertDoesNotThrow(() -> section.loadFromString(""));

        PluginConfig config = PluginConfig.load(section);

        assertEquals(ActivationMode.SWAP, config.getActivationMode());
        assertEquals(PluginConfig.DEFAULT_SPAWN_RADIUS, config.getSpawnRadius());
        assertEquals(PluginConfig.DEFAULT_MULTIPLIER, config.getMultiplier(), 0.001);
        assertEquals(PluginConfig.DEFAULT_BOOST_ENABLED, config.isBoostEnabled());
        assertEquals(PluginConfig.DEFAULT_WORLD, config.getWorldName());
        assertNull(config.getLegacyMessage());
    }

    private static class TestLogHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}

        public List<LogRecord> getRecords() {
            return records;
        }
    }
}
