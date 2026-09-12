package de.coolepizza.craftattack.config;

import de.coolepizza.craftattack.ActivationMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Configuration service and value holder for CraftAttack Spawn Elytra.
 * <p>
 * Supports both modern namespaced keys and legacy flat keys for seamless backwards compatibility,
 * while safely validating all values and applying sensible defaults without overwriting user files.
 */
public class PluginConfig {

    // Modern configuration keys
    public static final String KEY_ACTIVATION_MODE = "activation.mode";
    public static final String KEY_ELYTRA_SPAWN_RADIUS = "elytra.spawn-radius";
    public static final String KEY_ELYTRA_MULTIPLIER = "elytra.multiplier";
    public static final String KEY_ELYTRA_BOOST_ENABLED = "elytra.boost-enabled";
    public static final String KEY_ELYTRA_WORLD = "elytra.world";

    // Legacy configuration keys (maintained for backwards compatibility)
    public static final String LEGACY_KEY_SPAWN_RADIUS = "spawnRadius";
    public static final String LEGACY_KEY_MULTIPLY_VALUE = "multiplyValue";
    public static final String LEGACY_KEY_BOOST_ENABLED = "boostEnabled";
    public static final String LEGACY_KEY_WORLD = "world";
    public static final String LEGACY_KEY_MESSAGE = "message";

    // Sensible defaults
    public static final ActivationMode DEFAULT_ACTIVATION_MODE = ActivationMode.SWAP;
    public static final int DEFAULT_SPAWN_RADIUS = 50;
    public static final double DEFAULT_MULTIPLIER = 5.0;
    public static final boolean DEFAULT_BOOST_ENABLED = true;
    public static final String DEFAULT_WORLD = "world";

    private ActivationMode activationMode = DEFAULT_ACTIVATION_MODE;
    private int spawnRadius = DEFAULT_SPAWN_RADIUS;
    private double multiplier = DEFAULT_MULTIPLIER;
    private boolean boostEnabled = DEFAULT_BOOST_ENABLED;
    private String worldName = DEFAULT_WORLD;
    private String legacyMessage = null;

    public PluginConfig() {
    }

    public PluginConfig(ActivationMode activationMode, int spawnRadius, double multiplier, boolean boostEnabled, String worldName, String legacyMessage) {
        this.activationMode = activationMode != null ? activationMode : DEFAULT_ACTIVATION_MODE;
        this.spawnRadius = Math.max(0, spawnRadius);
        this.multiplier = Math.max(0.0, multiplier);
        this.boostEnabled = boostEnabled;
        this.worldName = (worldName != null && !worldName.trim().isEmpty()) ? worldName.trim() : DEFAULT_WORLD;
        this.legacyMessage = legacyMessage;
    }

    /**
     * Creates a new {@link PluginConfig} instance initialized with default settings.
     *
     * @return default PluginConfig instance
     */
    public static PluginConfig defaults() {
        return new PluginConfig();
    }

    /**
     * Loads a {@link PluginConfig} from a {@link ConfigurationSection} without logger output.
     *
     * @param config the configuration section to read from
     * @return loaded PluginConfig instance
     */
    public static PluginConfig load(ConfigurationSection config) {
        return load(config, null);
    }

    /**
     * Loads a {@link PluginConfig} from a {@link ConfigurationSection} with optional logging.
     *
     * @param config the configuration section to read from
     * @param logger optional logger for recording warnings during parsing
     * @return loaded PluginConfig instance
     */
    public static PluginConfig load(ConfigurationSection config, Logger logger) {
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.reload(config, logger);
        return pluginConfig;
    }

    /**
     * Reloads all configuration values from the given {@link ConfigurationSection}.
     * Gracefully checks modern keys first, falls back to legacy keys, validates values,
     * and defaults missing or invalid fields.
     *
     * @param config the configuration section
     * @param logger optional logger for recording warnings
     */
    public void reload(ConfigurationSection config, Logger logger) {
        if (config == null) {
            if (logger != null) {
                logger.warning("Configuration section is null. Default configuration values will be used.");
            }
            this.activationMode = DEFAULT_ACTIVATION_MODE;
            this.spawnRadius = DEFAULT_SPAWN_RADIUS;
            this.multiplier = DEFAULT_MULTIPLIER;
            this.boostEnabled = DEFAULT_BOOST_ENABLED;
            this.worldName = DEFAULT_WORLD;
            this.legacyMessage = null;
            return;
        }

        // 1. Activation Mode
        String rawMode = config.getString(KEY_ACTIVATION_MODE);
        this.activationMode = ActivationMode.fromString(rawMode, logger);

        // 2. Spawn Radius (modern: elytra.spawn-radius, legacy: spawnRadius)
        this.spawnRadius = readSpawnRadius(config, logger);

        // 3. Multiplier (modern: elytra.multiplier, legacy: multiplyValue)
        this.multiplier = readMultiplier(config, logger);

        // 4. Boost Enabled (modern: elytra.boost-enabled, legacy: boostEnabled)
        this.boostEnabled = readBoostEnabled(config);

        // 5. World (modern: elytra.world, legacy: world)
        this.worldName = readWorld(config, logger);

        // 6. Legacy Message (legacy: message)
        this.legacyMessage = config.getString(LEGACY_KEY_MESSAGE, null);
    }

    /**
     * Convenience reload without logger.
     *
     * @param config configuration section
     */
    public void reload(ConfigurationSection config) {
        reload(config, null);
    }

