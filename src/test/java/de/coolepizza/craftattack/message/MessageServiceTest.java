package de.coolepizza.craftattack.message;

import de.coolepizza.craftattack.ActivationMode;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MessageServiceTest {

    private boolean containsKeybind(Component component, String expectedKeybind) {
        if (component instanceof KeybindComponent kc && kc.keybind().equals(expectedKeybind)) {
            return true;
        }
        for (Component child : component.children()) {
            if (containsKeybind(child, expectedKeybind)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("Default MessageService should provide non-null defaults for all standard keys")
    void testDefaultMessagesExist() {
        MessageService service = MessageService.defaults();

        assertNotNull(service.getRawPrefix());
        assertNotNull(service.getRawMessage(MessageService.KEY_BOOST_ACTIONBAR));
        assertNotNull(service.getRawMessage(MessageService.KEY_RELOAD_SUCCESS));
        assertNotNull(service.getRawMessage(MessageService.KEY_NO_PERMISSION));
        assertNotNull(service.getRawMessage(MessageService.KEY_COMMAND_USAGE));

        assertFalse(service.getPrefixComponent().equals(Component.empty()));
        assertFalse(service.getBoostActionBar(ActivationMode.SWAP).equals(Component.empty()));
    }

    @Test
    @DisplayName("Boost actionbar resolves <key> placeholder to key.swapOffhand for SWAP mode")
    void testActionBarKeybindSwap() {
        MessageService service = MessageService.defaults();
        Component component = service.getBoostActionBar(ActivationMode.SWAP);

        assertTrue(containsKeybind(component, "key.swapOffhand"),
                "Expected component to contain key.swapOffhand keybind component");
    }

    @Test
    @DisplayName("Boost actionbar resolves <key> placeholder to key.sneak for SNEAK mode")
    void testActionBarKeybindSneak() {
        MessageService service = MessageService.defaults();
        Component component = service.getBoostActionBar(ActivationMode.SNEAK);

        assertTrue(containsKeybind(component, "key.sneak"),
                "Expected component to contain key.sneak keybind component");
    }

    @Test
    @DisplayName("Explicit keybind tag <key:identifier> correctly formats as specified Adventure KeybindComponent")
    void testExplicitKeybindTag() {
        MessageService service = MessageService.defaults();
        Component component = service.parse("<gray>Press <key:key.jump> to jump.", ActivationMode.SWAP);

        assertTrue(containsKeybind(component, "key.jump"),
                "Expected component to contain explicit key.jump keybind component");
    }

    @Test
    @DisplayName("Legacy %key% placeholder is converted and parsed as KeybindComponent")
    void testLegacyPercentKeyPlaceholder() {
        MessageService service = MessageService.defaults();
        Component component = service.parse("Drücke %key% um zu boosten.", ActivationMode.SWAP);

        assertTrue(containsKeybind(component, "key.swapOffhand"),
                "Expected component to resolve %key% into key.swapOffhand keybind component");
    }

    @Test
    @DisplayName("Placeholder <prefix> resolves to configured prefix Component")
    void testPrefixPlaceholderResolution() {
        MessageService service = MessageService.defaults();
        Component component = service.getMessage(MessageService.KEY_RELOAD_SUCCESS);

        assertNotNull(component);
        // Default prefix has content "SpawnElytra"
        String plainText = component.toString();
        assertTrue(plainText.contains("SpawnElytra"), "Expected component to contain prefix text");
    }

    @Test
    @DisplayName("getPrefixedMessage prepends prefix if template does not contain <prefix>")
    void testGetPrefixedMessagePrependsIfMissing() {
        String yaml = """
                prefix: "<gold>[MyPrefix] "
                messages:
                  plain-notice: "<yellow>Some plain notice without prefix tag."
                """;

        YamlConfiguration config = new YamlConfiguration();
        assertDoesNotThrow(() -> config.loadFromString(yaml));

        MessageService service = new MessageService(config, null);
        Component component = service.getPrefixedMessage("plain-notice");

        assertNotNull(component);
        assertTrue(component.toString().contains("MyPrefix"));
        assertTrue(component.toString().contains("Some plain notice"));
    }

    @Test
    @DisplayName("Missing language keys return fallback string without throwing exceptions or crashing")
    void testMissingKeyFallbackWithoutCrash() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("MessageServiceTestMissingKey");
        logger.addHandler(logHandler);

        MessageService service = new MessageService((YamlConfiguration) null, logger);

        Component result = assertDoesNotThrow(() -> service.getMessage("completely-unknown-key"));
        assertNotNull(result);
        assertEquals("completely-unknown-key", service.getRawMessage("completely-unknown-key"));
        assertFalse(logHandler.getRecords().isEmpty());
        assertTrue(logHandler.getRecords().get(0).getMessage().contains("Missing message key"));
    }

    @Test
    @DisplayName("Malformed MiniMessage tags fall back to plain text component safely")
    void testMalformedMiniMessageSyntaxFallback() {
        TestLogHandler logHandler = new TestLogHandler();
        Logger logger = Logger.getLogger("MessageServiceTestMalformed");
        logger.addHandler(logHandler);

        MessageService service = new MessageService((YamlConfiguration) null, logger);

        // String with unclosed or invalid tags that could cause parser issues
        String malformed = "<invalid:tag:without:end>Text message";
        Component result = assertDoesNotThrow(() -> service.parse(malformed, ActivationMode.SWAP));

        assertNotNull(result);
        assertTrue(result instanceof TextComponent);
    }

    @Test
    @DisplayName("withCustomActionBarMessage correctly overrides boost-actionbar")
    void testCustomActionBarMessage() {
        MessageService service = MessageService.withCustomActionBarMessage("Custom text <key>");
        Component component = service.getBoostActionBar(ActivationMode.SNEAK);

        assertTrue(containsKeybind(component, "key.sneak"));
    }

    @Test
    @DisplayName("Load custom language configuration section from YAML")
    void testYamlConfigurationLoading() {
        String yaml = """
                prefix: "<blue>[Server] "
                messages:
                  boost-actionbar: "<green>Use <key> to fly!"
                  reload-success: "<prefix><green>Reloaded!"
                  no-permission: "<prefix><red>Denied!"
                """;

        YamlConfiguration config = new YamlConfiguration();
        assertDoesNotThrow(() -> config.loadFromString(yaml));

        MessageService service = new MessageService(config, null);

        assertEquals("<blue>[Server] ", service.getRawPrefix());
        assertEquals("<green>Use <key> to fly!", service.getRawMessage(MessageService.KEY_BOOST_ACTIONBAR));

        Component boost = service.getBoostActionBar(ActivationMode.SWAP);
        assertTrue(containsKeybind(boost, "key.swapOffhand"));
    }

    @Test
    @DisplayName("Legacy %prefix% placeholder is converted and resolves prefix Component")
    void testLegacyPercentPrefixPlaceholder() {
        MessageService service = MessageService.defaults();
        Component component = service.parse("%prefix%<green>Hello world", ActivationMode.SWAP);

        assertNotNull(component);
        assertTrue(component.toString().contains("SpawnElytra"));
        assertTrue(component.toString().contains("Hello world"));
    }

    @Test
    @DisplayName("sendBoostActionBar dispatches action bar to Player and handles null Player safely")
    void testSendBoostActionBar() {
        MessageService service = MessageService.defaults();
        AtomicReference<Component> receivedBar = new AtomicReference<>();

        Player mockPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendActionBar") && args.length > 0) {
                        receivedBar.set((Component) args[0]);
                    }
                    return null;
                }
        );

        service.sendBoostActionBar(mockPlayer, ActivationMode.SNEAK);
        assertNotNull(receivedBar.get());
        assertTrue(containsKeybind(receivedBar.get(), "key.sneak"));

        // Null player should not throw any exception
        assertDoesNotThrow(() -> service.sendBoostActionBar(null, ActivationMode.SWAP));
    }

    @Test
    @DisplayName("sendPrefixedMessage and sendMessage dispatch to Audience and handle null safely")
    void testSendAudienceMessages() {
        MessageService service = MessageService.defaults();
        AtomicReference<Component> receivedMsg = new AtomicReference<>();

        Audience mockAudience = (Audience) Proxy.newProxyInstance(
                Audience.class.getClassLoader(),
                new Class<?>[]{Audience.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args.length > 0) {
                        receivedMsg.set((Component) args[0]);
                    }
                    return null;
                }
        );

        service.sendPrefixedMessage(mockAudience, MessageService.KEY_RELOAD_SUCCESS);
        assertNotNull(receivedMsg.get());
        assertTrue(receivedMsg.get().toString().contains("SpawnElytra"));

        receivedMsg.set(null);
        service.sendMessage(mockAudience, MessageService.KEY_BOOST_ACTIONBAR);
        assertNotNull(receivedMsg.get());

        // Null audience should not throw
        assertDoesNotThrow(() -> service.sendPrefixedMessage(null, MessageService.KEY_RELOAD_SUCCESS));
        assertDoesNotThrow(() -> service.sendMessage(null, MessageService.KEY_BOOST_ACTIONBAR));
    }

    @Test
    @DisplayName("parse returns Component.empty() when rawMessage is null or empty")
    void testParseNullAndEmpty() {
        MessageService service = MessageService.defaults();
        assertEquals(Component.empty(), service.parse(null, ActivationMode.SWAP));
        assertEquals(Component.empty(), service.parse("", ActivationMode.SWAP));
    }

    @Test
    @DisplayName("parse supports custom additional TagResolvers")
    void testAdditionalTagResolvers() {
        MessageService service = MessageService.defaults();
        TagResolver customResolver = TagResolver.resolver("user", (args, ctx) -> Tag.inserting(Component.text("TestUser")));

        Component component = service.parse("<prefix><yellow>Welcome <user>!", ActivationMode.SWAP, customResolver);
        assertNotNull(component);
        assertTrue(component.toString().contains("TestUser"));
        assertTrue(component.toString().contains("SpawnElytra"));
    }

    @Test
    @DisplayName("Precomputed components return identical cached references on consecutive calls")
    void testPrecomputedComponentsCached() {
        MessageService service = MessageService.defaults();

        Component prefix1 = service.getPrefixComponent();
        Component prefix2 = service.getPrefixComponent();
        assertSame(prefix1, prefix2, "Prefix component should return cached instance");

        Component boostSwap1 = service.getBoostActionBar(ActivationMode.SWAP);
        Component boostSwap2 = service.getBoostActionBar(ActivationMode.SWAP);
        assertSame(boostSwap1, boostSwap2, "SWAP action bar component should return cached instance");

        Component boostSneak1 = service.getBoostActionBar(ActivationMode.SNEAK);
        Component boostSneak2 = service.getBoostActionBar(ActivationMode.SNEAK);
        assertSame(boostSneak1, boostSneak2, "SNEAK action bar component should return cached instance");

        Component reloadMsg1 = service.getPrefixedMessage(MessageService.KEY_RELOAD_SUCCESS);
        Component reloadMsg2 = service.getPrefixedMessage(MessageService.KEY_RELOAD_SUCCESS);
        assertSame(reloadMsg1, reloadMsg2, "Prefixed reload message should return cached instance");
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
