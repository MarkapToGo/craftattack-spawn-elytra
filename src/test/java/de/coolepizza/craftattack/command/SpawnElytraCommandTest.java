package de.coolepizza.craftattack.command;

import de.coolepizza.craftattack.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SpawnElytraCommandTest {

    private MessageService messageService;
    private Command dummyCommand;

    @BeforeEach
    void setUp() {
        messageService = MessageService.defaults();
        dummyCommand = new Command("spawnelytra") {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return false;
            }
        };
    }

    private TestSender createSender(String... permissions) {
        return new TestSender(permissions);
    }

    @Test
    @DisplayName("Reload succeeds when sender has craftattack.spawnelytra.admin permission")
    void testReloadSuccessWithAdminPermission() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> {
            reloaded.set(true);
            return true;
        });

        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);
        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});

        assertTrue(result);
        assertTrue(reloaded.get(), "Expected reload action to be invoked");
        assertTrue(sender.hasReceivedMessageContaining("erfolgreich neu geladen"));
    }

    @Test
    @DisplayName("Reload succeeds when sender has spawnelytra.admin alias permission")
    void testReloadSuccessWithAliasPermission() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> {
            reloaded.set(true);
            return true;
        });

        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN_ALIAS);
        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});

        assertTrue(result);
        assertTrue(reloaded.get());
        assertTrue(sender.hasReceivedMessageContaining("erfolgreich neu geladen"));
    }

    @Test
    @DisplayName("Reload failure sends error message to sender")
    void testReloadFailure() {
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> false);

        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);
        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});

        assertTrue(result);
        assertTrue(sender.hasReceivedMessageContaining("Fehler"));
    }

    @Test
    @DisplayName("Sender without permission receives no-permission message and reload is not invoked")
    void testNoPermission() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> {
            reloaded.set(true);
            return true;
        });

        TestSender sender = createSender(); // no permissions
        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});

        assertTrue(result);
        assertFalse(reloaded.get(), "Reload should NOT be called without permission");
        assertTrue(sender.hasReceivedMessageContaining("keine Berechtigung"));
    }

    @Test
    @DisplayName("Sender with permission receives command usage on unknown subcommands")
    void testUnknownSubcommandUsage() {
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> true);

        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);
        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"unknown"});

        assertTrue(result);
        assertTrue(sender.hasReceivedMessageContaining("Verwendung"));
    }

    @Test
    @DisplayName("Tab completion returns 'reload' when prefix matches and sender has permission")
    void testTabCompletionWithPermission() {
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> true);
        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);

        List<String> completions = command.onTabComplete(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{""});
        assertEquals(List.of("reload"), completions);

        List<String> prefixCompletions = command.onTabComplete(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"rel"});
        assertEquals(List.of("reload"), prefixCompletions);

        List<String> mismatchCompletions = command.onTabComplete(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"xyz"});
        assertTrue(mismatchCompletions.isEmpty());
    }

    @Test
    @DisplayName("Tab completion returns empty list when sender lacks permission")
    void testTabCompletionWithoutPermission() {
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> true);
        TestSender sender = createSender(); // no permissions

        List<String> completions = command.onTabComplete(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{""});
        assertTrue(completions.isEmpty());
    }

    @Test
    @DisplayName("Tab completion returns 'reload' with alias permission and empty on second argument")
    void testTabCompletionWithAliasPermissionAndExtraArgs() {
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> true);
        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN_ALIAS);

        List<String> completions = command.onTabComplete(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{""});
        assertEquals(List.of("reload"), completions);

        List<String> secondArgCompletions = command.onTabComplete(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload", ""});
        assertTrue(secondArgCompletions.isEmpty());
    }

    @Test
    @DisplayName("No arguments provided shows command usage")
    void testNoArgumentsShowsUsage() {
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> true);
        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);

        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[0]);
        assertTrue(result);
        assertTrue(sender.hasReceivedMessageContaining("Verwendung"));
    }

    @Test
    @DisplayName("Extra arguments after reload shows usage and does not trigger reload")
    void testExtraArgumentsShowsUsage() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> {
            reloaded.set(true);
            return true;
        });
        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);

        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload", "extra"});
        assertTrue(result);
        assertFalse(reloaded.get(), "Reload should not execute with extra arguments");
        assertTrue(sender.hasReceivedMessageContaining("Verwendung"));
    }

    @Test
    @DisplayName("Reload is case-insensitive")
    void testCaseInsensitiveReload() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        SpawnElytraCommand command = new SpawnElytraCommand(() -> messageService, () -> {
            reloaded.set(true);
            return true;
        });
        TestSender sender = createSender(SpawnElytraCommand.PERMISSION_ADMIN);

        boolean result = command.onCommand(sender.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"RELOAD"});
        assertTrue(result);
        assertTrue(reloaded.get());
        assertTrue(sender.hasReceivedMessageContaining("erfolgreich"));
    }

    @Test
    @DisplayName("Null MessageService triggers legacy fallback messages without error")
    void testNullMessageServiceFallback() {
        // 1. No permission
        SpawnElytraCommand command1 = new SpawnElytraCommand(() -> null, () -> true);
        TestSender senderNoPerm = createSender();
        command1.onCommand(senderNoPerm.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});
        assertTrue(senderNoPerm.hasReceivedMessageContaining("keine Berechtigung"));

        // 2. Reload success
        SpawnElytraCommand command2 = new SpawnElytraCommand(() -> null, () -> true);
        TestSender senderSuccess = createSender(SpawnElytraCommand.PERMISSION_ADMIN);
        command2.onCommand(senderSuccess.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});
        assertTrue(senderSuccess.hasReceivedMessageContaining("Reloaded successfully"));

        // 3. Reload failure
        SpawnElytraCommand command3 = new SpawnElytraCommand(() -> null, () -> false);
        TestSender senderFailure = createSender(SpawnElytraCommand.PERMISSION_ADMIN);
        command3.onCommand(senderFailure.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"reload"});
        assertTrue(senderFailure.hasReceivedMessageContaining("Failed to reload"));

        // 4. Usage
        SpawnElytraCommand command4 = new SpawnElytraCommand(() -> null, () -> true);
        TestSender senderUsage = createSender(SpawnElytraCommand.PERMISSION_ADMIN);
        command4.onCommand(senderUsage.asCommandSender(), dummyCommand, "spawnelytra", new String[]{"something_else"});
        assertTrue(senderUsage.hasReceivedMessageContaining("Verwendung"));
    }

    private static class TestSender {
        private final Set<String> permissions = new HashSet<>();
        private final List<String> receivedMessages = new ArrayList<>();

        public TestSender(String... perms) {
            for (String p : perms) {
                permissions.add(p.toLowerCase());
            }
        }

        public CommandSender asCommandSender() {
            return (CommandSender) Proxy.newProxyInstance(
                    CommandSender.class.getClassLoader(),
                    new Class<?>[]{CommandSender.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.equals("hasPermission") && args.length == 1 && args[0] instanceof String perm) {
                            return permissions.contains(perm.toLowerCase());
                        }
                        if (name.equals("sendMessage")) {
                            if (args.length > 0 && args[0] != null) {
                                if (args[0] instanceof Component c) {
                                    receivedMessages.add(c.toString());
                                } else {
                                    receivedMessages.add(args[0].toString());
                                }
                            }
                            return null;
                        }
                        return null;
                    }
            );
        }

        public boolean hasReceivedMessageContaining(String text) {
            return receivedMessages.stream().anyMatch(m -> m.contains(text));
        }
    }
}
