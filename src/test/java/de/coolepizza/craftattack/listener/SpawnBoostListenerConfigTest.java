package de.coolepizza.craftattack.listener;

import de.coolepizza.craftattack.ActivationMode;
import de.coolepizza.craftattack.config.PluginConfig;
import de.coolepizza.craftattack.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class SpawnBoostListenerConfigTest {

    private World world;

    @BeforeEach
    void setUp() {
        world = createWorld();
    }

    private World createWorld() {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getSpawnLocation")) {
                        return new Location((World) proxy, 0, 64, 0);
                    }
                    if (name.equals("getName")) {
                        return "world";
                    }
                    if (name.equals("equals")) {
                        return proxy == args[0];
                    }
                    if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    return null;
                }
        );
    }

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
    @DisplayName("Modern constructor initializes listener from PluginConfig and MessageService")
    void testModernConstructor() {
        PluginConfig config = new PluginConfig(
                ActivationMode.SNEAK,
                65,
                7.5,
                true,
                "world",
                null
        );
        MessageService messageService = MessageService.defaults();

        SpawnBoostListener listener = new SpawnBoostListener(null, config, messageService, world, false);

        assertEquals(ActivationMode.SNEAK, listener.getActivationMode());
        assertEquals(65, listener.getSpawnRadius());
        assertEquals(7.5, listener.getMultiplier(), 0.001);
        assertEquals(7, listener.getMultiplyValue());
        assertTrue(listener.isBoostEnabled());
        assertEquals(world, listener.getWorld());

        Component actionBar = listener.buildActionBarMessage();
        assertTrue(containsKeybind(actionBar, "key.sneak"));
    }

    @Test
    @DisplayName("Dynamic update changes listener configuration without recreating instance")
    void testDynamicConfigUpdate() {
        PluginConfig initialConfig = new PluginConfig(
                ActivationMode.SWAP,
                50,
                5.0,
                true,
                "world",
                null
        );
        MessageService initialMessages = MessageService.defaults();
        SpawnBoostListener listener = new SpawnBoostListener(null, initialConfig, initialMessages, world, false);

        assertEquals(ActivationMode.SWAP, listener.getActivationMode());
        assertEquals(50, listener.getSpawnRadius());
        assertEquals(5.0, listener.getMultiplier(), 0.001);
        assertTrue(containsKeybind(listener.buildActionBarMessage(), "key.swapOffhand"));

        // Now reload/update with new configuration
        PluginConfig updatedConfig = new PluginConfig(
                ActivationMode.SNEAK,
                100,
                9.5,
                false,
                "world",
                null
        );
        MessageService updatedMessages = MessageService.withCustomActionBarMessage("Sneak now: <key>");
        listener.update(updatedConfig, updatedMessages, world);

        assertEquals(ActivationMode.SNEAK, listener.getActivationMode());
        assertEquals(100, listener.getSpawnRadius());
        assertEquals(9.5, listener.getMultiplier(), 0.001);
        assertFalse(listener.isBoostEnabled());
        assertTrue(containsKeybind(listener.buildActionBarMessage(), "key.sneak"));
    }
}
