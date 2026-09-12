package de.coolepizza.craftattack.command;

import de.coolepizza.craftattack.CraftAttackSpawnBoost;
import de.coolepizza.craftattack.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Command handler for /spawnelytra (and aliases).
 * Provides the /spawnelytra reload command with permission checks and tab completion.
 */
public class SpawnElytraCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION_ADMIN = "craftattack.spawnelytra.admin";
    public static final String PERMISSION_ADMIN_ALIAS = "spawnelytra.admin";

    private final CraftAttackSpawnBoost plugin;
    private final Supplier<MessageService> messageServiceSupplier;
    private final BooleanSupplier reloadAction;

    public SpawnElytraCommand(CraftAttackSpawnBoost plugin) {
        this(plugin,
                () -> plugin != null ? plugin.getMessageService() : null,
                () -> plugin != null && plugin.reloadPlugin());
    }

    public SpawnElytraCommand(Supplier<MessageService> messageServiceSupplier, BooleanSupplier reloadAction) {
        this(null, messageServiceSupplier, reloadAction);
    }

    private SpawnElytraCommand(CraftAttackSpawnBoost plugin, Supplier<MessageService> messageServiceSupplier, BooleanSupplier reloadAction) {
        this.plugin = plugin;
        this.messageServiceSupplier = messageServiceSupplier;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messageService = messageServiceSupplier != null ? messageServiceSupplier.get() : null;

        if (!hasPermission(sender)) {
            if (messageService != null) {
                messageService.sendPrefixedMessage(sender, MessageService.KEY_NO_PERMISSION);
            } else {
                sender.sendMessage("§cDazu hast du keine Berechtigung.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            boolean success = reloadAction != null ? reloadAction.getAsBoolean() : (plugin != null && plugin.reloadPlugin());
            if (messageService != null) {
                if (success) {
                    messageService.sendPrefixedMessage(sender, MessageService.KEY_RELOAD_SUCCESS);
                } else {
                    sender.sendMessage("§cFehler beim Neuladen der Konfiguration.");
                }
            } else {
                sender.sendMessage(success ? "§aReloaded successfully." : "§cFailed to reload.");
            }
            return true;
        }

        if (messageService != null) {
            messageService.sendPrefixedMessage(sender, MessageService.KEY_COMMAND_USAGE);
        } else {
            sender.sendMessage("§eVerwendung: §f/" + label + " reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(PERMISSION_ADMIN) || sender.hasPermission(PERMISSION_ADMIN_ALIAS);
    }
}
