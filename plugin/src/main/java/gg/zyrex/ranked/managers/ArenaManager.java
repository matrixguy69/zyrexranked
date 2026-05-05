package gg.zyrex.ranked.managers;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ─────────────────────────────────────────────────────────────────────────────
// ArenaManager
// Handles arena claiming, releasing, kit giving, and spawn points.
// Arenas are configured via config or a dedicated arenas.yml you can expand.
// ─────────────────────────────────────────────────────────────────────────────
public class ArenaManager {

    private final ZyrexRanked plugin;
    // ArenaName → Gamemode (null = free)
    private final Map<String, Gamemode> claimedArenas = new ConcurrentHashMap<>();

    // For a real server, load these from arenas.yml
    // Key: "smp_1", "pot_1", etc.  Value: spawn1/spawn2 Location
    private final Map<String, Location[]> arenaSpawns = new HashMap<>();

    public ArenaManager(ZyrexRanked plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    private void loadArenas() {
        // TODO: Load arena spawn points from arenas.yml
        // Example structure in arenas.yml:
        // arenas:
        //   smp_1:
        //     spawn1: {world: ranked_smp, x: 0, y: 64, z: 10}
        //     spawn2: {world: ranked_smp, x: 0, y: 64, z: -10}
        //
        // For now, arenas return null spawns — add real coordinates when you
        // set up your arena worlds.
        plugin.getLogger().info("ArenaManager loaded. Add arena spawns to arenas.yml!");
    }

    /**
     * Claim the next available arena for a gamemode.
     * Returns the arena name (e.g. "smp_1").
     */
    public String claimArena(Gamemode gamemode) {
        // Simple round-robin: find first unclaimed arena for this gamemode
        String prefix = gamemode.getId() + "_";
        for (int i = 1; i <= 20; i++) {
            String name = prefix + i;
            if (!claimedArenas.containsKey(name)) {
                claimedArenas.put(name, gamemode);
                return name;
            }
        }
        // All arenas busy — create a dynamic overflow arena name
        String overflow = prefix + UUID.randomUUID().toString().substring(0, 4);
        claimedArenas.put(overflow, gamemode);
        return overflow;
    }

    public void releaseArena(String arenaName, Gamemode gamemode) {
        claimedArenas.remove(arenaName);
        // TODO: Reset arena blocks if needed
    }

    /** Get spawn 1 for this arena. Returns null if not configured. */
    public Location getSpawn1(String arenaName, Gamemode gamemode) {
        Location[] spawns = arenaSpawns.get(arenaName);
        return spawns != null ? spawns[0] : null;
    }

    /** Get spawn 2 for this arena. Returns null if not configured. */
    public Location getSpawn2(String arenaName, Gamemode gamemode) {
        Location[] spawns = arenaSpawns.get(arenaName);
        return spawns != null ? spawns[1] : null;
    }

    /**
     * Give the correct kit for each gamemode.
     * Customise items to match your server's meta.
     */
    public void giveKit(Player player, Gamemode gamemode) {
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);

        switch (gamemode) {
            case SMP -> giveSmpKit(player);
            case POT -> givePotKit(player);
            case UHC -> giveUhcKit(player);
            case SWORD -> giveSwordKit(player);
            case SPEARMACE -> giveSpearMaceKit(player);
        }
    }

    private void giveSmpKit(Player p) {
        p.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        p.getInventory().setItem(0, new ItemStack(Material.NETHERITE_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.TOTEM_OF_UNDYING));
        p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 4));
        // Add crystals, obsidian, etc. as your SMP meta requires
    }

    private void givePotKit(Player p) {
        p.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        p.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.SPLASH_POTION, 16));
        p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 8));
    }

    private void giveUhcKit(Player p) {
        p.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        p.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.BOW));
        p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 3));
        p.getInventory().setItem(9, new ItemStack(Material.ARROW, 64));
    }

    private void giveSwordKit(Player p) {
        p.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        p.getInventory().setItem(0, new ItemStack(Material.NETHERITE_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.NETHERITE_AXE));
        p.getInventory().setItem(2, new ItemStack(Material.MACE));
        p.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 6));
    }

    private void giveSpearMaceKit(Player p) {
        p.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        p.getInventory().setItem(0, new ItemStack(Material.MACE));
        p.getInventory().setItem(1, new ItemStack(Material.TRIDENT));
        p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 4));
    }

    public int getActiveArenaCount() { return claimedArenas.size(); }
}
