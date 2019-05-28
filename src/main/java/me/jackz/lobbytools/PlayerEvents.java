package me.jackz.lobbytools;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.jackz.lobbytools.lib.ParkourRegion;
import me.jackz.lobbytools.lib.ParkourRegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class PlayerEvents implements Listener {
    private Main plugin;
    private final static Material[] PRESSURE_PLATES = {
            Material.STONE_PRESSURE_PLATE,
            Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Material.OAK_PRESSURE_PLATE,
            Material.DARK_OAK_PRESSURE_PLATE,
            Material.BIRCH_PRESSURE_PLATE,
            Material.SPRUCE_PRESSURE_PLATE,
            Material.ACACIA_PRESSURE_PLATE,
            Material.JUNGLE_PRESSURE_PLATE
    };
    private static ParkourRegionManager parkourRegionManager;
    private final Inventory GADGETS_MENU = Bukkit.createInventory(null,54,"§9Gadgets");
    private int y_teleport = 0;
    PlayerEvents(Main plugin) {
        this.plugin = plugin;
        y_teleport = plugin.getConfig().getInt("y_spawn_teleport");
        parkourRegionManager = plugin.getParkourRegionManager();
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if(!p.getWorld().equals(Main.world)) return;
        Block b = p.getLocation().getBlock();


        if(plugin.getConfig().getBoolean("launchpad_enabled")) {
            for (Material plate : PRESSURE_PLATES) {
                if (b.getType().equals(plate)) {
                    p.setVelocity(p.getLocation().getDirection().multiply(4));
                    p.setVelocity(new Vector(p.getVelocity().getX(), 1.0D, p.getVelocity().getZ()));
                    break;
                }
            }

        }
        if(p.getLocation().getY() <= y_teleport) {
            p.teleport(Main.world.getSpawnLocation());
        }

        List<ParkourRegion> parkourRegionList = parkourRegionManager.getRegions();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(p.getLocation()));
        for (ProtectedRegion region : set) {
            for (ParkourRegion parkourRegion : parkourRegionList) {
                if(region.getId().equalsIgnoreCase(parkourRegion.getName())) {
                    //is parkour region
                    if(p.getLocation().getY() < parkourRegion.getMinY()) {
                        p.teleport(parkourRegion.getSpawnPoint(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        if(parkourRegion.hasFailMessage()) p.sendMessage(parkourRegion.getFailMessage());
                    }
                    break;
                }
            }
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (item == null) {
            if (!p.hasPermission("lobbytools.bypass.inventory")) e.setCancelled(true);
            return;
        }
        switch (item.getType()) {
            case CLOCK: {
                boolean is_hidden = plugin.hidden_map.getOrDefault(p.getUniqueId(), false);
                if(is_hidden) {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        if(p.equals(player)) continue;
                        p.showPlayer(plugin,player);
                    }
                    p.sendMessage("§aShowing all players now.");
                    plugin.hidden_map.put(p.getUniqueId(), false);
                }else{
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        if(p.equals(player)) continue;
                        if(player.hasPermission("lobbytools.bypass.hide")) continue;
                        p.hidePlayer(plugin,player);
                    }
                    p.sendMessage("§aHiding all players now.");
                    plugin.hidden_map.put(p.getUniqueId(), true);
                }
                break;
            }case CHEST: {
                p.openInventory(GADGETS_MENU);
                break;
            } case NETHER_STAR:
                p.openInventory(InventoryEvents.SERVER_SELECTOR);
                break;
            default:
                if (!p.hasPermission("lobbytools.bypass.inventory")) e.setCancelled(true);
        }
    }
}
