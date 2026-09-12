package de.coolepizza.craftattack.listener;

import de.coolepizza.craftattack.ActivationMode;
import de.coolepizza.craftattack.config.PluginConfig;
import de.coolepizza.craftattack.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener and flight manager for CraftAttack Spawn Elytra.
 * <p>
 * Implements robust UUID-based tracking to avoid memory leaks, encapsulates
 * the ticker task cleanly without extending BukkitRunnable, provides distanceSquared
 * optimizations, and detects player landings safely.
 */
public class SpawnBoostListener implements Listener, Runnable {

    private final Plugin plugin;
    private PluginConfig pluginConfig;
    private MessageService messageService;
    private World world;

    private final Set<UUID> flying = new HashSet<>();
    private final Set<UUID> boosted = new HashSet<>();
    private final Map<UUID, Long> launchTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fallProtectionExpiries = new ConcurrentHashMap<>();

    private String message;
    private ActivationMode activationMode;
    private BukkitTask tickerTask;

    /**
     * Factory method creating and initializing a SpawnBoostListener from plugin configuration.
     * Safely reads configuration without destructively overwriting user files or throwing NPE on missing worlds.
     *
     * @param plugin the owning plugin
     * @return configured SpawnBoostListener
     */
    public static SpawnBoostListener create(Plugin plugin) {
        if (plugin != null) {
            plugin.saveDefaultConfig();
        }

        PluginConfig config = (plugin != null)
                ? PluginConfig.load(plugin.getConfig(), plugin.getLogger())
                : PluginConfig.defaults();
        MessageService messageService = MessageService.create(plugin);

        String worldName = config.getWorldName();
        World world = null;
        try {
            world = Bukkit.getWorld(worldName);
        } catch (Exception ignored) {
        }

        if (world == null && plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().warning("Configured world '" + worldName + "' not currently loaded. Will resolve dynamically.");
        }

        return new SpawnBoostListener(plugin, config, messageService, world, true);
    }

    /**
     * Modern constructor accepting configuration and message services.
     *
     * @param plugin         the owning plugin
     * @param config         the plugin configuration service
     * @param messageService the localization service
     * @param world          the world where spawn elytra is active
     * @param startTask      whether to start the repeating check task
     */
    public SpawnBoostListener(Plugin plugin, PluginConfig config, MessageService messageService, World world, boolean startTask) {
        this.plugin = plugin;
        this.pluginConfig = config != null ? config : PluginConfig.defaults();
        this.messageService = messageService != null ? messageService : MessageService.defaults();
        this.world = world;
        this.activationMode = this.pluginConfig.getActivationMode();
        this.message = this.pluginConfig.getLegacyMessage();

        if (startTask && this.plugin != null) {
            startTask();
        }
    }

