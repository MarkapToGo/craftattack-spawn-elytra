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

    private PluginConfig pluginConfig;
    private MessageService messageService;
    private SpawnBoostListener boostListener;

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
     * Safely reloads plugin configuration and localization files.
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
