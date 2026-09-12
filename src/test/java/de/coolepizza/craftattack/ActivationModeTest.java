package de.coolepizza.craftattack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivationModeTest {

    @Test
    @DisplayName("ActivationMode keybind values should match Minecraft client keybinds")
    void testKeybindIdentifiers() {
        assertEquals("key.swapOffhand", ActivationMode.SWAP.getKeybind());
        assertEquals("key.sneak", ActivationMode.SNEAK.getKeybind());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SWAP", "swap", "Swap", "  SWAP  ", "  swap  "})
    @DisplayName("Safe parsing correctly parses SWAP in any casing and with whitespace")
    void testParseSwap(String input) {
        assertEquals(ActivationMode.SWAP, ActivationMode.fromString(input));
        assertEquals(ActivationMode.SWAP, ActivationMode.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SNEAK", "sneak", "Sneak", "  SNEAK  ", "  sneak  "})
    @DisplayName("Safe parsing correctly parses SNEAK in any casing and with whitespace")
    void testParseSneak(String input) {
        assertEquals(ActivationMode.SNEAK, ActivationMode.fromString(input));
        assertEquals(ActivationMode.SNEAK, ActivationMode.parse(input));
    }

    @Test
    @DisplayName("Safe parsing falls back to SWAP when input is null or blank and logs warning")
    void testFallbackOnMissing() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("ActivationModeTestNull");
        logger.addHandler(logHandler);

        assertEquals(ActivationMode.SWAP, ActivationMode.fromString(null, logger));
        assertFalse(logHandler.getRecords().isEmpty());
        assertTrue(logHandler.getRecords().get(0).getMessage().contains("missing"));

        logHandler.getRecords().clear();
        assertEquals(ActivationMode.SWAP, ActivationMode.fromString("   ", logger));
        assertFalse(logHandler.getRecords().isEmpty());
        assertTrue(logHandler.getRecords().get(0).getMessage().contains("missing"));
    }

    @Test
    @DisplayName("Safe parsing falls back to SWAP when input is invalid and logs warning")
    void testFallbackOnInvalid() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("ActivationModeTestInvalid");
        logger.addHandler(logHandler);

        assertEquals(ActivationMode.SWAP, ActivationMode.fromString("INVALID_MODE", logger));
        assertFalse(logHandler.getRecords().isEmpty());
        assertTrue(logHandler.getRecords().get(0).getMessage().contains("Invalid activation mode"));
        assertTrue(logHandler.getRecords().get(0).getMessage().contains("INVALID_MODE"));
    }

    @Test
    @DisplayName("Safe parsing without logger behaves gracefully without throwing NullPointerException")
    void testNullLoggerHandling() {
        assertEquals(ActivationMode.SWAP, ActivationMode.fromString(null, null));
        assertEquals(ActivationMode.SWAP, ActivationMode.fromString("UNKNOWN", null));
        assertEquals(ActivationMode.SWAP, ActivationMode.fromString(null));
        assertEquals(ActivationMode.SWAP, ActivationMode.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("ActivationMode enum values() and valueOf() contract")
    void testEnumConstants() {
        ActivationMode[] values = ActivationMode.values();
        assertEquals(2, values.length);
        assertEquals(ActivationMode.SWAP, ActivationMode.valueOf("SWAP"));
        assertEquals(ActivationMode.SNEAK, ActivationMode.valueOf("SNEAK"));
    }

    @Test
    @DisplayName("parse with logger delegates properly to fromString")
    void testParseAliasWithLogger() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("ActivationModeParseAlias");
        logger.addHandler(logHandler);

        assertEquals(ActivationMode.SNEAK, ActivationMode.parse("SNEAK", logger));
        assertEquals(ActivationMode.SWAP, ActivationMode.parse("INVALID", logger));
        assertFalse(logHandler.getRecords().isEmpty());
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
