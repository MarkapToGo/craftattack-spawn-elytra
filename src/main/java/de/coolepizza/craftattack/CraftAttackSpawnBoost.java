package de.coolepizza.craftattack;

import de.coolepizza.craftattack.command.SpawnElytraCommand;
import de.coolepizza.craftattack.config.PluginConfig;
import de.coolepizza.craftattack.listener.SpawnBoostListener;
import de.coolepizza.craftattack.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftAttackSpawnBoost extends JavaPlugin {

    private volatile PluginConfig pluginConfig;
    private volatile MessageService messageService;
    private volatile SpawnBoostListener boostListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pluginConfig = PluginConfig.load(getConfig(), getLogger());
        this.messageService = MessageService.create(this);

        String worldName = pluginConfig.getWorldName();
        World world = null;
        try {
            world = Bukkit.getWorld(worldName);
        } catch (Exception ignored) {
        }

        if (world == null) {
            getLogger().warning("Configured world '" + worldName + "' not currently loaded! It will be resolved dynamically when available.");
        }

        this.boostListener = new SpawnBoostListener(this, pluginConfig, messageService, world, true);
        getServer().getPluginManager().registerEvents(boostListener, this);

        SpawnElytraCommand command = new SpawnElytraCommand(this);
        PluginCommand pluginCommand = getCommand("spawnelytra");
        if (pluginCommand != null) {
            pluginCommand.setPermission(null);
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        if (boostListener != null) {
            boostListener.cleanup();
        }
        getServer().getScheduler().cancelTasks(this);
    }

    /**
     * Safely reloads plugin configuration and localization files synchronously.
     *
     * @return true if reloaded successfully, false if an error occurred
     */
    public boolean reloadPlugin() {
        try {
            reloadConfig();
            if (pluginConfig == null) {
                pluginConfig = new PluginConfig();
            }
            pluginConfig.reload(getConfig(), getLogger());

            if (messageService == null) {
                messageService = MessageService.create(this);
            } else {
                messageService.reload();
            }

            World world = null;
            try {
                world = Bukkit.getWorld(pluginConfig.getWorldName());
            } catch (Exception ignored) {
            }

            if (world == null) {
                getLogger().warning("World '" + pluginConfig.getWorldName() + "' not found upon reload. Retaining dynamic world resolution.");
            }

            if (boostListener != null) {
                boostListener.update(pluginConfig, messageService, world);
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("An error occurred while reloading CraftAttack Spawn Elytra: " + e.getMessage());
            return false;
        }
    }

    /**
     * Asynchronously reloads plugin configuration and localization files off the main thread,
     * then atomically applies the parsed state on the server main thread to eliminate tick freezes.
     *
     * @return CompletableFuture completing with true on success or false on failure
     */
    public java.util.concurrent.CompletableFuture<Boolean> reloadPluginAsync() {
        if (!isEnabled()) {
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Off-thread disk I/O and YAML parsing
                java.io.File configFile = new java.io.File(getDataFolder(), "config.yml");
                org.bukkit.configuration.file.YamlConfiguration yamlConfig = new org.bukkit.configuration.file.YamlConfiguration();
                if (configFile.exists()) {
                    yamlConfig.load(configFile);
                }
                PluginConfig newPluginConfig = PluginConfig.load(yamlConfig, getLogger());

                java.io.File langFile = new java.io.File(getDataFolder(), "language.yml");
                MessageService newMessageService = new MessageService(langFile, getLogger());

                return new ReloadResult(newPluginConfig, newMessageService, null);
            } catch (Exception e) {
                return new ReloadResult(null, null, e);
            }
        }).thenApplyAsync(result -> {
            // 2. Main-thread atomic state application
            if (!isEnabled()) {
                return false;
            }
            if (result.error != null) {
                getLogger().severe("An error occurred while asynchronously reloading configuration: " + result.error.getMessage());
                return false;
            }

            try {
                this.pluginConfig = result.config;
                this.messageService = result.messageService;

                World world = null;
                try {
                    world = Bukkit.getWorld(pluginConfig.getWorldName());
                } catch (Exception ignored) {
                }

                if (world == null) {
                    getLogger().warning("World '" + pluginConfig.getWorldName() + "' not found upon reload. Retaining dynamic world resolution.");
                }

                if (boostListener != null) {
                    boostListener.update(pluginConfig, messageService, world);
                }
                return true;
            } catch (Exception e) {
                getLogger().severe("Failed to apply reloaded configuration: " + e.getMessage());
                return false;
            }
        }, task -> {
            if (!isEnabled()) {
                return;
            }
            try {
                if (getServer() != null && getServer().getScheduler() != null) {
                    getServer().getScheduler().runTask(this, task);
                    return;
                }
            } catch (Exception ignored) {
            }
            task.run();
        });
    }

    private record ReloadResult(PluginConfig config, MessageService messageService, Exception error) {}

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public SpawnBoostListener getBoostListener() {
        return boostListener;
    }
}
