package de.coolepizza.craftattack.message;

import de.coolepizza.craftattack.ActivationMode;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service managing localization, language files, MiniMessage deserialization,
 * and keybind placeholder substitution for CraftAttack Spawn Elytra.
 */
public class MessageService {

    public static final String KEY_PREFIX = "prefix";
    public static final String KEY_BOOST_ACTIONBAR = "boost-actionbar";
    public static final String KEY_RELOAD_SUCCESS = "reload-success";
    public static final String KEY_NO_PERMISSION = "no-permission";
    public static final String KEY_COMMAND_USAGE = "command-usage";

    public static final String DEFAULT_PREFIX = "<gray>[<aqua>SpawnElytra<gray>] ";
    public static final String DEFAULT_BOOST_ACTIONBAR = "<gray>Drücke <yellow><key></yellow> um dich zu boosten.";
    public static final String DEFAULT_RELOAD_SUCCESS = "<prefix><green>Konfiguration und Sprache wurden erfolgreich neu geladen.";
    public static final String DEFAULT_NO_PERMISSION = "<prefix><red>Dazu hast du keine Berechtigung.";
    public static final String DEFAULT_COMMAND_USAGE = "<prefix><yellow>Verwendung: <white>/spawnelytra reload";

    private static final Map<String, String> DEFAULT_MESSAGES;

    static {
        Map<String, String> defaults = new HashMap<>();
        defaults.put(KEY_BOOST_ACTIONBAR, DEFAULT_BOOST_ACTIONBAR);
        defaults.put(KEY_RELOAD_SUCCESS, DEFAULT_RELOAD_SUCCESS);
        defaults.put(KEY_NO_PERMISSION, DEFAULT_NO_PERMISSION);
        defaults.put(KEY_COMMAND_USAGE, DEFAULT_COMMAND_USAGE);
        DEFAULT_MESSAGES = Collections.unmodifiableMap(defaults);
    }

