package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.ShopHelpers.getShop;
import static com.wolvereness.physicalshop.ShopHelpers.isBlockDestroyable;
import static com.wolvereness.physicalshop.ShopHelpers.isProtectedChestsAround;
import static com.wolvereness.physicalshop.ShopHelpers.truncateName;
import static com.wolvereness.physicalshop.config.ConfigOptions.SERVER_SHOP;
import static com.wolvereness.physicalshop.config.Localized.Message.*;
import static java.util.logging.Level.SEVERE;
import static org.bukkit.Material.CHEST;
import static org.bukkit.block.BlockFace.DOWN;
import static org.bukkit.block.BlockFace.UP;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
	public void onBlockBlockBreak(final BlockBreakEvent e) {
		onBlockDestroyed(e, e.getPlayer(), e.getBlock());
	}
	/**
	 * Block BlockBurnEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBlockBurn(final BlockBurnEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block LeavesDecayEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBlockFade(final BlockFadeEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block BlockPhysicsEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBlockPhysics(final BlockPhysicsEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block the BlockPistonExtendEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBlockPistonExtend(final BlockPistonExtendEvent e) {
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
	public void onBlockBlockPistonRetract(final BlockPistonRetractEvent e) {
		final BlockFace direction = e.getDirection();

		// We need to check to see if a sign is attached to the piston piece
		final Block b = e.getBlock();
		if(onBlockDestroyed(e, null, b.getRelative(direction))) return;

		// We only care about the second block if sticky piston is retracting.
		if (!e.isSticky()) return;
		onBlockDestroyed(e, null, b.getRelative(direction, 2));
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
		if (p != null && plugin.getPermissionHandler().hasAdmin(p)) return true;
		if (isBlockDestroyable(b, p, plugin)) {
			if(p != null) {
				plugin.getLocale().sendMessage(p, CANT_DESTROY);
			}
			return false;
		}
		e.setCancelled(true);
		return true;
	}
	/**
	 * Stop EntityChangeBlockEvents from affecting stores.
	 * Prevents Endermen from breaking stores, etc.
	 * @param e Event
	 */
	@EventHandler
	public void onBlockEntityChangeBlock(final EntityChangeBlockEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Entity Explode event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockEntityExplode(final EntityExplodeEvent e) {
		for (final Block block : e.blockList()) {
			if(onBlockDestroyed(e, null, block)) return;
		}
	}
	/**
	 * Block LeavesDecayEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler
	public void onBlockLeavesDecay(final LeavesDecayEvent e) {
		onBlockDestroyed(e, null, e.getBlock());
	}
	/**
	 * Block Place event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent e) {
		if (e.isCancelled() || e.getBlock().getType() != CHEST) return;
		if (!plugin.getPluginConfig().isProtectChestAccess() || plugin.getPermissionHandler().hasAdmin(e.getPlayer())) return;

		final Block block = e.getBlock();

		if(isProtectedChestsAround(block, e.getPlayer(), plugin)) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_PLACE_CHEST);
			e.setCancelled(true);
			return;
		}

		final Shop placedShop = getShop(block.getRelative(UP), plugin);
		if(placedShop != null) {
			plugin.getServer().getPluginManager().callEvent(new ShopCreationEvent(e, placedShop));
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
				plugin.lwcCheck(e.getCause().getBlock().getRelative(DOWN), e.getCause().getPlayer())
				|| plugin.locketteCheck(e.getCause().getBlock().getRelative(DOWN), e.getCause().getPlayer())) {
			e.setCheckExistingChest(false);
		}
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
				&& e.getAction() == RIGHT_CLICK_BLOCK
				&& block.getType() == CHEST
				&& !plugin.getPermissionHandler().hasAdmin(e.getPlayer())
				) {
			if(isProtectedChestsAround(block, e.getPlayer(), plugin)) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE_CHEST);
				e.setCancelled(true);
				return;
			}
			return;
		}

		final Shop shop = getShop(block, plugin);

		if (shop == null) return;

		if (!plugin.getPermissionHandler().hasUse(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE);
			return;
		}

		if (e.getAction() == LEFT_CLICK_BLOCK) {
			shop.status(e.getPlayer(), plugin);
		} else if (e.getAction() == RIGHT_CLICK_BLOCK) {
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
					e.setLine(3, truncateName(e.getPlayer().getName()));
				}
			}
			plugin.getServer().getPluginManager().callEvent(event.setCheckExistingChest(plugin.getPluginConfig().isExistingChestProtected()));
		}
		if (
				!e.isCancelled()
				&& event.isCheckExistingChest()
				&& e.getBlock().getRelative(DOWN).getType() == CHEST
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