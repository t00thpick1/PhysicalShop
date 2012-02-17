package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.SERVER_SHOP;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_BUILD;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_BUILD_SERVER;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_PLACE_CHEST;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_USE;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_USE_CHEST;
import static com.wolvereness.physicalshop.config.Localized.Message.EXISTING_CHEST;
import static java.util.logging.Level.SEVERE;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.wolvereness.physicalshop.events.ShopCreationEvent;
import com.wolvereness.physicalshop.events.ShopSignCreationEvent;
import com.wolvereness.physicalshop.exception.InvalidSignException;
import com.wolvereness.physicalshop.exception.InvalidSignOwnerException;
import com.wolvereness.util.NameCollection;
import com.wolvereness.util.NameCollection.OutOfEntriesException;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class PhysicalShopListener implements Listener {
	private final PhysicalShop plugin;
	/**
	 * Default constructor
	 * @param plugin PhysicalShop plugin to consider
	 */
	public PhysicalShopListener(final PhysicalShop plugin) {
		this.plugin = plugin;
	}
	/**
	 * Block Break event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		onBlockDestroyed(e, e.getPlayer(), e.getBlock());
	}
	/**
	 * Central place for checking for an event cancellation
	 * @param e Event
	 * @param p Player
	 * @param b Block
	 * @return true if the caller should cease execution
	 */
	public boolean onBlockDestroyed(final Cancellable e, final Player p, final Block b) {
		if (e.isCancelled() || !plugin.getPluginConfig().isProtectBreak()) return true;
		if (ShopHelpers.isBlockDestroyable(b, p, plugin)) return false;
		e.setCancelled(true);
		return true;
	}
	/**
	 * Block Place event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent e) {
		if (e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		if (!plugin.getPluginConfig().isProtectChestAccess() || plugin.getPermissionHandler().hasAdmin(e.getPlayer())) return;

		final Block block = e.getBlock();

		final Block[] blocks = new Block[] {
				block.getRelative(BlockFace.NORTH),
				block.getRelative(BlockFace.EAST),
				block.getRelative(BlockFace.SOUTH),
				block.getRelative(BlockFace.WEST), };

		for (final Block b : blocks) {
			if (b.getType() == Material.CHEST) {
				final Shop shop = ShopHelpers.getShop(b.getRelative(BlockFace.UP), plugin);

				if (shop != null && shop.isShopBlock(b) && !shop.canDestroy(e.getPlayer(), plugin)) {
					plugin.getLocale().sendMessage(e.getPlayer(), CANT_PLACE_CHEST);
					e.setCancelled(true);
					break;
				}
			}
		}

		final Shop placedShop = ShopHelpers.getShop(block.getRelative(BlockFace.UP), plugin);

		if(placedShop != null) {
			plugin.getServer().getPluginManager().callEvent(new ShopCreationEvent(e, placedShop));
		}
	}
	/**
	 * Stop EntityChangeBlockEvents from affecting stores.
	 * Prevents Endermen from breaking stores, etc.
	 * @param e Event
	 */
	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Entity Explode event
	 * @param e Event
	 */
	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent e) {
		for (final Block block : e.blockList()) {
			if(onBlockDestroyed(e, null, block)) return;
		}
	}
	/**
	 * This listens for new shop creation and checks to see if the player has permission to build over a chest
	 * @param e Event
	 */
	@EventHandler
	public void onNewShopSign(final ShopSignCreationEvent e) {
		if(e.isCancelled() || !e.isCheckExistingChest()) return;
		if(
				plugin.lwcCheck(e.getCause().getBlock().getRelative(BlockFace.DOWN), e.getCause().getPlayer())
				|| plugin.locketteCheck(e.getCause().getBlock().getRelative(BlockFace.DOWN), e.getCause().getPlayer())) {
			e.setCheckExistingChest(false);
		}
	}
	/**
	 * Block BlockBurnEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onPhysics(final BlockBurnEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block LeavesDecayEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onPhysics(final BlockFadeEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block BlockPhysicsEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onPhysics(final BlockPhysicsEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block LeavesDecayEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onPhysics(final LeavesDecayEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block the BlockPistonExtendEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler
	public void onPistonExtend(final BlockPistonExtendEvent e) {
		for (final Block block : e.getBlocks()) {
			if(onBlockDestroyed(e, null, block)) return;
		}
	}
	/**
	 * Block the BlockPistonRetractEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler
	public void onPistonRetract(final BlockPistonRetractEvent e) {
		final BlockFace direction = e.getDirection();

		// We need to check to see if a sign is attached to the piston piece
		final Block b = e.getBlock();
		if(onBlockDestroyed(e, null, b.getRelative(direction))) return;

		// We only care about the second block if sticky piston is retracting.
		if (!e.isSticky()) return;
		onBlockDestroyed(e, null, b.getRelative(direction, 2));
	}
	/**
	 * Player Interact event
	 * @param e Event
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.isCancelled()) return;

		final Block block = e.getClickedBlock();

		if (
				plugin.getPluginConfig().isProtectChestAccess()
				&& (e.getAction() == Action.RIGHT_CLICK_BLOCK)
				&& (block.getType() == Material.CHEST)) {
			final Shop shop = ShopHelpers.getShop(block.getRelative(BlockFace.UP), plugin);
			if (
					(shop != null)
					&& shop.isShopBlock(block)
					&& !shop.canDestroy(e.getPlayer(), plugin)
					&& !plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE_CHEST);
				e.setCancelled(true);
				return;
			}
		}

		final Shop shop = ShopHelpers.getShop(block, plugin);

		if (shop == null) return;

		if (!plugin.getPermissionHandler().hasUse(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE);
			return;
		}

		if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
			shop.status(e.getPlayer(), plugin);
		} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			shop.interact(e.getPlayer(), plugin);
			e.setCancelled(true);
		}
	}
	/**
	 * Sign Change event
	 * @param e Event
	 */
	@EventHandler
	public void onSignChange(final SignChangeEvent e) {
		if (e.isCancelled()) return;

		try {
			new Shop(e.getLines(), plugin);
		} catch (final InvalidSignOwnerException ex) {
		} catch (final InvalidSignException ex) {
			return;
		}

		if (!plugin.getPermissionHandler().hasBuild(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_BUILD);
			e.setCancelled(true);
			return;
		}

		final ShopSignCreationEvent event = new ShopSignCreationEvent(e);
		if (e.getLine(3).equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) {
			if (!plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_BUILD_SERVER);
				e.setCancelled(true);
			}
			plugin.getServer().getPluginManager().callEvent(event.setCheckExistingChest(false));
		} else {
			if (plugin.getPluginConfig().isAutoFillName()) {
				if(plugin.getPluginConfig().isExtendedNames()) {
					try {
						e.setLine(3, NameCollection.getSignName(e.getPlayer().getName()));
					} catch (final OutOfEntriesException ex) {
						plugin.getLogger().severe("Player " + e.getPlayer() + " cannot register extended name!");
						e.getPlayer().sendMessage("Name overflow, notify server administrator!");
						e.setCancelled(true);
						return;
					}
				} else {
					e.setLine(3, ShopHelpers.truncateName(e.getPlayer().getName()));
				}
			}
			plugin.getServer().getPluginManager().callEvent(event.setCheckExistingChest(plugin.getPluginConfig().isExistingChestProtected()));
		}
		if (
				!e.isCancelled()
				&& event.isCheckExistingChest()
				&& e.getBlock().getRelative(BlockFace.DOWN).getType() == Material.CHEST
				&& !plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), EXISTING_CHEST);
			e.setCancelled(true);
			return;
		}
		if(!e.isCancelled() && e.getLine(3).equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) {
			try {
				plugin.getServer().getPluginManager().callEvent(new ShopCreationEvent(e, new Shop(e.getLines(), plugin)));
			} catch (final InvalidSignException ex) {
				plugin.getLogger().log(SEVERE, "Unexpected invalid shop", ex);
			}
		}
	}
}
