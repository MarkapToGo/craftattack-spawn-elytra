package de.coolepizza.craftattack.listener;

import de.coolepizza.craftattack.ActivationMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnBoostListenerTest {

    private World world;
    private Player player;
    private UUID playerUuid;
    private AtomicReference<Vector> playerVelocity;
    private final Vector direction = new Vector(1.0, 0.5, 0.0).normalize();
    private Location playerLocation;

    private AtomicBoolean playerGliding;
    private AtomicBoolean playerAllowFlight;
    private AtomicBoolean playerOnGround;
    private AtomicBoolean playerInWater;
    private AtomicBoolean playerInLava;
    private AtomicReference<GameMode> playerGameMode;
    private AtomicReference<Component> playerLastActionBar;
    private AtomicReference<List<Player>> worldPlayers;

    @BeforeEach
    void setUp() {
        worldPlayers = new AtomicReference<>(new ArrayList<>());
        playerGliding = new AtomicBoolean(false);
        playerAllowFlight = new AtomicBoolean(false);
        playerOnGround = new AtomicBoolean(false);
        playerInWater = new AtomicBoolean(false);
        playerInLava = new AtomicBoolean(false);
        playerGameMode = new AtomicReference<>(GameMode.SURVIVAL);
        playerLastActionBar = new AtomicReference<>(null);

        world = createWorld();
        playerVelocity = new AtomicReference<>(null);
        playerLocation = new Location(world, 0, 100, 0);
        playerLocation.setDirection(direction);
        playerUuid = UUID.randomUUID();
        player = createPlayer(playerLocation, playerVelocity, playerUuid);
        worldPlayers.get().add(player);
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
                    if (name.equals("getPlayers")) {
                        return worldPlayers != null ? worldPlayers.get() : List.of();
                    }
                    if (name.equals("getBlockAt")) {
                        return Proxy.newProxyInstance(
                                Block.class.getClassLoader(),
                                new Class<?>[]{Block.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("isPassable")) return true;
                                    if (m.getName().equals("isLiquid")) return false;
                                    return null;
                                }
                        );
                    }
                    if (name.equals("equals")) {
                        return proxy == args[0];
                    }
                    if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (name.equals("toString")) {
                        return "MockWorld@" + Integer.toHexString(System.identityHashCode(proxy));
                    }
                    return null;
                }
        );
    }

    private Player createPlayer(Location location, AtomicReference<Vector> velocityRef, UUID uuid) {
        return createPlayer(location, velocityRef, uuid, playerGliding, playerAllowFlight, playerOnGround, playerInWater, playerInLava, playerGameMode, playerLastActionBar);
    }

    private Player createPlayer(Location location, AtomicReference<Vector> velocityRef, UUID uuid,
                                AtomicBoolean gliding, AtomicBoolean allowFlight, AtomicBoolean onGround,
                                AtomicBoolean inWater, AtomicBoolean inLava, AtomicReference<GameMode> gameMode,
                                AtomicReference<Component> lastActionBar) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getLocation")) {
                        return location;
                    }
                    if (name.equals("getUniqueId")) {
                        return uuid;
                    }
                    if (name.equals("isOnline")) {
                        return true;
                    }
                    if (name.equals("getGameMode")) {
                        return gameMode != null ? gameMode.get() : GameMode.SURVIVAL;
                    }
                    if (name.equals("isOnGround")) {
                        return onGround != null && onGround.get();
                    }
                    if (name.equals("isInWater")) {
                        return inWater != null && inWater.get();
                    }
                    if (name.equals("isInLava")) {
                        return inLava != null && inLava.get();
                    }
                    if (name.equals("isGliding")) {
                        return gliding != null && gliding.get();
                    }
                    if (name.equals("setGliding")) {
                        if (gliding != null && args.length > 0 && args[0] instanceof Boolean b) {
                            gliding.set(b);
                        }
                        return null;
                    }
                    if (name.equals("getAllowFlight")) {
                        return allowFlight != null && allowFlight.get();
                    }
                    if (name.equals("setAllowFlight")) {
                        if (allowFlight != null && args.length > 0 && args[0] instanceof Boolean b) {
                            allowFlight.set(b);
                        }
                        return null;
                    }
                    if (name.equals("sendActionBar")) {
                        if (lastActionBar != null && args.length > 0 && args[0] instanceof Component c) {
                            lastActionBar.set(c);
                        }
                        return null;
                    }
                    if (name.equals("setVelocity")) {
                        if (velocityRef != null && args.length > 0 && args[0] instanceof Vector v) {
                            velocityRef.set(v.clone());
                        }
                        return null;
                    }
                    if (name.equals("getType")) {
                        return EntityType.PLAYER;
                    }
                    if (name.equals("getWorld")) {
                        return location != null ? location.getWorld() : null;
                    }
                    if (name.equals("equals")) {
                        return proxy == args[0];
                    }
                    if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (name.equals("toString")) {
                        return "MockPlayer@" + Integer.toHexString(System.identityHashCode(proxy));
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(double.class)) return 0.0;
                    if (method.getReturnType().equals(float.class)) return 0.0f;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    return null;
                }
        );
    }

    private Entity createNonPlayerEntity() {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getType")) return EntityType.ZOMBIE;
                    if (method.getName().equals("getUniqueId")) return UUID.randomUUID();
                    return null;
                }
        );
    }

    @SuppressWarnings({"deprecation", "removal"})
    private EntityDamageEvent createDamageEvent(Entity entity, EntityDamageEvent.DamageCause cause, double damage) {
        DamageSource damageSource = (DamageSource) Proxy.newProxyInstance(
                DamageSource.class.getClassLoader(),
                new Class<?>[]{DamageSource.class},
                (proxy, method, args) -> null
        );
        return new EntityDamageEvent(entity, cause, damageSource, damage);
    }

    private SpawnBoostListener createListener(ActivationMode mode, boolean boostEnabled) {
        return new SpawnBoostListener(
                null,
                5,
                50,
                boostEnabled,
                world,
                "Drücke %key% um dich zu Boosten.",
                mode,
                false
        );
    }

    @Test
    @DisplayName("Action bar message contains correct keybind for SWAP mode")
    void testActionBarMessageSwap() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        Component component = listener.buildActionBarMessage();

        boolean hasSwapKeybind = component.children().stream()
                .anyMatch(c -> c instanceof KeybindComponent kc && kc.keybind().equals("key.swapOffhand"));
        assertTrue(hasSwapKeybind, "Expected action bar message to contain key.swapOffhand");
    }

    @Test
    @DisplayName("Action bar message contains correct keybind for SNEAK mode")
    void testActionBarMessageSneak() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, true);
        Component component = listener.buildActionBarMessage();

        boolean hasSneakKeybind = component.children().stream()
                .anyMatch(c -> c instanceof KeybindComponent kc && kc.keybind().equals("key.sneak"));
        assertTrue(hasSneakKeybind, "Expected action bar message to contain key.sneak");
    }

    @Test
    @DisplayName("Action bar message handles message without %key% placeholder")
    void testActionBarMessageWithoutKey() {
        SpawnBoostListener listener = new SpawnBoostListener(
                null, 5, 50, true, world, "No keybind here", ActivationMode.SWAP, false
        );
        Component component = listener.buildActionBarMessage();
        assertEquals(Component.text("No keybind here"), component);
    }

    @Test
    @DisplayName("Action bar message handles null or empty message")
    void testActionBarMessageEmpty() {
        SpawnBoostListener listener = new SpawnBoostListener(
                null, 5, 50, true, world, "", ActivationMode.SWAP, false
        );
        assertEquals(Component.empty(), listener.buildActionBarMessage());

        SpawnBoostListener nullListener = new SpawnBoostListener(
                null, 5, 50, true, world, null, ActivationMode.SWAP, false
        );
        assertEquals(Component.empty(), nullListener.buildActionBarMessage());
    }

    @Test
    @DisplayName("triggerBoost successfully boosts flying player and applies velocity")
    void testTriggerBoostSuccess() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        boolean result = listener.triggerBoost(player);

        assertTrue(result);
        assertTrue(listener.getBoosted().contains(player.getUniqueId()));

        Vector appliedVelocity = playerVelocity.get();
        assertNotNull(appliedVelocity);

        Vector expectedVelocity = direction.clone().multiply(5);
        assertEquals(expectedVelocity.getX(), appliedVelocity.getX(), 0.001);
        assertEquals(expectedVelocity.getY(), appliedVelocity.getY(), 0.001);
        assertEquals(expectedVelocity.getZ(), appliedVelocity.getZ(), 0.001);
    }

    @Test
    @DisplayName("triggerBoost fails when player is not in flying set")
    void testTriggerBoostFailsIfNotFlying() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        boolean result = listener.triggerBoost(player);

        assertFalse(result);
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("triggerBoost fails when player has already boosted on this flight")
    void testTriggerBoostFailsIfAlreadyBoosted() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());
        listener.getBoosted().add(player.getUniqueId());

        boolean result = listener.triggerBoost(player);

        assertFalse(result);
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("triggerBoost fails when boost is disabled")
    void testTriggerBoostFailsIfBoostDisabled() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, false);
        listener.getFlying().add(player.getUniqueId());

        boolean result = listener.triggerBoost(player);

        assertFalse(result);
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("SWAP mode: onSwapItem triggers boost and cancels event")
    void testSwapModeOnSwapItemSuccess() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(player, null, null);

        listener.onSwapItem(event);

        assertTrue(event.isCancelled(), "Swap event should be cancelled when boost triggers");
        assertTrue(listener.getBoosted().contains(player.getUniqueId()));
        assertNotNull(playerVelocity.get());
    }

    @Test
    @DisplayName("SWAP mode: onSwapItem does not cancel event if player is not flying")
    void testSwapModeOnSwapItemNotFlying() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(player, null, null);

        listener.onSwapItem(event);

        assertFalse(event.isCancelled(), "Swap event should not be cancelled if player is not flying");
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("SNEAK mode: onSwapItem is ignored")
    void testSneakModeIgnoresSwapItem() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(player, null, null);

        listener.onSwapItem(event);

        assertFalse(event.isCancelled());
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("SNEAK mode: onToggleSneak triggers boost only on transition to sneak (isSneaking == true)")
    void testSneakModeOnToggleSneakTransition() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerToggleSneakEvent pressSneakEvent = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(pressSneakEvent);

        assertTrue(listener.getBoosted().contains(player.getUniqueId()));
        assertNotNull(playerVelocity.get());

        // Clear velocity ref to verify subsequent calls
        playerVelocity.set(null);

        // Releasing sneak should do nothing
        PlayerToggleSneakEvent releaseSneakEvent = new PlayerToggleSneakEvent(player, false);
        listener.onToggleSneak(releaseSneakEvent);
        assertNull(playerVelocity.get());

        // Sneaking again during the same flight should not trigger boost again
        PlayerToggleSneakEvent secondPressEvent = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(secondPressEvent);
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("SNEAK mode: releasing sneak (isSneaking == false) does not trigger boost")
    void testSneakModeReleaseDoesNotTrigger() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerToggleSneakEvent releaseSneakEvent = new PlayerToggleSneakEvent(player, false);
        listener.onToggleSneak(releaseSneakEvent);

        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("SWAP mode: onToggleSneak is ignored")
    void testSwapModeIgnoresSneak() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerToggleSneakEvent pressSneakEvent = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(pressSneakEvent);

        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("EntityToggleGlideEvent is cancelled when player is in flying list")
    void testGlideToggleCancellation() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        EntityToggleGlideEvent glideEvent = new EntityToggleGlideEvent(player, false);
        listener.onToggleGlide(glideEvent);
        assertTrue(glideEvent.isCancelled(), "Glide cancellation should be prevented while in flying set");

        Player nonFlyingPlayer = createPlayer(new Location(world, 0, 100, 0), null, UUID.randomUUID());
        EntityToggleGlideEvent normalGlideEvent = new EntityToggleGlideEvent(nonFlyingPlayer, false);
        listener.onToggleGlide(normalGlideEvent);
        assertFalse(normalGlideEvent.isCancelled(), "Glide toggle should not be cancelled for non-flying player");
    }

    @Test
    @DisplayName("PlayerQuitEvent cleans up flying and boosted state")
    void testQuitEventCleansUpPlayer() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());
        listener.getBoosted().add(player.getUniqueId());

        PlayerQuitEvent event = new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED);
        listener.onQuit(event);

        assertFalse(listener.getFlying().contains(player.getUniqueId()));
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
    }

    @Test
    @DisplayName("PlayerTeleportEvent cleans up flying state")
    void testTeleportEventCleansUpPlayer() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());
        listener.getBoosted().add(player.getUniqueId());

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, playerLocation, playerLocation);
        listener.onTeleport(event);

        assertFalse(listener.getFlying().contains(player.getUniqueId()));
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
    }

    @Test
    @DisplayName("PlayerChangedWorldEvent cleans up flying state")
    void testWorldChangeEventCleansUpPlayer() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        listener.onWorldChange(event);

        assertFalse(listener.getFlying().contains(player.getUniqueId()));
    }

    @Test
    @DisplayName("GameMode change to creative cleans up flying state")
    void testGameModeChangeCleansUpPlayer() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(player, GameMode.CREATIVE, PlayerGameModeChangeEvent.Cause.COMMAND, Component.empty());
        listener.onGameModeChange(event);

        assertFalse(listener.getFlying().contains(player.getUniqueId()));
    }

    @Test
    @DisplayName("isInSpawnRadius accurately uses distanceSquared for boundary checks")
    void testDistanceSquaredCalculations() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        // Spawn is at (0, 64, 0), radius is 50
        // Player at (0, 100, 0): distance = 36 <= 50 (inside)
        assertTrue(listener.isInSpawnRadius(player));

        // Player at (0, 115, 0): distance = 51 > 50 (outside)
        Player outsidePlayer = createPlayer(new Location(world, 0, 115, 0), null, UUID.randomUUID());
        assertFalse(listener.isInSpawnRadius(outsidePlayer));

        // Null player or null world safely returns false
        assertFalse(listener.isInSpawnRadius(null));
    }

    @Test
    @DisplayName("Landing handling removes flight state and provides fall damage immunity")
    void testLandingHandlingAndFallDamageImmunity() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());
        listener.getBoosted().add(player.getUniqueId());

        listener.handleLanding(player);

        assertFalse(listener.getFlying().contains(player.getUniqueId()));
        assertFalse(listener.getBoosted().contains(player.getUniqueId()));
        assertTrue(listener.isFallProtected(player.getUniqueId()));
        assertFalse(listener.isFallProtected(UUID.randomUUID()));
    }

    @Test
    @DisplayName("cleanup() clears all tracking sets and stops tasks cleanly")
    void testCleanupOnDisable() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());
        listener.getBoosted().add(player.getUniqueId());

        listener.cleanup();

        assertTrue(listener.getFlying().isEmpty());
        assertTrue(listener.getBoosted().isEmpty());
        assertFalse(listener.isFallProtected(player.getUniqueId()));
    }

    @Test
    @DisplayName("Safe handling when world is null initially")
    void testSafeWorldNullHandling() {
        SpawnBoostListener listener = new SpawnBoostListener(
                null, 5, 50, true, null, "Msg", ActivationMode.SWAP, false
        );

        assertNull(listener.getWorld());
        assertFalse(listener.isInSpawnRadius(player));
        assertDoesNotThrow(listener::run);
        assertDoesNotThrow(listener::cleanup);
    }

    @Test
    @DisplayName("Double-jump inside spawn radius in survival mode initiates flight, sets gliding, and sends action bar")
    void testDoubleJumpInsideSpawnInitiatesFlight() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(player, true);

        listener.onDoubleJump(event);

        assertTrue(event.isCancelled(), "Flight toggle event should be cancelled to take over flight control");
        assertTrue(playerGliding.get(), "Player should be set to gliding");
        assertTrue(listener.isFlying(player), "Player should be added to flying set");
        assertNotNull(playerLastActionBar.get(), "Action bar boost hint should be sent to player");
    }

    @Test
    @DisplayName("Double-jump outside spawn radius does not trigger flight takeover")
    void testDoubleJumpOutsideSpawnIgnored() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        Location outsideLoc = new Location(world, 0, 150, 0);
        outsideLoc.setDirection(direction);
        AtomicReference<Vector> outsideVel = new AtomicReference<>(null);
        Player outsidePlayer = createPlayer(outsideLoc, outsideVel, UUID.randomUUID());

        PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(outsidePlayer, true);
        listener.onDoubleJump(event);

        assertFalse(event.isCancelled());
        assertFalse(listener.isFlying(outsidePlayer));
    }

    @Test
    @DisplayName("Double-jump in creative mode is ignored by spawn boost listener")
    void testDoubleJumpInCreativeModeIgnored() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        playerGameMode.set(GameMode.CREATIVE);

        PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(player, true);
        listener.onDoubleJump(event);

        assertFalse(event.isCancelled());
        assertFalse(listener.isFlying(player));
    }

    @Test
    @DisplayName("Double-jump with boost disabled initiates flight but omits action bar message")
    void testDoubleJumpWithBoostDisabled() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, false);

        PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(player, true);
        listener.onDoubleJump(event);

        assertTrue(event.isCancelled());
        assertTrue(listener.isFlying(player));
        assertTrue(playerGliding.get());
        assertNull(playerLastActionBar.get(), "Action bar should not be sent when boost is disabled");
    }

    @Test
    @DisplayName("Activation logic does not repeatedly trigger boost from a held sneak state")
    void testHeldSneakStateDoesNotRepeatedlyBoost() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, true);

        // 1. Initiate flight via double jump
        PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, true);
        listener.onDoubleJump(flightEvent);
        assertTrue(listener.isFlying(player));

        // 2. Player enters sneak state (sneak pressed) -> triggers initial boost
        PlayerToggleSneakEvent initialSneak = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(initialSneak);

        assertTrue(listener.isBoosted(player));
        assertNotNull(playerVelocity.get(), "Initial sneak should apply velocity boost");

        // Clear velocity ref to test subsequent states
        playerVelocity.set(null);

        // 3. While player remains sneaking (held sneak state), subsequent sneak events do NOT re-boost
        PlayerToggleSneakEvent heldSneak1 = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(heldSneak1);
        assertNull(playerVelocity.get(), "Held sneak event must not trigger secondary boost");

        PlayerToggleSneakEvent heldSneak2 = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(heldSneak2);
        assertNull(playerVelocity.get(), "Subsequent held sneak event must not trigger boost");

        // 4. While sneak is held, periodic ticks do NOT re-boost or alter boosted state
        listener.tick();
        assertNull(playerVelocity.get(), "Scheduled tick during held sneak must not re-trigger boost");
        assertTrue(listener.isFlying(player), "Player should remain flying during held sneak");
        assertTrue(listener.isBoosted(player), "Player should remain flagged as boosted");

        // 5. Swap item events in SNEAK mode do NOT trigger boost even while sneaking
        PlayerSwapHandItemsEvent swapEvent = new PlayerSwapHandItemsEvent(player, null, null);
        listener.onSwapItem(swapEvent);
        assertFalse(swapEvent.isCancelled());
        assertNull(playerVelocity.get());

        // 6. Releasing sneak does NOT trigger boost
        PlayerToggleSneakEvent releaseSneak = new PlayerToggleSneakEvent(player, false);
        listener.onToggleSneak(releaseSneak);
        assertNull(playerVelocity.get());

        // 7. Re-pressing sneak during the same flight does NOT trigger boost again (one boost per flight)
        PlayerToggleSneakEvent secondPress = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(secondPress);
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("Sneak toggle when not flying does not trigger boost")
    void testSneakWhenNotFlyingDoesNotBoost() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, true);

        PlayerToggleSneakEvent sneakEvent = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(sneakEvent);

        assertFalse(listener.isBoosted(player));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("Sneak toggle when boost is disabled does not trigger boost")
    void testSneakWhenBoostDisabledDoesNotBoost() {
        SpawnBoostListener listener = createListener(ActivationMode.SNEAK, false);
        listener.getFlying().add(player.getUniqueId());

        PlayerToggleSneakEvent sneakEvent = new PlayerToggleSneakEvent(player, true);
        listener.onToggleSneak(sneakEvent);

        assertFalse(listener.isBoosted(player));
        assertNull(playerVelocity.get());
    }

    @Test
    @DisplayName("Fall and Fly-Into-Wall damage is cancelled while player is flying")
    void testDamageCancellationWhileFlying() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        EntityDamageEvent fallDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.FALL, 15.0);
        listener.onDamage(fallDamage);
        assertTrue(fallDamage.isCancelled(), "Fall damage should be cancelled while flying");

        EntityDamageEvent wallDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.FLY_INTO_WALL, 10.0);
        listener.onDamage(wallDamage);
        assertTrue(wallDamage.isCancelled(), "Fly-into-wall damage should be cancelled while flying");
    }

    @Test
    @DisplayName("Fall and Fly-Into-Wall damage is cancelled during post-landing fall protection window")
    void testDamageCancellationWhileFallProtected() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        // Player lands
        listener.handleLanding(player);
        assertFalse(listener.isFlying(player));
        assertTrue(listener.isFallProtected(player.getUniqueId()));

        EntityDamageEvent fallDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.FALL, 15.0);
        listener.onDamage(fallDamage);
        assertTrue(fallDamage.isCancelled(), "Fall damage should be cancelled during landing immunity window");

        EntityDamageEvent wallDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.FLY_INTO_WALL, 10.0);
        listener.onDamage(wallDamage);
        assertTrue(wallDamage.isCancelled(), "Fly-into-wall damage should be cancelled during landing immunity window");
    }

    @Test
    @DisplayName("Non-flight damage causes are not cancelled even while flying")
    void testNonFlightDamageCausesNotCancelled() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        EntityDamageEvent lavaDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.LAVA, 5.0);
        listener.onDamage(lavaDamage);
        assertFalse(lavaDamage.isCancelled(), "Lava damage must not be cancelled");

        EntityDamageEvent fireDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.FIRE, 2.0);
        listener.onDamage(fireDamage);
        assertFalse(fireDamage.isCancelled(), "Fire damage must not be cancelled");

        EntityDamageEvent voidDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.VOID, 4.0);
        listener.onDamage(voidDamage);
        assertFalse(voidDamage.isCancelled(), "Void damage must not be cancelled");

        EntityDamageEvent attackDamage = createDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 8.0);
        listener.onDamage(attackDamage);
        assertFalse(attackDamage.isCancelled(), "Entity attack damage must not be cancelled");
    }

    @Test
    @DisplayName("Damage is not cancelled for unprotected player or non-player entities")
    void testDamageNotCancelledForUnprotectedOrNonPlayer() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        // Player not flying, not protected
        EntityDamageEvent normalFall = createDamageEvent(player, EntityDamageEvent.DamageCause.FALL, 10.0);
        listener.onDamage(normalFall);
        assertFalse(normalFall.isCancelled());

        // Non-player entity
        Entity zombie = createNonPlayerEntity();
        EntityDamageEvent zombieFall = createDamageEvent(zombie, EntityDamageEvent.DamageCause.FALL, 10.0);
        listener.onDamage(zombieFall);
        assertFalse(zombieFall.isCancelled());
    }

    @Test
    @DisplayName("Takeoff grace period prevents premature landing detection within 300ms of launch")
    void testTakeoffGracePeriodPreventsPrematureLanding() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        // Launch player now
        PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, true);
        listener.onDoubleJump(flightEvent);

        // Player is on ground immediately after double-jump (takeoff phase)
        playerOnGround.set(true);

        assertFalse(listener.hasLanded(player), "Takeoff grace period should prevent landing within 300ms of launch");
    }

    @Test
    @DisplayName("Landing is detected on ground, in water, or in lava after takeoff grace period")
    void testLandingDetectedAfterGracePeriod() throws InterruptedException {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        // Launch player
        PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, true);
        listener.onDoubleJump(flightEvent);

        // Wait past the 300ms takeoff grace window
        Thread.sleep(350L);

        // 1. On ground
        playerOnGround.set(true);
        assertTrue(listener.hasLanded(player), "Player on ground after grace period should be considered landed");

        // 2. In water
        playerOnGround.set(false);
        playerInWater.set(true);
        assertTrue(listener.hasLanded(player), "Player in water after grace period should be considered landed");

        // 3. In lava
        playerInWater.set(false);
        playerInLava.set(true);
        assertTrue(listener.hasLanded(player), "Player in lava after grace period should be considered landed");

        // 4. Airborne
        playerInLava.set(false);
        assertFalse(listener.hasLanded(player), "Airborne player should not be considered landed");

        // 5. Null player safe check
        assertFalse(listener.hasLanded(null));
    }

    @Test
    @DisplayName("tick() manages allowFlight for non-flying players and processes landings for airborne players")
    void testTickLifecycle() throws InterruptedException {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);

        // 1. Non-flying player inside spawn radius gets allowFlight=true
        playerAllowFlight.set(false);
        listener.tick();
        assertTrue(playerAllowFlight.get(), "Non-flying player inside spawn radius should have allowFlight enabled");

        // 2. Player initiates flight
        PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, true);
        listener.onDoubleJump(flightEvent);
        assertTrue(listener.isFlying(player));

        // 3. Wait past takeoff grace period
        Thread.sleep(350L);

        // 4. Player touches ground and tick runs
        playerOnGround.set(true);
        listener.tick();

        assertFalse(listener.isFlying(player), "Landed player should be removed from flying set by tick");
        assertFalse(playerGliding.get(), "Gliding should be disabled upon landing");
        assertTrue(listener.isFallProtected(player.getUniqueId()), "Landed player should receive landing fall protection");
    }

    @Test
    @DisplayName("PlayerRespawnEvent cleans up flying and boosted state")
    void testRespawnCleansUpPlayer() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());
        listener.getBoosted().add(player.getUniqueId());

        PlayerRespawnEvent event = new PlayerRespawnEvent(player, playerLocation, false, false, false, PlayerRespawnEvent.RespawnReason.DEATH);
        listener.onRespawn(event);

        assertFalse(listener.isFlying(player));
        assertFalse(listener.isBoosted(player));
    }

    @Test
    @DisplayName("GameMode change to adventure retains flight state, change to spectator cleans up")
    void testGameModeChanges() {
        SpawnBoostListener listener = createListener(ActivationMode.SWAP, true);
        listener.getFlying().add(player.getUniqueId());

        // Change to Adventure mode should retain flight state
        PlayerGameModeChangeEvent toAdventure = new PlayerGameModeChangeEvent(player, GameMode.ADVENTURE, PlayerGameModeChangeEvent.Cause.COMMAND, Component.empty());
        listener.onGameModeChange(toAdventure);
        assertTrue(listener.isFlying(player));

        // Change to Spectator mode should clean up flight state
        PlayerGameModeChangeEvent toSpectator = new PlayerGameModeChangeEvent(player, GameMode.SPECTATOR, PlayerGameModeChangeEvent.Cause.COMMAND, Component.empty());
        listener.onGameModeChange(toSpectator);
        assertFalse(listener.isFlying(player));
    }
}