    private int readSpawnRadius(ConfigurationSection config, Logger logger) {
        int radius = DEFAULT_SPAWN_RADIUS;
        boolean found = false;

        if (config.isInt(KEY_ELYTRA_SPAWN_RADIUS)) {
            radius = config.getInt(KEY_ELYTRA_SPAWN_RADIUS);
            found = true;
        } else if (config.isInt(LEGACY_KEY_SPAWN_RADIUS)) {
            radius = config.getInt(LEGACY_KEY_SPAWN_RADIUS);
            found = true;
        } else if (config.contains(KEY_ELYTRA_SPAWN_RADIUS) || config.contains(LEGACY_KEY_SPAWN_RADIUS)) {
            String raw = config.getString(KEY_ELYTRA_SPAWN_RADIUS, config.getString(LEGACY_KEY_SPAWN_RADIUS));
            if (raw != null) {
                try {
                    radius = Integer.parseInt(raw.trim());
                    found = true;
                } catch (NumberFormatException e) {
                    if (logger != null) {
                        logger.warning("Invalid number format for spawn radius '" + raw + "'. Defaulting to " + DEFAULT_SPAWN_RADIUS);
                    }
                }
            }
        }

        if (found && radius < 0) {
            if (logger != null) {
                logger.warning("Spawn radius cannot be negative (" + radius + "). Defaulting to " + DEFAULT_SPAWN_RADIUS);
            }
            radius = DEFAULT_SPAWN_RADIUS;
        }
        return radius;
    }

    private double readMultiplier(ConfigurationSection config, Logger logger) {
        double mult = DEFAULT_MULTIPLIER;
        boolean found = false;

        if (config.isDouble(KEY_ELYTRA_MULTIPLIER) || config.isInt(KEY_ELYTRA_MULTIPLIER)) {
            mult = config.getDouble(KEY_ELYTRA_MULTIPLIER);
            found = true;
        } else if (config.isDouble(LEGACY_KEY_MULTIPLY_VALUE) || config.isInt(LEGACY_KEY_MULTIPLY_VALUE)) {
            mult = config.getDouble(LEGACY_KEY_MULTIPLY_VALUE);
            found = true;
        } else if (config.contains(KEY_ELYTRA_MULTIPLIER) || config.contains(LEGACY_KEY_MULTIPLY_VALUE)) {
            String raw = config.getString(KEY_ELYTRA_MULTIPLIER, config.getString(LEGACY_KEY_MULTIPLY_VALUE));
            if (raw != null) {
                try {
                    mult = Double.parseDouble(raw.trim());
                    found = true;
                } catch (NumberFormatException e) {
                    if (logger != null) {
                        logger.warning("Invalid number format for multiplier '" + raw + "'. Defaulting to " + DEFAULT_MULTIPLIER);
                    }
                }
            }
        }

        if (found && mult < 0.0) {
            if (logger != null) {
                logger.warning("Multiplier cannot be negative (" + mult + "). Defaulting to " + DEFAULT_MULTIPLIER);
            }
            mult = DEFAULT_MULTIPLIER;
        }
        return mult;
    }

    private boolean readBoostEnabled(ConfigurationSection config) {
        if (config.isBoolean(KEY_ELYTRA_BOOST_ENABLED)) {
            return config.getBoolean(KEY_ELYTRA_BOOST_ENABLED);
        }
        if (config.isBoolean(LEGACY_KEY_BOOST_ENABLED)) {
            return config.getBoolean(LEGACY_KEY_BOOST_ENABLED);
        }
        if (config.contains(KEY_ELYTRA_BOOST_ENABLED)) {
            return Boolean.parseBoolean(config.getString(KEY_ELYTRA_BOOST_ENABLED));
        }
        if (config.contains(LEGACY_KEY_BOOST_ENABLED)) {
            return Boolean.parseBoolean(config.getString(LEGACY_KEY_BOOST_ENABLED));
        }
        return DEFAULT_BOOST_ENABLED;
    }

    private String readWorld(ConfigurationSection config, Logger logger) {
        String world = null;
        if (config.isString(KEY_ELYTRA_WORLD)) {
            world = config.getString(KEY_ELYTRA_WORLD);
        } else if (config.isString(LEGACY_KEY_WORLD)) {
            world = config.getString(LEGACY_KEY_WORLD);
        }

        if (world == null || world.trim().isEmpty()) {
            if (config.contains(KEY_ELYTRA_WORLD) || config.contains(LEGACY_KEY_WORLD)) {
                if (logger != null) {
                    logger.warning("Configured world name is empty. Defaulting to '" + DEFAULT_WORLD + "'.");
                }
            }
            return DEFAULT_WORLD;
        }
        return world.trim();
    }

    public ActivationMode getActivationMode() {
        return activationMode;
    }

    public void setActivationMode(ActivationMode activationMode) {
        this.activationMode = Objects.requireNonNull(activationMode, "activationMode cannot be null");
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(int spawnRadius) {
        this.spawnRadius = Math.max(0, spawnRadius);
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Integer representation of multiplier for legacy code compatibility.
     *
     * @return multiplier cast to int
     */
    public int getMultiplyValue() {
        return (int) multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = Math.max(0.0, multiplier);
    }

    public boolean isBoostEnabled() {
        return boostEnabled;
    }

    public void setBoostEnabled(boolean boostEnabled) {
        this.boostEnabled = boostEnabled;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = (worldName != null && !worldName.trim().isEmpty()) ? worldName.trim() : DEFAULT_WORLD;
    }

    public String getLegacyMessage() {
        return legacyMessage;
    }

    public void setLegacyMessage(String legacyMessage) {
        this.legacyMessage = legacyMessage;
    }
}
