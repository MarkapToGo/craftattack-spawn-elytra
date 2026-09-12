package de.coolepizza.craftattack;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Supported activation modes for triggering the spawn elytra boost.
 */
public enum ActivationMode {

    SWAP("key.swapOffhand"),
    SNEAK("key.sneak");

    private final String keybind;

    ActivationMode(String keybind) {
        this.keybind = keybind;
    }

    /**
     * Gets the keybind identifier used for Adventure action bar keybind components.
     *
     * @return the keybind string (e.g. "key.swapOffhand" or "key.sneak")
     */
    public String getKeybind() {
        return keybind;
    }

    /**
     * Safely parses an activation mode from a raw string.
     * If the raw string is null, empty, or not a valid mode name,
     * logs a warning (if a logger is provided) and falls back to {@link #SWAP}.
     *
     * @param rawMode the raw string from configuration
     * @param logger  optional logger to record warnings
     * @return the parsed ActivationMode, or SWAP as fallback
     */
    public static ActivationMode fromString(String rawMode, Logger logger) {
        if (rawMode == null || rawMode.trim().isEmpty()) {
            if (logger != null) {
                logger.warning("Configuration 'activation.mode' is missing. Defaulting to SWAP.");
            }
            return SWAP;
        }

        try {
            return ActivationMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            if (logger != null) {
                logger.warning("Invalid activation mode '" + rawMode + "'. Defaulting to SWAP.");
            }
            return SWAP;
        }
    }

    /**
     * Safely parses an activation mode from a raw string without logging.
     *
     * @param rawMode the raw string from configuration
     * @return the parsed ActivationMode, or SWAP as fallback
     */
    public static ActivationMode fromString(String rawMode) {
        return fromString(rawMode, null);
    }

    /**
     * Alias for {@link #fromString(String, Logger)}.
     */
    public static ActivationMode parse(String rawMode, Logger logger) {
        return fromString(rawMode, logger);
    }

    /**
     * Alias for {@link #fromString(String)}.
     */
    public static ActivationMode parse(String rawMode) {
        return fromString(rawMode, null);
    }
}