    /**
     * Legacy constructor for backwards compatibility.
     */
    public SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean boostEnabled, World world, String message) {
        this(plugin, multiplyValue, spawnRadius, boostEnabled, world, message, ActivationMode.SWAP, true);
    }

    /**
     * Legacy constructor for backwards compatibility with ActivationMode.
     */
    public SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean boostEnabled, World world, String message, ActivationMode activationMode) {
        this(plugin, multiplyValue, spawnRadius, boostEnabled, world, message, activationMode, true);
    }

    /**
     * Legacy constructor for backwards compatibility with task control.
     */
    public SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean boostEnabled, World world, String message, ActivationMode activationMode, boolean startTask) {
        this.plugin = plugin;
        this.activationMode = activationMode != null ? activationMode : ActivationMode.SWAP;
        this.world = world;
        this.message = message;
        this.pluginConfig = new PluginConfig(
                this.activationMode,
                spawnRadius,
                multiplyValue,
                boostEnabled,
                world != null ? world.getName() : "world",
                message
        );
        this.messageService = (message != null)
                ? MessageService.withCustomActionBarMessage(message)
                : null;

        if (startTask && this.plugin != null) {
            startTask();
        }
    }

    /**
     * Starts the scheduled background ticker task safely.
     */
    public synchronized void startTask() {
        if (this.tickerTask == null && this.plugin != null) {
            try {
                this.tickerTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this, 0L, 3L);
            } catch (IllegalStateException | IllegalArgumentException | NullPointerException ignored) {
                // Allows running in mock/unit test environments without Bukkit scheduler
            }
        }
    }

    /**
     * Stops the scheduled background ticker task safely.
     */
    public synchronized void stopTask() {
        if (this.tickerTask != null) {
            try {
                this.tickerTask.cancel();
            } catch (Exception ignored) {
            }
            this.tickerTask = null;
        }
    }

    /**
     * Backwards-compatible alias for {@link #stopTask()} matching previous BukkitRunnable lifecycle.
     */
    public void cancel() {
        stopTask();
    }

    /**
     * Safely resets all flying players, cancels scheduled tasks, and clears tracking sets.
     * Called during plugin disable.
     */
    public synchronized void cleanup() {
        stopTask();

        for (UUID uuid : new HashSet<>(flying)) {
            cleanupPlayer(uuid);
        }

        flying.clear();
        boosted.clear();
        launchTimes.clear();
        fallProtectionExpiries.clear();
    }

    /**
     * Cleans up flight state for a specific player by UUID.
     *
     * @param uuid player unique ID
     */
    public void cleanupPlayer(UUID uuid) {
        if (uuid == null) return;
        flying.remove(uuid);
        boosted.remove(uuid);
        launchTimes.remove(uuid);
        fallProtectionExpiries.remove(uuid);

        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setGliding(false);
                if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                    player.setAllowFlight(isInSpawnRadius(player));
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Cleans up flight state for a specific player reference.
     *
     * @param player player to clean up
     */
    public void cleanupPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        flying.remove(uuid);
        boosted.remove(uuid);
        launchTimes.remove(uuid);
        fallProtectionExpiries.remove(uuid);

        if (player.isOnline()) {
            try {
                player.setGliding(false);
                if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                    player.setAllowFlight(isInSpawnRadius(player));
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Updates configuration and localization references dynamically during reload.
     */
    public void update(PluginConfig config, MessageService messageService, World world) {
        if (config != null) {
            this.pluginConfig = config;
            this.activationMode = config.getActivationMode();
            this.message = config.getLegacyMessage();
        }
        if (messageService != null) {
            this.messageService = messageService;
        }
        if (world != null) {
            this.world = world;
        } else if (config != null) {
            try {
                World resolved = Bukkit.getWorld(config.getWorldName());
                if (resolved != null) {
                    this.world = resolved;
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void run() {
        tick();
    }

    /**
     * Periodic tick checking player flight conditions, spawn boundaries, and landings.
     */
    public void tick() {
        World activeWorld = getWorld();
        if (activeWorld == null) return;

        long now = System.currentTimeMillis();
        fallProtectionExpiries.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (Player player : activeWorld.getPlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                continue;
            }

            UUID uuid = player.getUniqueId();
            boolean inSpawn = isInSpawnRadius(player);

            if (!flying.contains(uuid)) {
                player.setAllowFlight(inSpawn);
            } else {
                if (hasLanded(player)) {
                    handleLanding(player);
                }
            }
        }
    }

    /**
     * Detects if an active flying player has landed on the ground, entered liquid, or collided with solid blocks.
     * Prevents premature cancellation during initial takeoff.
     *
     * @param player player to check
     * @return true if player has landed, false if still airborne
     */
    public boolean hasLanded(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();

        // Prevent premature landing detection within 300ms of launch
        Long launchTime = launchTimes.get(uuid);
        if (launchTime != null && (System.currentTimeMillis() - launchTime < 300L)) {
            return false;
        }

        if (((Entity) player).isOnGround()) {
            return true;
        }

        if (player.isInWater() || player.isInLava()) {
            return true;
        }

        Location loc = player.getLocation();
        if (loc != null) {
            Block block = loc.getBlock();
            if (!block.isPassable() && !block.isLiquid()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles landing for a flying player: stops gliding, grants landing fall damage immunity,
     * and resets flight status.
     *
     * @param player landing player
     */
    public void handleLanding(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        flying.remove(uuid);
        boosted.remove(uuid);
        launchTimes.remove(uuid);
        // Grant 1000ms landing immunity against fall or wall impact damage
        fallProtectionExpiries.put(uuid, System.currentTimeMillis() + 1000L);

        player.setGliding(false);
        player.setAllowFlight(isInSpawnRadius(player));
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (!isInSpawnRadius(player)) return;

        event.setCancelled(true);
        player.setGliding(true);
        UUID uuid = player.getUniqueId();
        flying.add(uuid);
        launchTimes.put(uuid, System.currentTimeMillis());

        if (!isBoostEnabled()) return;
        player.sendActionBar(buildActionBarMessage());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER
                && (event.getCause() == EntityDamageEvent.DamageCause.FALL
                || event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL)) {
            UUID uuid = event.getEntity().getUniqueId();
            if (flying.contains(uuid) || isFallProtected(uuid)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        if (getActivationMode() != ActivationMode.SWAP) return;
        if (triggerBoost(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (getActivationMode() != ActivationMode.SNEAK) return;
        if (!event.isSneaking()) return;
        triggerBoost(event.getPlayer());
    }

    /**
     * Unified method to trigger boost for a player.
     * Respects all conditions:
     * - boostEnabled must be true
     * - player must be flying via plugin
     * - player hasn't already boosted on this flight
     *
     * @param player the player attempting to boost
     * @return true if boost was applied, false otherwise
     */
    public boolean triggerBoost(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        if (!isBoostEnabled() || !flying.contains(uuid) || boosted.contains(uuid)) {
            return false;
        }
        boosted.add(uuid);
        player.setVelocity(player.getLocation().getDirection().multiply(getMultiplier()));
        return true;
    }

    /**
     * Unified method to trigger boost for a player by UUID.
     *
     * @param uuid player unique ID
     * @return true if boost was applied, false otherwise
     */
    public boolean triggerBoost(UUID uuid) {
        if (uuid == null) return false;
        Player player = Bukkit.getPlayer(uuid);
        return player != null && triggerBoost(player);
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && flying.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (flying.contains(event.getPlayer().getUniqueId())) {
            cleanupPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (flying.contains(event.getPlayer().getUniqueId())) {
            cleanupPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode newMode = event.getNewGameMode();
        if (newMode != GameMode.SURVIVAL && newMode != GameMode.ADVENTURE) {
            if (flying.contains(event.getPlayer().getUniqueId())) {
                cleanupPlayer(event.getPlayer());
            }
        }
    }

    /**
     * Builds the action bar message component with the keybind for the current activation mode.
     *
     * @return Adventure Component to display in action bar
     */
    public Component buildActionBarMessage() {
        if (messageService != null) {
            return messageService.getBoostActionBar(getActivationMode());
        }
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return Component.text(message);
    }

    /**
     * Fast distance check using distanceSquared to avoid square root calculations.
     *
     * @param player player to evaluate
     * @return true if player is within configured spawn radius, false otherwise
     */
    public boolean isInSpawnRadius(Player player) {
        if (player == null) return false;
        World activeWorld = getWorld();
        if (activeWorld == null) return false;

        Location playerLoc = player.getLocation();
        if (playerLoc == null || playerLoc.getWorld() == null || !activeWorld.equals(playerLoc.getWorld())) {
            return false;
        }

        Location spawnLocation = activeWorld.getSpawnLocation();
        if (spawnLocation == null || spawnLocation.getWorld() == null || !activeWorld.equals(spawnLocation.getWorld())) {
            return false;
        }

        double radius = getSpawnRadius();
        return spawnLocation.distanceSquared(playerLoc) <= (radius * radius);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public PluginConfig getConfig() {
        return pluginConfig;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public int getMultiplyValue() {
        return pluginConfig != null ? pluginConfig.getMultiplyValue() : 5;
    }

    public double getMultiplier() {
        return pluginConfig != null ? pluginConfig.getMultiplier() : 5.0;
    }

    public int getSpawnRadius() {
        return pluginConfig != null ? pluginConfig.getSpawnRadius() : 50;
    }

    public boolean isBoostEnabled() {
        return pluginConfig != null && pluginConfig.isBoostEnabled();
    }

    /**
     * Returns the configured world, attempting safe dynamic resolution if initially unloaded.
     *
     * @return World instance, or null if world is not loaded
     */
    public World getWorld() {
        if (this.world != null) {
            return this.world;
        }
        String worldName = (pluginConfig != null) ? pluginConfig.getWorldName() : null;
        if (worldName != null) {
            try {
                this.world = Bukkit.getWorld(worldName);
            } catch (Exception ignored) {
            }
        }
        return this.world;
    }

    /**
     * Gets active flying players tracked by UUID.
     *
     * @return set of flying player UUIDs
     */
    public Set<UUID> getFlying() {
        return flying;
    }

    /**
     * Gets boosted players tracked by UUID.
     *
     * @return set of boosted player UUIDs
     */
    public Set<UUID> getBoosted() {
        return boosted;
    }

    public boolean isFlying(UUID uuid) {
        return uuid != null && flying.contains(uuid);
    }

    public boolean isFlying(Player player) {
        return player != null && isFlying(player.getUniqueId());
    }

    public boolean isBoosted(UUID uuid) {
        return uuid != null && boosted.contains(uuid);
    }

    public boolean isBoosted(Player player) {
        return player != null && isBoosted(player.getUniqueId());
    }

    public boolean isFallProtected(UUID uuid) {
        if (uuid == null) return false;
        Long expiry = fallProtectionExpiries.get(uuid);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public String getMessage() {
        return message;
    }

    public ActivationMode getActivationMode() {
        return activationMode != null ? activationMode : (pluginConfig != null ? pluginConfig.getActivationMode() : ActivationMode.SWAP);
    }
}