    private final Plugin plugin;
    private final File languageFile;
    private final Logger logger;
    private final MiniMessage miniMessage;
    private ConfigurationSection languageConfig;
    private String customActionBarMessage;

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
        this.languageFile = plugin != null ? new File(plugin.getDataFolder(), "language.yml") : null;
        this.logger = plugin != null ? plugin.getLogger() : null;
        this.miniMessage = MiniMessage.miniMessage();
        reload();
    }

    public MessageService(ConfigurationSection languageConfig, Logger logger) {
        this.plugin = null;
        this.languageFile = null;
        this.logger = logger;
        this.miniMessage = MiniMessage.miniMessage();
        this.languageConfig = languageConfig;
    }

    public MessageService(File languageFile, Logger logger) {
        this.plugin = null;
        this.languageFile = languageFile;
        this.logger = logger;
        this.miniMessage = MiniMessage.miniMessage();
        reload();
    }

    /**
     * Creates a MessageService initialized with built-in defaults.
     */
    public static MessageService defaults() {
        return new MessageService((ConfigurationSection) null, null);
    }

    /**
     * Creates a MessageService for the given plugin instance, saving default language.yml if missing.
     */
    public static MessageService create(Plugin plugin) {
        return new MessageService(plugin);
    }

    /**
     * Creates a MessageService that overrides the boost-actionbar message.
     * Useful for legacy constructor compatibility and unit tests.
     */
    public static MessageService withCustomActionBarMessage(String customActionBarMessage) {
        MessageService service = defaults();
        service.setCustomActionBarMessage(customActionBarMessage);
        return service;
    }

    /**
     * Reloads the language configuration from disk, preserving user customizations
     * and applying default fallbacks.
     */
    public void reload() {
        if (languageFile == null && plugin == null) {
            return;
        }

        File targetFile = languageFile;
        if (targetFile == null && plugin != null) {
            targetFile = new File(plugin.getDataFolder(), "language.yml");
        }

        if (targetFile != null) {
            // Save resource safely without overwriting existing files
            if (!targetFile.exists() && plugin != null) {
                try {
                    plugin.saveResource("language.yml", false);
                } catch (Exception e) {
                    if (logger != null) {
                        logger.warning("Could not save default language.yml: " + e.getMessage());
                    }
                }
            }

            YamlConfiguration yamlConfig = new YamlConfiguration();
            if (targetFile.exists()) {
                try {
                    yamlConfig.load(targetFile);
                } catch (Exception e) {
                    if (logger != null) {
                        logger.severe("Failed to parse language.yml! Check syntax: " + e.getMessage());
                    }
                }
            }

            // Apply bundled jar defaults as fallback if available
            if (plugin != null) {
                try (InputStream stream = plugin.getResource("language.yml")) {
                    if (stream != null) {
                        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                            YamlConfiguration jarDefaults = YamlConfiguration.loadConfiguration(reader);
                            yamlConfig.setDefaults(jarDefaults);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            this.languageConfig = yamlConfig;
        }
    }

    /**
     * Gets the raw string for a message key from configuration, or returns the built-in default.
     *
     * @param key message key (e.g. "boost-actionbar")
     * @return raw message string, or key if completely undefined
     */
    public String getRawMessage(String key) {
        if (KEY_BOOST_ACTIONBAR.equals(key) && customActionBarMessage != null) {
            return customActionBarMessage;
        }

        if (languageConfig != null) {
            if (languageConfig.isString("messages." + key)) {
                return languageConfig.getString("messages." + key);
            }
            if (languageConfig.isString(key)) {
                return languageConfig.getString(key);
            }
        }

        String fallback = DEFAULT_MESSAGES.get(key);
        if (fallback != null) {
            return fallback;
        }

        if (logger != null) {
            logger.warning("Missing message key '" + key + "' in language configuration.");
        }
        return key;
    }

    /**
     * Gets the raw prefix string.
     */
    public String getRawPrefix() {
        if (languageConfig != null && languageConfig.isString(KEY_PREFIX)) {
            return languageConfig.getString(KEY_PREFIX);
        }
        return DEFAULT_PREFIX;
    }

    /**
     * Parses the prefix as an Adventure {@link Component}.
     */
    public Component getPrefixComponent() {
        String rawPrefix = getRawPrefix();
        if (rawPrefix == null || rawPrefix.isEmpty()) {
            return Component.empty();
        }
        try {
            return miniMessage.deserialize(rawPrefix);
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to parse prefix '" + rawPrefix + "': " + e.getMessage());
            }
            return Component.text(rawPrefix);
        }
    }

    /**
     * Builds the action bar component for the specified activation mode.
     *
     * @param mode activation mode
     * @return action bar Component
     */
    public Component getBoostActionBar(ActivationMode mode) {
        String raw = getRawMessage(KEY_BOOST_ACTIONBAR);
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return parse(raw, mode);
    }

    /**
     * Sends the boost action bar message to the player.
     *
     * @param player player to receive action bar
     * @param mode   current activation mode
     */
    public void sendBoostActionBar(Player player, ActivationMode mode) {
        if (player != null) {
            player.sendActionBar(getBoostActionBar(mode));
        }
    }

    /**
     * Gets a localized component for a key with the specified activation mode and additional tag resolvers.
     *
     * @param key                 message key
     * @param mode                activation mode for <key> placeholder
     * @param additionalResolvers optional additional MiniMessage tag resolvers
     * @return parsed Component
     */
    public Component getMessage(String key, ActivationMode mode, TagResolver... additionalResolvers) {
        String raw = getRawMessage(key);
        return parse(raw, mode, additionalResolvers);
    }

    /**
     * Gets a localized component for a key, using default SWAP activation mode.
     *
     * @param key                 message key
     * @param additionalResolvers optional additional MiniMessage tag resolvers
     * @return parsed Component
     */
    public Component getMessage(String key, TagResolver... additionalResolvers) {
        return getMessage(key, ActivationMode.SWAP, additionalResolvers);
    }

    /**
     * Gets a localized component guaranteed to include the prefix.
     * If the message template already contains "<prefix>", it is resolved.
     * If not, the prefix is prepended.
     *
     * @param key                 message key
     * @param additionalResolvers optional additional MiniMessage tag resolvers
     * @return parsed Component with prefix
     */
    public Component getPrefixedMessage(String key, TagResolver... additionalResolvers) {
        String raw = getRawMessage(key);
        if (raw.contains("<prefix>") || raw.contains("%prefix%")) {
            return parse(raw, ActivationMode.SWAP, additionalResolvers);
        }
        return getPrefixComponent().append(parse(raw, ActivationMode.SWAP, additionalResolvers));
    }

    /**
     * Sends a prefixed message to an Audience (Player, ConsoleCommandSender, etc.).
     *
     * @param audience            message receiver
     * @param key                 message key
     * @param additionalResolvers optional additional MiniMessage tag resolvers
     */
    public void sendPrefixedMessage(Audience audience, String key, TagResolver... additionalResolvers) {
        if (audience != null) {
            audience.sendMessage(getPrefixedMessage(key, additionalResolvers));
        }
    }

    /**
     * Sends an unprefixed message to an Audience.
     *
     * @param audience            message receiver
     * @param key                 message key
     * @param additionalResolvers optional additional MiniMessage tag resolvers
     */
    public void sendMessage(Audience audience, String key, TagResolver... additionalResolvers) {
        if (audience != null) {
            audience.sendMessage(getMessage(key, additionalResolvers));
        }
    }

    /**
     * Deserializes a raw string using MiniMessage, resolving `<prefix>`, `<key>`, `<key:identifier>`,
     * and legacy `%key%` / `%prefix%`.
     * Gracefully falls back to plain text if MiniMessage syntax is invalid.
     *
     * @param rawMessage          raw template string
     * @param mode                current activation mode
     * @param additionalResolvers optional extra resolvers
     * @return parsed Component
     */
    public Component parse(String rawMessage, ActivationMode mode, TagResolver... additionalResolvers) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return Component.empty();
        }

        // Replace legacy placeholders if present
        String processed = rawMessage;
        if (processed.contains("%key%")) {
            processed = processed.replace("%key%", "<key>");
        }
        if (processed.contains("%prefix%")) {
            processed = processed.replace("%prefix%", "<prefix>");
        }

        ActivationMode activeMode = mode != null ? mode : ActivationMode.SWAP;

        TagResolver prefixResolver = TagResolver.resolver("prefix", (args, ctx) -> Tag.inserting(getPrefixComponent()));

        TagResolver keyResolver = TagResolver.resolver("key", (args, ctx) -> {
            if (args.hasNext()) {
                String identifier = args.pop().value();
                return Tag.inserting(Component.keybind(identifier));
            }
            return Tag.inserting(Component.keybind(activeMode.getKeybind()));
        });

        TagResolver combined;
        if (additionalResolvers != null && additionalResolvers.length > 0) {
            TagResolver[] all = new TagResolver[additionalResolvers.length + 2];
            all[0] = prefixResolver;
            all[1] = keyResolver;
            System.arraycopy(additionalResolvers, 0, all, 2, additionalResolvers.length);
            combined = TagResolver.resolver(all);
        } else {
            combined = TagResolver.resolver(prefixResolver, keyResolver);
        }

        try {
            return miniMessage.deserialize(processed, combined);
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to parse MiniMessage format for '" + rawMessage + "': " + e.getMessage());
            }
            return Component.text(rawMessage);
        }
    }

    public void setCustomActionBarMessage(String customActionBarMessage) {
        this.customActionBarMessage = customActionBarMessage;
    }

    public String getCustomActionBarMessage() {
        return customActionBarMessage;
    }

    public ConfigurationSection getLanguageConfig() {
        return languageConfig;
    }

    public void setLanguageConfig(ConfigurationSection languageConfig) {
        this.languageConfig = languageConfig;
    }
}
